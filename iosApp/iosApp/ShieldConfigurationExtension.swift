// ShieldConfigurationExtension.swift
// Bilbo — Shield Configuration Extension
//
// Provides branded Bilbo shield UI for Hard Lock mode.
// Deep blue/purple palette matching the Android brand.
// Two actions: override with FP cost, or navigate to Bilbo.

import ManagedSettings
import ManagedSettingsUI
import UIKit

// MARK: - Colors

private extension UIColor {
    /// Bilbo deep navy — primary shield background
    static let bilboNavy = UIColor(red: 0.094, green: 0.133, blue: 0.259, alpha: 1)
    /// Bilbo deep purple — accent
    static let bilboPurple = UIColor(red: 0.345, green: 0.204, blue: 0.635, alpha: 1)
    /// Bilbo teal — CTA accent
    static let bilboTeal = UIColor(red: 0.000, green: 0.537, blue: 0.482, alpha: 1)
    /// Off-white text
    static let bilboWhite = UIColor(red: 0.980, green: 0.980, blue: 0.996, alpha: 1)
    /// Amber warning
    static let bilboAmber = UIColor(red: 1.000, green: 0.702, blue: 0.000, alpha: 1)
}

// MARK: - Shield action names

enum BilboShieldAction: String {
    case overrideFP   = "override_fp"       // "Override (10 FP)"
    case goToBilbo    = "go_to_bilbo"       // "Go to Bilbo"
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
            backgroundColor: .bilboNavy,
            icon: bilboIcon(),
            title: ShieldConfiguration.Label(
                text: "Time's up.",
                color: .bilboWhite
            ),
            subtitle: ShieldConfiguration.Label(
                text: "\(appName) is locked.",
                color: .bilboAmber
            ),
            primaryButtonLabel: ShieldConfiguration.Label(
                text: "Override (10 FP)",
                color: .bilboWhite
            ),
            primaryButtonBackgroundColor: .bilboPurple,
            secondaryButtonLabel: ShieldConfiguration.Label(
                text: "Go to Bilbo",
                color: .bilboTeal
            )
        )
    }

    // MARK: - Bilbo icon

    /// Returns a simple programmatic Bilbo icon.
    /// Falls back to a system bolt symbol if image generation fails.
    private func bilboIcon() -> UIImage {
        let size = CGSize(width: 80, height: 80)
        let renderer = UIGraphicsImageRenderer(size: size)
        let image = renderer.image { ctx in
            let cgCtx = ctx.cgContext

            // Background circle
            cgCtx.setFillColor(UIColor.bilboPurple.cgColor)
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

            cgCtx.setFillColor(UIColor.bilboAmber.cgColor)
            boltPath.fill()
        }
        return image
    }
}

// MARK: - Shield Action Handler Extension
// The main app's ShieldActionExtension reads these action identifiers.

extension ShieldConfigurationExtension {
    /// Encodes action info for the companion ShieldActionExtension.
    static func actionIdentifier(for action: BilboShieldAction) -> String {
        action.rawValue
    }
}
