// ShieldConfigurationExtension.swift
// Spark — Shield Configuration Extension
//
// Provides branded Spark shield UI for Hard Lock mode.
// Deep blue/purple palette matching the Android brand.
// Two actions: override with FP cost, or navigate to Spark.

import ManagedSettings
import ManagedSettingsUI
import UIKit

// MARK: - Colors

private extension UIColor {
    /// Spark deep navy — primary shield background
    static let sparkNavy = UIColor(red: 0.094, green: 0.133, blue: 0.259, alpha: 1)
    /// Spark deep purple — accent
    static let sparkPurple = UIColor(red: 0.345, green: 0.204, blue: 0.635, alpha: 1)
    /// Spark teal — CTA accent
    static let sparkTeal = UIColor(red: 0.000, green: 0.537, blue: 0.482, alpha: 1)
    /// Off-white text
    static let sparkWhite = UIColor(red: 0.980, green: 0.980, blue: 0.996, alpha: 1)
    /// Amber warning
    static let sparkAmber = UIColor(red: 1.000, green: 0.702, blue: 0.000, alpha: 1)
}

// MARK: - Shield action names

enum SparkShieldAction: String {
    case overrideFP   = "override_fp"       // "Override (10 FP)"
    case goToSpark    = "go_to_spark"       // "Go to Spark"
}

// MARK: - Extension principal class

class ShieldConfigurationExtension: ShieldConfigurationDataSource {

    // MARK: - ShieldConfigurationDataSource

    override func configuration(
        shielding application: Application
    ) -> ShieldConfiguration {
        makeConfiguration(appName: application.localizedDisplayName ?? "this app")
    }

    override func configuration(
        shielding application: Application,
        in webDomain: WebDomain
    ) -> ShieldConfiguration {
        makeConfiguration(appName: application.localizedDisplayName ?? "this app")
    }

    override func configuration(
        shielding webDomain: WebDomain
    ) -> ShieldConfiguration {
        makeConfiguration(appName: webDomain.domain ?? "this site")
    }

    // MARK: - Build branded ShieldConfiguration

    private func makeConfiguration(appName: String) -> ShieldConfiguration {
        ShieldConfiguration(
            backgroundBlurStyle: .systemUltraThinMaterialDark,
            backgroundColor: .sparkNavy,
            icon: sparkIcon(),
            title: ShieldConfiguration.Label(
                text: "Time's up.",
                color: .sparkWhite
            ),
            subtitle: ShieldConfiguration.Label(
                text: "\(appName) is locked.",
                color: .sparkAmber
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Override (10 FP)",
                color: .sparkWhite
            ),
            primaryButtonBackgroundColor: .sparkPurple,
            secondaryButtonLabel: ShieldConfiguration.Label(
                text: "Go to Spark",
                color: .sparkTeal
            )
        )
    }

    // MARK: - Spark icon

    /// Returns a simple programmatic Spark "spark bolt" icon.
    /// Falls back to a system bolt symbol if image generation fails.
    private func sparkIcon() -> UIImage {
        let size = CGSize(width: 80, height: 80)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            let cgCtx = ctx.cgContext

            // Background circle
            cgCtx.setFillColor(UIColor.sparkPurple.cgColor)
            cgCtx.fillEllipse(in: CGRect(origin: .zero, size: size))

            // Draw lightning bolt path
            let boltPath = UIBezierPath()
            boltPath.move(to: CGPoint(x: 48, y: 10))
            boltPath.addLine(to: CGPoint(x: 28, y: 42))
            boltPath.addLine(to: CGPoint(x: 42, y: 42))
            boltPath.addLine(to: CGPoint(x: 32, y: 70))
            boltPath.addLine(to: CGPoint(x: 54, y: 36))
            boltPath.addLine(to: CGPoint(x: 40, y: 36))
            boltPath.close()

            cgCtx.setFillColor(UIColor.sparkAmber.cgColor)
            boltPath.fill()
        }
        return image
    }
}

// MARK: - Shield Action Handler Extension
// The main app's ShieldActionExtension reads these action identifiers.

extension ShieldConfigurationExtension {
    /// Encodes action info for the companion ShieldActionExtension.
    static func actionIdentifier(for action: SparkShieldAction) -> String {
        action.rawValue
    }
}
