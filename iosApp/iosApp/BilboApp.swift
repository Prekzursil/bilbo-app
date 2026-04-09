import SwiftUI

@main
struct BilboApp: App {

    @UIApplicationDelegateAdaptor(BilboAppDelegate.self) var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}

class BilboAppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Configure Sentry
        // SentrySDK.start { options in
        //     options.dsn = "YOUR_SENTRY_DSN"
        //     options.tracesSampleRate = 1.0
        // }

        // Configure PostHog
        // let posthogConfig = PHGPostHogConfiguration(apiKey: "YOUR_POSTHOG_KEY", host: "https://app.posthog.com")
        // PHGPostHog.setup(with: posthogConfig)

        return true
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        let token = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("APNs device token: \(token)")
        // Forward token to Supabase push notification service
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")
    }
}
