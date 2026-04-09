// PushNotificationManager.swift
// Spark — iOS Platform
//
// Handles:
//   • APNs registration & device token lifecycle
//   • Token persistence to Supabase (via REST)
//   • Notification category/action registration
//   • Incoming notification routing for nudge, insight, challenge types

import Foundation
import UserNotifications
import UIKit

// MARK: - Notification category identifiers

enum SparkNotificationCategory: String {
    case nudge           = "SPARK_NUDGE"
    case weeklyInsight   = "SPARK_WEEKLY_INSIGHT"
    case challengeUpdate = "SPARK_CHALLENGE_UPDATE"
    case checkInReminder = "SPARK_CHECK_IN"
}

// MARK: - Notification action identifiers

enum SparkNotificationAction: String {
    // Nudge actions
    case nudgeAccept   = "NUDGE_ACCEPT"
    case nudgeDismiss  = "NUDGE_DISMISS"
    case nudgeSnooze   = "NUDGE_SNOOZE_15"

    // Insight actions
    case viewInsight   = "VIEW_INSIGHT"
    case shareInsight  = "SHARE_INSIGHT"

    // Challenge actions
    case viewChallenge = "VIEW_CHALLENGE"
    case skipChallenge = "SKIP_CHALLENGE"
}

// MARK: - Notification payload models

struct NudgePayload: Codable {
    let nudgeId: String
    let message: String
    let type: String
}

struct InsightPayload: Codable {
    let insightId: String
    let weekStart: String   // ISO-8601
}

struct ChallengePayload: Codable {
    let challengeId: String
    let title: String
    let currentProgress: Int
    let targetProgress: Int
}

// MARK: - Manager

@MainActor
final class PushNotificationManager: NSObject, ObservableObject {

    // MARK: - Shared instance

    static let shared = PushNotificationManager()

    // MARK: - Published state

    @Published private(set) var authorizationStatus: UNAuthorizationStatus = .notDetermined
    @Published private(set) var deviceToken: String?
    @Published var lastReceivedNotification: [AnyHashable: Any]?

    // MARK: - Private

    private let supabaseUrl: String = {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_URL") as? String ?? ""
    }()
    private let supabaseAnonKey: String = {
        Bundle.main.object(forInfoDictionaryKey: "SUPABASE_ANON_KEY") as? String ?? ""
    }()
    private let defaults = UserDefaults(suiteName: "group.dev.spark.app") ?? .standard
    private let tokenKey = "apns_device_token"

    // MARK: - Init

    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
        deviceToken = defaults.string(forKey: tokenKey)
    }

    // MARK: - Registration

    /// Call from AppDelegate.application(_:didFinishLaunchingWithOptions:)
    func registerForPushNotifications() async {
        let center = UNUserNotificationCenter.current()

        // Register notification categories first
        center.setNotificationCategories(buildCategories())

        // Request authorization
        do {
            let granted = try await center.requestAuthorization(
                options: [.alert, .badge, .sound, .criticalAlert]
            )
            if granted {
                await MainActor.run {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
            let settings = await center.notificationSettings()
            authorizationStatus = settings.authorizationStatus
        } catch {
            print("[Spark][PushNotification] Authorization error: \(error)")
        }
    }

    /// Called by AppDelegate when APNs assigns or updates the token.
    func handleDeviceToken(_ tokenData: Data) {
        let token = tokenData.map { String(format: "%02x", $0) }.joined()
        deviceToken = token
        defaults.set(token, forKey: tokenKey)
        Task { await uploadTokenToSupabase(token: token) }
    }

    /// Called by AppDelegate when APNs registration fails.
    func handleRegistrationError(_ error: Error) {
        print("[Spark][PushNotification] Registration failed: \(error)")
        authorizationStatus = .denied
    }

    // MARK: - Supabase token upsert

    private func uploadTokenToSupabase(token: String) async {
        guard !supabaseUrl.isEmpty, !supabaseAnonKey.isEmpty else { return }
        guard let userId = defaults.string(forKey: "spark_user_id") else { return }

        let urlStr = "\(supabaseUrl)/rest/v1/device_tokens"
        guard let url = URL(string: urlStr) else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("return=minimal", forHTTPHeaderField: "Prefer")
        request.setValue("Bearer \(supabaseAnonKey)", forHTTPHeaderField: "Authorization")
        request.setValue(supabaseAnonKey, forHTTPHeaderField: "apikey")

        let payload: [String: Any] = [
            "user_id": userId,
            "token": token,
            "platform": "ios",
            "updated_at": ISO8601DateFormatter().string(from: Date())
        ]

        guard let body = try? JSONSerialization.data(withJSONObject: payload) else { return }
        request.httpBody = body

        do {
            let (_, response) = try await URLSession.shared.data(for: request)
            if let http = response as? HTTPURLResponse, http.statusCode >= 400 {
                print("[Spark][PushNotification] Token upload HTTP \(http.statusCode)")
            }
        } catch {
            print("[Spark][PushNotification] Token upload error: \(error)")
        }
    }

    // MARK: - Notification categories

    private func buildCategories() -> Set<UNNotificationCategory> {
        // Nudge
        let nudgeCategory = UNNotificationCategory(
            identifier: SparkNotificationCategory.nudge.rawValue,
            actions: [
                UNNotificationAction(
                    identifier: SparkNotificationAction.nudgeAccept.rawValue,
                    title: "Accept Nudge",
                    options: [.foreground]
                ),
                UNNotificationAction(
                    identifier: SparkNotificationAction.nudgeSnooze.rawValue,
                    title: "Snooze 15 min",
                    options: []
                ),
                UNNotificationAction(
                    identifier: SparkNotificationAction.nudgeDismiss.rawValue,
                    title: "Dismiss",
                    options: [.destructive]
                )
            ],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )

        // Weekly insight
        let insightCategory = UNNotificationCategory(
            identifier: SparkNotificationCategory.weeklyInsight.rawValue,
            actions: [
                UNNotificationAction(
                    identifier: SparkNotificationAction.viewInsight.rawValue,
                    title: "View Insight",
                    options: [.foreground]
                ),
                UNNotificationAction(
                    identifier: SparkNotificationAction.shareInsight.rawValue,
                    title: "Share",
                    options: [.foreground]
                )
            ],
            intentIdentifiers: [],
            options: []
        )

        // Challenge update
        let challengeCategory = UNNotificationCategory(
            identifier: SparkNotificationCategory.challengeUpdate.rawValue,
            actions: [
                UNNotificationAction(
                    identifier: SparkNotificationAction.viewChallenge.rawValue,
                    title: "See Challenge",
                    options: [.foreground]
                ),
                UNNotificationAction(
                    identifier: SparkNotificationAction.skipChallenge.rawValue,
                    title: "Skip",
                    options: [.destructive]
                )
            ],
            intentIdentifiers: [],
            options: []
        )

        // Check-in reminder (no custom actions — tap to open app)
        let checkInCategory = UNNotificationCategory(
            identifier: SparkNotificationCategory.checkInReminder.rawValue,
            actions: [],
            intentIdentifiers: [],
            options: []
        )

        return [nudgeCategory, insightCategory, challengeCategory, checkInCategory]
    }

    // MARK: - Incoming notification routing

    func handleNotification(userInfo: [AnyHashable: Any]) {
        lastReceivedNotification = userInfo
        guard let categoryId = (userInfo["aps"] as? [String: Any])?["category"] as? String,
              let category = SparkNotificationCategory(rawValue: categoryId) else {
            return
        }

        switch category {
        case .nudge:
            handleNudge(userInfo: userInfo)
        case .weeklyInsight:
            handleWeeklyInsight(userInfo: userInfo)
        case .challengeUpdate:
            handleChallengeUpdate(userInfo: userInfo)
        case .checkInReminder:
            handleCheckInReminder()
        }
    }

    func handleNotificationAction(
        identifier: String,
        userInfo: [AnyHashable: Any],
        completionHandler: @escaping () -> Void
    ) {
        defer { completionHandler() }
        guard let action = SparkNotificationAction(rawValue: identifier) else { return }

        switch action {
        case .nudgeAccept:
            NotificationCenter.default.post(name: .sparkNudgeAccepted, object: userInfo)
        case .nudgeDismiss:
            NotificationCenter.default.post(name: .sparkNudgeDismissed, object: userInfo)
        case .nudgeSnooze:
            NotificationCenter.default.post(name: .sparkNudgeSnoozed, object: userInfo)
        case .viewInsight:
            NotificationCenter.default.post(name: .sparkOpenInsight, object: userInfo)
        case .shareInsight:
            NotificationCenter.default.post(name: .sparkShareInsight, object: userInfo)
        case .viewChallenge:
            NotificationCenter.default.post(name: .sparkOpenChallenge, object: userInfo)
        case .skipChallenge:
            break
        }
    }

    // MARK: - Category handlers

    private func handleNudge(userInfo: [AnyHashable: Any]) {
        NotificationCenter.default.post(name: .sparkNudgeReceived, object: userInfo)
    }

    private func handleWeeklyInsight(userInfo: [AnyHashable: Any]) {
        NotificationCenter.default.post(name: .sparkWeeklyInsightReady, object: userInfo)
    }

    private func handleChallengeUpdate(userInfo: [AnyHashable: Any]) {
        NotificationCenter.default.post(name: .sparkChallengeUpdated, object: userInfo)
    }

    private func handleCheckInReminder() {
        NotificationCenter.default.post(name: .sparkCheckInReminder, object: nil)
    }
}

// MARK: - UNUserNotificationCenterDelegate

extension PushNotificationManager: UNUserNotificationCenterDelegate {

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        // Show banner + badge + sound even when the app is foregrounded.
        return [.banner, .badge, .sound]
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        let actionId  = response.actionIdentifier

        await MainActor.run {
            if actionId == UNNotificationDefaultActionIdentifier {
                self.handleNotification(userInfo: userInfo)
            } else {
                self.handleNotificationAction(identifier: actionId, userInfo: userInfo) {}
            }
        }
    }
}

// MARK: - Notification.Name extensions

extension Notification.Name {
    static let sparkNudgeReceived     = Notification.Name("spark.nudge.received")
    static let sparkNudgeAccepted     = Notification.Name("spark.nudge.accepted")
    static let sparkNudgeDismissed    = Notification.Name("spark.nudge.dismissed")
    static let sparkNudgeSnoozed      = Notification.Name("spark.nudge.snoozed")
    static let sparkWeeklyInsightReady = Notification.Name("spark.insight.weekly_ready")
    static let sparkOpenInsight        = Notification.Name("spark.insight.open")
    static let sparkShareInsight       = Notification.Name("spark.insight.share")
    static let sparkChallengeUpdated  = Notification.Name("spark.challenge.updated")
    static let sparkOpenChallenge     = Notification.Name("spark.challenge.open")
    static let sparkCheckInReminder   = Notification.Name("spark.checkin.reminder")
}
