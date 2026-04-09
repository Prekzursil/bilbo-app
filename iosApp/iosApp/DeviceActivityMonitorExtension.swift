// DeviceActivityMonitorExtension.swift
// Bilbo — DeviceActivityMonitor Extension
//
// Monitors app usage schedules and bridges events back to the main app
// via a shared App Group UserDefaults container.

import DeviceActivity
import Foundation
import ManagedSettings

// MARK: - App Group constant (must match entitlements)

private let kAppGroup = "group.dev.bilbo.app"

// MARK: - Shared keys (read by main app)

enum DAMonitorKey {
    static let usageLog      = "da_usage_log"       // JSON array of UsageRecord
    static let lastEventDate = "da_last_event_date"
}

// MARK: - Usage record model (Codable for JSON bridge)

struct UsageRecord: Codable {
    let bundleId: String
    let activityName: String
    let eventType: String   // "start" | "end" | "threshold"
    let timestamp: Date
    let durationSeconds: Double
}

// MARK: - Extension principal class

class DeviceActivityMonitorExtension: DeviceActivityMonitor {

    // Shared defaults bridge to main app
    private var sharedDefaults: UserDefaults {
        UserDefaults(suiteName: kAppGroup) ?? .standard
    }

    // In-memory session start times: activityName → Date
    private var sessionStarts: [String: Date] = [:]

    // MARK: - Scheduling helpers (called by main app via NSXPCConnection is not needed;
    //         the main app schedules via DeviceActivityCenter directly.)

    /// Call from the main app to register a monitoring schedule for a set of apps.
    /// - Parameters:
    ///   - activityName: Unique name per tracked application bundle.
    ///   - schedule: The daily monitoring window.
    ///   - events: Usage thresholds to fire callbacks on.
    static func scheduleMonitoring(
        activityName: DeviceActivityName,
        schedule: DeviceActivitySchedule,
        events: [DeviceActivityEvent.Name: DeviceActivityEvent]
    ) {
        let center = DeviceActivityCenter()
        do {
            try center.startMonitoring(activityName, during: schedule, events: events)
        } catch {
            // Log; the main app should surface this.
            print("[Bilbo][DAMonitor] Failed to schedule \(activityName.rawValue): \(error)")
        }
    }

    /// Stop monitoring a specific activity.
    static func stopMonitoring(activityName: DeviceActivityName) {
        DeviceActivityCenter().stopMonitoring([activityName])
    }

    /// Stop all monitored activities.
    static func stopAllMonitoring() {
        DeviceActivityCenter().stopMonitoring()
    }

    // MARK: - DeviceActivityMonitor callbacks

    override func intervalDidStart(for activity: DeviceActivityName) {
        let start = Date()
        sessionStarts[activity.rawValue] = start
        appendRecord(
            UsageRecord(
                bundleId: bundleId(for: activity),
                activityName: activity.rawValue,
                eventType: "start",
                timestamp: start,
                durationSeconds: 0
            )
        )
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        let end = Date()
        let start = sessionStarts.removeValue(forKey: activity.rawValue) ?? end
        let duration = end.timeIntervalSince(start)

        appendRecord(
            UsageRecord(
                bundleId: bundleId(for: activity),
                activityName: activity.rawValue,
                eventType: "end",
                timestamp: end,
                durationSeconds: duration
            )
        )

        // Accumulate daily total
        accumulateDailyUsage(bundleId: bundleId(for: activity), seconds: duration)
    }

    override func eventDidReachThreshold(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        let now = Date()
        let start = sessionStarts[activity.rawValue] ?? now
        let elapsed = now.timeIntervalSince(start)

        appendRecord(
            UsageRecord(
                bundleId: bundleId(for: activity),
                activityName: activity.rawValue,
                eventType: "threshold:\(event.rawValue)",
                timestamp: now,
                durationSeconds: elapsed
            )
        )

        sharedDefaults.set(now.timeIntervalSince1970, forKey: DAMonitorKey.lastEventDate)
    }

    override func intervalWillStartWarning(for activity: DeviceActivityName) {
        // No-op; can be used for pre-start notifications if needed.
    }

    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        // No-op; can be used for approaching-limit warnings if needed.
    }

    override func eventWillReachThresholdWarning(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        // No-op; threshold warning before it fires.
    }

    // MARK: - Private helpers

    /// Derive bundle ID from activity name.
    /// Convention: activity name is the bundle ID (e.g., "com.instagram.Instagram").
    private func bundleId(for activity: DeviceActivityName) -> String {
        activity.rawValue
    }

    /// Append a usage record to the shared log (persisted JSON array).
    private func appendRecord(_ record: UsageRecord) {
        var existing = loadRecords()
        existing.append(record)
        // Keep only the last 500 records to avoid unbounded growth.
        if existing.count > 500 { existing = Array(existing.suffix(500)) }
        saveRecords(existing)
    }

    private func loadRecords() -> [UsageRecord] {
        guard let data = sharedDefaults.data(forKey: DAMonitorKey.usageLog),
              let records = try? JSONDecoder().decode([UsageRecord].self, from: data) else {
            return []
        }
        return records
    }

    private func saveRecords(_ records: [UsageRecord]) {
        guard let data = try? JSONEncoder().encode(records) else { return }
        sharedDefaults.set(data, forKey: DAMonitorKey.usageLog)
    }

    /// Persist cumulative daily usage per bundle ID.
    private func accumulateDailyUsage(bundleId: String, seconds: Double) {
        let key = "da_daily_\(bundleId)_\(todayString())"
        let current = sharedDefaults.double(forKey: key)
        sharedDefaults.set(current + seconds, forKey: key)
    }

    private func todayString() -> String {
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd"
        return fmt.string(from: Date())
    }
}

// MARK: - Activity name helpers (used by main app)

extension DeviceActivityName {
    /// Constructs a DeviceActivityName from a bundle ID.
    static func forApp(_ bundleId: String) -> DeviceActivityName {
        DeviceActivityName(bundleId)
    }
}
