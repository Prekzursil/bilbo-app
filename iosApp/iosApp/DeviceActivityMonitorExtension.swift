import DeviceActivity
import ManagedSettings
import Foundation

// MARK: - SparkDeviceActivityMonitor

/// Monitors device activity events such as thresholds being reached
/// and interval start/end. Runs in a separate extension process.
class SparkDeviceActivityMonitor: DeviceActivityMonitor {

    let store = ManagedSettingsStore()

    override func intervalDidStart(for activity: DeviceActivityName) {
        super.intervalDidStart(for: activity)
        // Called when a monitored activity interval begins (e.g., start of day)
        // Reset daily usage counters via shared App Group UserDefaults
        let defaults = UserDefaults(suiteName: "group.dev.spark.app")
        defaults?.set(Date(), forKey: "dailyIntervalStart")
    }

    override func intervalDidEnd(for activity: DeviceActivityName) {
        super.intervalDidEnd(for: activity)
        // Called when a monitored activity interval ends (e.g., end of day)
        // Persist final usage data to shared container for main app to read
    }

    override func eventDidReachThreshold(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        super.eventDidReachThreshold(event, activity: activity)
        // Called when app usage crosses a user-defined threshold
        // Apply shield to restricted apps
        store.shield.applications = nil // placeholder — replace with actual selection
    }

    override func intervalWillStartWarning(for activity: DeviceActivityName) {
        super.intervalWillStartWarning(for: activity)
    }

    override func intervalWillEndWarning(for activity: DeviceActivityName) {
        super.intervalWillEndWarning(for: activity)
    }

    override func eventWillReachThresholdWarning(
        _ event: DeviceActivityEvent.Name,
        activity: DeviceActivityName
    ) {
        super.eventWillReachThresholdWarning(event, activity: activity)
    }
}
