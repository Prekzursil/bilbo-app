import Foundation
import SwiftUI
import UserNotifications
import Combine

// MARK: - EnforcementState

/// Published state driving `EnforcementView` presentation.
struct EnforcementState {
    let bundleId: String
    let appName: String
    let mode: EnforcementDisplayMode
    let suggestion: String
}

// MARK: - EnforcementManager

/// Manages session-timer countdowns, determines enforcement mode, and coordinates
/// the presentation of `EnforcementView` (nudge or hard lock) on iOS.
///
/// ### iOS Strategy
/// Because iOS does not support full foreground interception like Android, enforcement
/// is delivered via:
/// 1. **In-app**: When the user taps a tracked-app shortcut inside Bilbo, the timer fires
///    inside the app and this manager shows an overlay via SwiftUI sheet/fullScreenCover.
/// 2. **Background notifications**: A local notification fires when a timer expires while
///    Bilbo is backgrounded; tapping it opens Bilbo and triggers the enforcement UI.
/// 3. **Cooldown re-check**: On every `scenePhase` foreground event, active cooldowns
///    are checked and the hard-lock UI re-presents if needed.
@MainActor
final class EnforcementManager: ObservableObject {

    // ── Singleton ─────────────────────────────────────────────────────────────

    static let shared = EnforcementManager()
    private init() {
        restoreCooldowns()
    }

    // ── Constants ─────────────────────────────────────────────────────────────

    private let hardLockCooldownMinutes = 30
    private let nudgeExtensionMinutes   = 5
    private let nudgeFPCost             = 5
    private let hardLockFPCost          = 10

    // ── Published ─────────────────────────────────────────────────────────────

    /// When non-nil, the app should present `EnforcementView` with this state.
    @Published var activeEnforcement: EnforcementState? = nil

    // ── Internal state ────────────────────────────────────────────────────────

    /// Active timer tasks: bundleId → Task handle
    private var timerTasks: [String: Task<Void, Never>] = [:]

    /// Cooldown expiry timestamps: bundleId → Unix epoch seconds
    private var cooldowns: [String: TimeInterval] = [:]

    private let cooldownKey = "spark.enforcement.cooldowns"
    private let notifCategory = "SPARK_TIMER_EXPIRY"

    // ── Timer management ──────────────────────────────────────────────────────

    /// Start a countdown timer for [bundleId].
    /// Cancels any existing timer for that bundle ID.
    ///
    /// - Parameters:
    ///   - bundleId: The tracked app's bundle identifier.
    ///   - appName: Human-readable app name.
    ///   - durationMinutes: Declared session duration.
    ///   - fpBalance: Current FP balance (used for override checks).
    func startTimer(
        bundleId: String,
        appName: String,
        durationMinutes: Int,
        fpBalance: Int = 0
    ) {
        cancelTimer(for: bundleId)
        scheduleExpiryNotification(bundleId: bundleId, appName: appName, durationMinutes: durationMinutes)

        let task = Task {
            let durationSecs = durationMinutes * 60
            let warningSecs  = max(0, durationSecs - 120)

            if warningSecs > 0 {
                try? await Task.sleep(nanoseconds: UInt64(warningSecs) * 1_000_000_000)
                guard !Task.isCancelled else { return }
                postWarningNotification(appName: appName, remainingSecs: 120)
            } else {
                try? await Task.sleep(nanoseconds: UInt64(durationSecs) * 1_000_000_000)
            }

            guard !Task.isCancelled else { return }

            // Remaining 2 minutes (or full duration if < 2 min)
            let finalWait = min(durationSecs, 120)
            try? await Task.sleep(nanoseconds: UInt64(finalWait) * 1_000_000_000)
            guard !Task.isCancelled else { return }

            timerExpired(bundleId: bundleId, appName: appName, durationMinutes: durationMinutes, fpBalance: fpBalance)
        }

        timerTasks[bundleId] = task
    }

    /// Cancel an active timer for [bundleId].
    func cancelTimer(for bundleId: String) {
        timerTasks[bundleId]?.cancel()
        timerTasks.removeValue(forKey: bundleId)
        cancelExpiryNotification(bundleId: bundleId)
    }

    // ── Enforcement ───────────────────────────────────────────────────────────

    /// Called when a timer expires.
    private func timerExpired(bundleId: String, appName: String, durationMinutes: Int, fpBalance: Int) {
        timerTasks.removeValue(forKey: bundleId)
        let mode = determineEnforcementMode(for: bundleId)

        switch mode {
        case .nudge:
            activeEnforcement = EnforcementState(
                bundleId: bundleId,
                appName: appName,
                mode: .nudge(declaredMinutes: durationMinutes, actualMinutes: durationMinutes),
                suggestion: randomSuggestion()
            )

        case .hardLock:
            let expiryEpoch = Date().timeIntervalSince1970 + Double(hardLockCooldownMinutes * 60)
            cooldowns[bundleId] = expiryEpoch
            persistCooldowns()

            activeEnforcement = EnforcementState(
                bundleId: bundleId,
                appName: appName,
                mode: .hardLock(
                    cooldownMinutes: hardLockCooldownMinutes,
                    remainingSeconds: hardLockCooldownMinutes * 60
                ),
                suggestion: randomSuggestion()
            )
        }
    }

    /// Check if [bundleId] is in cooldown; if so, surface the hard-lock enforcement UI.
    /// Returns true if cooldown is active.
    @discardableResult
    func checkAndEnforceCooldown(bundleId: String, appName: String, fpBalance: Int = 0) -> Bool {
        guard let expiry = cooldowns[bundleId] else { return false }
        let now = Date().timeIntervalSince1970
        guard expiry > now else {
            cooldowns.removeValue(forKey: bundleId)
            persistCooldowns()
            return false
        }

        let remaining = Int(expiry - now)
        activeEnforcement = EnforcementState(
            bundleId: bundleId,
            appName: appName,
            mode: .hardLock(cooldownMinutes: hardLockCooldownMinutes, remainingSeconds: remaining),
            suggestion: randomSuggestion()
        )
        return true
    }

    // ── User Actions ──────────────────────────────────────────────────────────

    /// User tapped "Got it" on the nudge.
    func acknowledgeNudge() {
        activeEnforcement = nil
    }

    /// User requested a 5-minute extension on the nudge (costs 5 FP).
    func extendSession(bundleId: String, appName: String, currentFPBalance: Int) {
        guard currentFPBalance >= nudgeFPCost else { return }
        // FP deduction is handled by the budget layer; just restart timer
        activeEnforcement = nil
        startTimer(bundleId: bundleId, appName: appName, durationMinutes: nudgeExtensionMinutes, fpBalance: currentFPBalance - nudgeFPCost)
    }

    /// User chose "Go Home" from the hard lock screen.
    func dismissHardLock() {
        activeEnforcement = nil
    }

    /// User confirmed the hard lock override (costs 10 FP).
    func overrideHardLock(bundleId: String, currentFPBalance: Int) {
        guard currentFPBalance >= hardLockFPCost else { return }
        cooldowns.removeValue(forKey: bundleId)
        persistCooldowns()
        activeEnforcement = nil
        // FP deduction is handled by the calling BudgetViewModel
    }

    // ── Enforcement mode determination ────────────────────────────────────────

    /// Determines whether NUDGE or HARD_LOCK should apply for [bundleId].
    /// Currently reads from UserDefaults; wire to AppProfileRepository for full integration.
    private func determineEnforcementMode(for bundleId: String) -> AppEnforcementMode {
        let key = "spark.enforcementMode.\(bundleId)"
        let stored = UserDefaults.standard.string(forKey: key) ?? "NUDGE"
        return stored == "HARD_LOCK" ? .hardLock : .nudge
    }

    /// Sets the enforcement mode for a bundle ID (called from settings).
    func setEnforcementMode(_ mode: AppEnforcementMode, for bundleId: String) {
        let key = "spark.enforcementMode.\(bundleId)"
        UserDefaults.standard.set(mode == .hardLock ? "HARD_LOCK" : "NUDGE", forKey: key)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private func scheduleExpiryNotification(bundleId: String, appName: String, durationMinutes: Int) {
        let center = UNUserNotificationCenter.current()
        let content = UNMutableNotificationContent()
        content.title = "⏰ Time's up — \(appName)"
        content.body = "Your \(durationMinutes)-min session has ended. Open Bilbo to review."
        content.sound = .default
        content.categoryIdentifier = notifCategory
        content.userInfo = ["bundleId": bundleId, "appName": appName]

        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: TimeInterval(durationMinutes * 60),
            repeats: false
        )
        let request = UNNotificationRequest(
            identifier: "spark.expiry.\(bundleId)",
            content: content,
            trigger: trigger
        )
        center.add(request)
    }

    private func cancelExpiryNotification(bundleId: String) {
        UNUserNotificationCenter.current()
            .removePendingNotificationRequests(withIdentifiers: ["spark.expiry.\(bundleId)"])
    }

    private func postWarningNotification(appName: String, remainingSecs: Int) {
        let center = UNUserNotificationCenter.current()
        let content = UNMutableNotificationContent()
        content.title = "⏱ Almost time — \(appName)"
        content.body = "Your session ends in about 2 minutes. Time to wrap up."
        content.sound = .default
        let request = UNNotificationRequest(
            identifier: "spark.warning.\(appName)",
            content: content,
            trigger: nil  // immediate
        )
        center.add(request)
    }

    // ── Cooldown persistence ──────────────────────────────────────────────────

    private func persistCooldowns() {
        let data = try? JSONEncoder().encode(cooldowns)
        UserDefaults.standard.set(data, forKey: cooldownKey)
    }

    private func restoreCooldowns() {
        guard
            let data = UserDefaults.standard.data(forKey: cooldownKey),
            let decoded = try? JSONDecoder().decode([String: TimeInterval].self, from: data)
        else { return }

        let now = Date().timeIntervalSince1970
        cooldowns = decoded.filter { $0.value > now }
        persistCooldowns()
    }

    // ── Suggestions ───────────────────────────────────────────────────────────

    private func randomSuggestion() -> String {
        let suggestions = [
            "Take a short walk outside 🚶",
            "Do 10 deep breaths 🌬️",
            "Read a few pages of a book 📖",
            "Make yourself a hot drink ☕",
            "Do some light stretching 🧘",
            "Write in your journal ✍️",
            "Listen to a favourite song 🎵",
            "Drink a glass of water 💧",
        ]
        return suggestions.randomElement() ?? suggestions[0]
    }
}

// MARK: - AppEnforcementMode

enum AppEnforcementMode {
    case nudge
    case hardLock
}
