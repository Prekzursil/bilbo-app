import ManagedSettings
import ManagedSettingsUI
import UIKit

// MARK: - SparkShieldConfiguration

/// Provides a custom UI for the app shield shown when a user
/// has exceeded their screen time limit for a given application.
class SparkShieldConfiguration: ShieldConfigurationDataSource {

    override func configuration(
        shielding application: Application
    ) -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            backgroundColor: UIColor(red: 0.05, green: 0.05, blue: 0.10, alpha: 0.95),
            icon: UIImage(systemName: "bolt.circle.fill"),
            title: ShieldConfiguration.Label(
                text: "Time's Up",
                color: .white
            ),
            subtitle: ShieldConfiguration.Label(
                text: "You've reached your limit for \(application.localizedDisplayName ?? "this app").",
                color: UIColor.white.withAlphaComponent(0.8)
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Take a Break",
                color: .white
            ),
            primaryButtonBackgroundColor: UIColor(red: 1.0, green: 0.45, blue: 0.0, alpha: 1.0),
            secondaryButtonLabel: ShieldConfiguration.Label(
                text: "Ignore Limit",
                color: UIColor.white.withAlphaComponent(0.6)
            )
        )
    }

    override func configuration(
        shielding application: Application,
        in category: ActivityCategory
    ) -> ShieldConfiguration {
        configuration(shielding: application)
    }

    override func configuration(
        shielding webDomain: WebDomain
    ) -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            title: ShieldConfiguration.Label(
                text: "Site Blocked",
                color: .white
            ),
            subtitle: ShieldConfiguration.Label(
                text: "This site is blocked during your focus session.",
                color: UIColor.white.withAlphaComponent(0.8)
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Go Back",
                color: .white
            ),
            primaryButtonBackgroundColor: UIColor(red: 1.0, green: 0.45, blue: 0.0, alpha: 1.0)
        )
    }
}
