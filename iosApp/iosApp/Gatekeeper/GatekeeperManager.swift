import Foundation
import UserNotifications

// MARK: - IntentDeclarationiOS

/// Local representation of an intent declaration stored on-device.
struct IntentDeclarationiOS: Codable {
    let id: UUID
    let timestamp: Date
    let declaredBundleId: String
    let declaredAppName: String
    let declaredDurationMinutes: Int

    /// True if the declared time window has not yet elapsed.
    var isActive: Bool {
        let endDate = timestamp.addingTimeInterval(TimeInterval(declaredDurationMinutes * 60))
        return Date() < endDate
    }

    /// Remaining seconds in the declared window; 0 if expired.
    var remainingSeconds: TimeInterval {
        let endDate = timestamp.addingTimeInterval(TimeInterval(declaredDurationMinutes * 60))
        return max(0, endDate.timeIntervalSince(Date()))
    }
}

// MARK: - GatekeeperManager

/// Manages when to surface the `GatekeeperView` sheet on iOS.
///
/// ### Strategy
/// On iOS, apps cannot intercept foreground-app transitions the way Android
/// does.  Instead `GatekeeperManager` hooks into the Bilbo app's own lifecycle:
/// - When Bilbo comes to the foreground via a tapped local notification, the
///   manager inspects the notification payload to decide if a gatekeeper should
///   be shown.
/// - `scheduleReminder` fires a local notification N seconds before a declared
///   window expires, prompting a mindfulness check.
/// - Direct "open app shortcuts within Bilbo also call `shouldShowGatekeeper`
///   before navigation.
///
/// All declarations are stored in `UserDefaults` for simplicity (can be
/// migrated to SQLite via the KMM shared module once the iOS bridge is ready).
final class GatekeeperManager: ObservableObject {

    // ── Singleton ─────────────────────────────────────────────────────────────

    static let shared = GatekeeperManager()
    private init() { loadDeclarations() }

    // ── Constants ─────────────────────────────────────────────────────────────

    private static let storageKey = "spark.gatekeeper.declarations"
    private static let reminderCategory = "SPARK_TIMER_EXPIRY"

    /// Apps that should never trigger the gatekeeper on iOS.
    private static let defaultBypassBundleIds: Set<String> = [
        "com.apple.mobilephone",
        "com.apple.MobileSMS",
        "com.apple.Maps",
        "com.apple.camera",
        "com.apple.Preferences",
        "com.apple.mobilecal",
        "com.apple.calculator",
        "com.apple.clock",
        "dev.spark.app",
    ]

    // ── Published state ───────────────────────────────────────────────────────

    /// When non-nil, the UI should present `GatekeeperView` for this bundle ID.
    @Published var pendingGatekeeperBundleId: String? = nil
    @Published var pendingGatekeeperAppName: String? = nil

    // ── Internal storage ──────────────────────────────────────────────────────

    private var declarations: [IntentDeclarationiOS] = []
    private var userBypassBundleIds: Set<String> = []

    // ── Public API ────────────────────────────────────────────────────────────

    /// Returns true if a gatekeeper should be presented before opening [bundleId].
    func shouldShowGatekeeper(bundleId: String) -> Bool {
        let allBypasses = GatekeeperManager.defaultBypassBundleIds.union(userBypassBundleIds)
        if allBypasses.contains(bundleId) { return false }
        return !hasActiveDeclaration(for: bundleId)
    }

    /// Call this to indicate the user wants to open [bundleId]; surfaces the
    /// gatekeeper if needed via the [pendingGatekeeperBundleId] publisher.
    func requestOpen(bundleId: String, appName: String) {
        if shouldShowGatekeeper(bundleId: bundleId) {
            pendingGatekeeperBundleId = bundleId
            pendingGatekeeperAppName = appName
        }
    }

    /// Dismiss the pending gatekeeper without recording a declaration.
    func dismissGatekeeper() {
        pendingGatekeeperBundleId = nil
        pendingGatekeeperAppName = nil
    }

    /// Create and persist a new [IntentDeclarationiOS], then schedule a
    /// reminder notification before the window expires.
    @discardableResult
    func createDeclaration(
        bundleId: String,
        appName: String,
        durationMinutes: Int,
        intention: String
    ) -> IntentDeclarationiOS {
        let declaration = IntentDeclarationiOS(
            id: UUID(),
            timestamp: Date(),
            declaredBundleId: bundleId,
            declaredAppName: appName,
            declaredDurationMinutes: durationMinutes
        )
        declarations.append(declaration)
        saveDeclarations()
        pendingGatekeeperBundleId = nil
        pendingGatekeeperAppName = nil
        scheduleReminder(for: declaration)
        return declaration
    }

    /// Returns the currently-active declaration for [bundleId], or nil.
    func activeDeclaration(for bundleId: String) -> IntentDeclarationiOS? {
        declarations
            .filter { $0.declaredBundleId == bundleId && $0.isActive }
            .sorted { $0.timestamp > $1.timestamp }
            .first
    }

    /// Add [bundleId] to the user-configurable bypass list.
    func addBypass(_ bundleId: String) {
        userBypassBundleIds.insert(bundleId)
    }

    /// Remove [bundleId] from the user-configurable bypass list.
    func removeBypass(_ bundleId: String) {
        userBypassBundleIds.remove(bundleId)
    }

    /// Prune expired declarations older than 30 days.
    func pruneExpired() {
        let cutoff = Date().addingTimeInterval(-30 * 24 * 60 * 60)
        declarations.removeAll { $0.timestamp < cutoff }
        saveDeclarations()
    }

    // ── Notification scheduling ───────────────────────────────────────────────

    /// Schedules a local notification 1 minute before the declared window ends.
    private func scheduleReminder(for declaration: IntentDeclarationiOS) {
        let center = UNUserNotificationCenter.current()
        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, _ in
            guard granted else { return }
            self.doScheduleReminder(declaration: declaration, center: center)
        }
    }

    private func doScheduleReminder(
        declaration: IntentDeclarationiOS,
        center: UNUserNotificationCenter
    ) {
        let totalSeconds = Double(declaration.declaredDurationMinutes * 60)
        // Fire 60 seconds before the window closes (or at 75% of window if < 2 min)
        let triggerOffset = min(60.0, totalSeconds * 0.75)
        let triggerDate = declaration.timestamp.addingTimeInterval(totalSeconds - triggerOffset)
        let interval = triggerDate.timeIntervalSinceNow
        guard interval > 0 else { return }

        let content = UNMutableNotificationContent()
        content.title = "Time check — \(declaration.declaredAppName)"
        content.body = "Your \(declaration.declaredDurationMinutes)-min window is almost up. Still on track?"
        content.sound = .default
        content.categoryIdentifier = GatekeeperManager.reminderCategory

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        let request = UNNotificationRequest(
            identifier: "spark.reminder.\(declaration.id.uuidString)",
            content: content,
            trigger: trigger
        )

        center.add(request) { error in
            if let error = error {
                print("GatekeeperManager: failed to schedule reminder — \(error)")
            }
        }
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private func hasActiveDeclaration(for bundleId: String) -> Bool {
        declarations.contains { $0.declaredBundleId == bundleId && $0.isActive }
    }

    private func saveDeclarations() {
        guard let data = try? JSONEncoder().encode(declarations) else { return }
        UserDefaults.standard.set(data, forKey: GatekeeperManager.storageKey)
    }

    private func loadDeclarations() {
        guard
            let data = UserDefaults.standard.data(forKey: GatekeeperManager.storageKey),
            let decoded = try? JSONDecoder().decode([IntentDeclarationiOS].self, from: data)
        else { return }
        declarations = decoded
        pruneExpired()
    }
}
