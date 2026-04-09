import SwiftUI

// MARK: - EnforcementMode

enum EnforcementDisplayMode {
    case nudge(declaredMinutes: Int, actualMinutes: Int)
    case hardLock(cooldownMinutes: Int, remainingSeconds: Int)
}

// MARK: - EnforcementView

/// SwiftUI overlay view with two modes matching the Android enforcement overlays:
/// - `.nudge`: Translucent top card — user can dismiss or pay 5 FP for 5 extra minutes.
/// - `.hardLock`: Full-screen blocking overlay with countdown timer and override option.
struct EnforcementView: View {

    let appName: String
    let mode: EnforcementDisplayMode
    let fpBalance: Int
    let suggestion: String
    let onGotIt: () -> Void                 // nudge only
    let onExtend5Min: () -> Void            // nudge only
    let onGoHome: () -> Void               // hard lock only
    let onOverride: () -> Void             // hard lock only

    var body: some View {
        switch mode {
        case let .nudge(declared, actual):
            NudgeCard(
                appName: appName,
                declaredMinutes: declared,
                actualMinutes: actual,
                fpBalance: fpBalance,
                onGotIt: onGotIt,
                onExtend5Min: onExtend5Min
            )
        case let .hardLock(cooldownMinutes, remainingSeconds):
            HardLockView(
                appName: appName,
                cooldownMinutes: cooldownMinutes,
                initialRemainingSeconds: remainingSeconds,
                suggestion: suggestion,
                fpBalance: fpBalance,
                onGoHome: onGoHome,
                onOverride: onOverride
            )
        }
    }
}

// MARK: - NudgeCard

/// Translucent amber-toned card that slides in from the top.
private struct NudgeCard: View {

    let appName: String
    let declaredMinutes: Int
    let actualMinutes: Int
    let fpBalance: Int
    let onGotIt: () -> Void
    let onExtend5Min: () -> Void

    @State private var isVisible = false

    // Palette
    private let cardBg    = Color(red: 0.17, green: 0.12, blue: 0.055)
    private let amber     = Color(red: 0.96, green: 0.65, blue: 0.14)
    private let amberSoft = Color(red: 1.0, green: 0.82, blue: 0.50)
    private let onSurface = Color(red: 0.96, green: 0.93, blue: 0.84)
    private let subtle    = Color(red: 0.67, green: 0.55, blue: 0.42)
    private let green     = Color(red: 0.42, green: 0.80, blue: 0.47)
    private let scrim     = Color.black.opacity(0.45)

    var canExtend: Bool { fpBalance >= 5 }

    var body: some View {
        ZStack(alignment: .top) {
            scrim
                .ignoresSafeArea()
                .allowsHitTesting(false)

            VStack {
                cardContent
                    .offset(y: isVisible ? 0 : -300)
                    .animation(
                        .spring(response: 0.42, dampingFraction: 0.78),
                        value: isVisible
                    )
                    .opacity(isVisible ? 1 : 0)
                    .animation(.easeIn(duration: 0.25), value: isVisible)

                Spacer()
            }
            .padding(.horizontal, 16)
            .padding(.top, 24)
        }
        .onAppear { withAnimation { isVisible = true } }
    }

    private var cardContent: some View {
        VStack(spacing: 0) {
            VStack(spacing: 16) {
                // Clock emoji
                Text("⏰")
                    .font(.system(size: 38))

                // Title
                Group {
                    Text("Time's up! Your ")
                        .foregroundColor(onSurface)
                    + Text("\(declaredMinutes) min")
                        .foregroundColor(amber)
                        .fontWeight(.bold)
                    + Text(" on ")
                        .foregroundColor(onSurface)
                    + Text(appName)
                        .foregroundColor(amberSoft)
                        .fontWeight(.semibold)
                    + Text(" is over.")
                        .foregroundColor(onSurface)
                }
                .font(.system(size: 17, weight: .medium, design: .rounded))
                .multilineTextAlignment(.center)
                .lineSpacing(4)

                // Actual time
                Text("You've been here for \(actualMinutes) minute\(actualMinutes == 1 ? "" : "s").")
                    .font(.system(size: 14))
                    .foregroundColor(subtle)
                    .multilineTextAlignment(.center)

                // Got it
                Button(action: onGotIt) {
                    Text("Got it")
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(green)
                        .cornerRadius(14)
                }

                // Extension
                if canExtend {
                    Button(action: onExtend5Min) {
                        Text("5 more minutes  (−5 FP)")
                            .font(.system(size: 15, weight: .medium, design: .rounded))
                            .foregroundColor(amber)
                            .frame(maxWidth: .infinity)
                            .frame(height: 48)
                            .overlay(
                                RoundedRectangle(cornerRadius: 14)
                                    .stroke(amber.opacity(0.5), lineWidth: 1)
                            )
                    }
                } else {
                    Text("Balance: \(fpBalance) FP · Not enough to extend")
                        .font(.system(size: 12))
                        .foregroundColor(subtle)
                }
            }
            .padding(24)
        }
        .background(cardBg)
        .cornerRadius(20)
        .shadow(color: Color.black.opacity(0.4), radius: 16, x: 0, y: 8)
    }
}

// MARK: - HardLockView

/// Full-screen, opaque blocking view with a live countdown.
private struct HardLockView: View {

    let appName: String
    let cooldownMinutes: Int
    let initialRemainingSeconds: Int
    let suggestion: String
    let fpBalance: Int
    let onGoHome: () -> Void
    let onOverride: () -> Void

    @State private var remainingSeconds: Int = 0
    @State private var showOverrideAlert = false
    @State private var timer: Timer? = nil

    private let gradientTop   = Color(red: 0.05, green: 0.11, blue: 0.24)
    private let gradientBot   = Color(red: 0.10, green: 0.05, blue: 0.20)
    private let purple        = Color(red: 0.48, green: 0.38, blue: 1.0)
    private let onSurface     = Color(red: 0.87, green: 0.89, blue: 0.96)
    private let subtle        = Color(red: 0.53, green: 0.54, blue: 0.69)
    private let cardSurface   = Color(red: 0.07, green: 0.11, blue: 0.20)
    private let green         = Color(red: 0.39, green: 0.85, blue: 0.66)
    private let red           = Color(red: 1.0, green: 0.42, blue: 0.42)

    var canOverride: Bool { fpBalance >= 10 }

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [gradientTop, gradientBot],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // Lock icon (pulsing)
                PulsingLockIcon(color: purple)

                Spacer().frame(height: 24)

                // Header
                Group {
                    Text("Time's up. ")
                        .foregroundColor(onSurface)
                    + Text(appName)
                        .foregroundColor(purple)
                        .fontWeight(.bold)
                    + Text(" is locked for \(cooldownMinutes) minutes.")
                        .foregroundColor(onSurface)
                }
                .font(.system(size: 22, weight: .semibold, design: .rounded))
                .multilineTextAlignment(.center)
                .lineSpacing(6)
                .padding(.horizontal, 28)

                Spacer().frame(height: 32)

                // Countdown
                CountdownDisplay(remainingSeconds: remainingSeconds, accentColor: onSurface)

                Spacer().frame(height: 32)

                // Suggestion card
                SuggestionCard(
                    suggestion: suggestion,
                    cardColor: cardSurface,
                    accentColor: purple,
                    textColor: onSurface,
                    subtleColor: subtle
                )
                .padding(.horizontal, 28)

                Spacer().frame(height: 40)

                // Go Home
                Button(action: onGoHome) {
                    Text("Go Home")
                        .font(.system(size: 17, weight: .bold, design: .rounded))
                        .foregroundColor(Color(red: 0, green: 0.1, blue: 0.05))
                        .frame(maxWidth: .infinity)
                        .frame(height: 56)
                        .background(green)
                        .cornerRadius(16)
                }
                .padding(.horizontal, 28)

                Spacer().frame(height: 12)

                // Override
                Button(action: { showOverrideAlert = true }) {
                    Text(canOverride ? "Override (costs 10 FP)" : "Override (not enough FP)")
                        .font(.system(size: 15))
                        .foregroundColor(canOverride ? red.opacity(0.85) : subtle)
                }

                Spacer().frame(height: 8)

                Text("Balance: \(fpBalance) FP")
                    .font(.system(size: 12))
                    .foregroundColor(subtle)

                Spacer()
            }
        }
        .onAppear {
            remainingSeconds = initialRemainingSeconds
            startCountdown()
        }
        .onDisappear { timer?.invalidate() }
        .alert("Override Lock?", isPresented: $showOverrideAlert) {
            if canOverride {
                Button("Yes, override", role: .destructive) { onOverride() }
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            if canOverride {
                Text("Override costs 10 Focus Points.\nCurrent balance: \(fpBalance) FP.\nContinue?")
            } else {
                Text("Not enough Focus Points to override.\nYou need 10 FP but only have \(fpBalance) FP.")
            }
        }
    }

    private func startCountdown() {
        timer?.invalidate()
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            if remainingSeconds > 0 {
                remainingSeconds -= 1
            } else {
                timer?.invalidate()
            }
        }
    }
}

// MARK: - Supporting Components

private struct PulsingLockIcon: View {
    let color: Color
    @State private var scale: CGFloat = 0.95

    var body: some View {
        ZStack {
            Circle()
                .fill(color.opacity(0.15))
                .frame(width: 80, height: 80)
            Circle()
                .stroke(color.opacity(0.4), lineWidth: 1)
                .frame(width: 80, height: 80)
            Text("🔒")
                .font(.system(size: 36))
        }
        .scaleEffect(scale)
        .onAppear {
            withAnimation(
                .easeInOut(duration: 2.0).repeatForever(autoreverses: true)
            ) { scale = 1.05 }
        }
    }
}

private struct CountdownDisplay: View {
    let remainingSeconds: Int
    let accentColor: Color

    private var timeString: String {
        let h = remainingSeconds / 3600
        let m = (remainingSeconds % 3600) / 60
        let s = remainingSeconds % 60
        if h > 0 {
            return String(format: "%02d:%02d:%02d", h, m, s)
        } else {
            return String(format: "%02d:%02d", m, s)
        }
    }

    var body: some View {
        Text(timeString)
            .font(.system(size: 52, weight: .light, design: .monospaced))
            .foregroundColor(accentColor)
            .padding(.horizontal, 32)
            .padding(.vertical, 16)
            .background(Color.white.opacity(0.05))
            .cornerRadius(20)
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .stroke(Color.white.opacity(0.1), lineWidth: 1)
            )
    }
}

private struct SuggestionCard: View {
    let suggestion: String
    let cardColor: Color
    let accentColor: Color
    let textColor: Color
    let subtleColor: Color

    var body: some View {
        VStack(spacing: 6) {
            Text("While you wait, try:")
                .font(.system(size: 11, weight: .medium))
                .kerning(0.8)
                .foregroundColor(subtleColor)
            Text(suggestion)
                .font(.system(size: 16, weight: .medium, design: .rounded))
                .foregroundColor(textColor)
                .multilineTextAlignment(.center)
        }
        .padding(18)
        .frame(maxWidth: .infinity)
        .background(cardColor)
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(accentColor.opacity(0.2), lineWidth: 1)
        )
    }
}

// MARK: - Preview

#Preview("Nudge") {
    ZStack {
        Color.black.ignoresSafeArea()
        EnforcementView(
            appName: "Instagram",
            mode: .nudge(declaredMinutes: 15, actualMinutes: 18),
            fpBalance: 42,
            suggestion: "Take a short walk outside 🚶",
            onGotIt: {},
            onExtend5Min: {},
            onGoHome: {},
            onOverride: {}
        )
    }
}

#Preview("HardLock") {
    EnforcementView(
        appName: "TikTok",
        mode: .hardLock(cooldownMinutes: 30, remainingSeconds: 1724),
        fpBalance: 15,
        suggestion: "Read a chapter of your book 📖",
        onGotIt: {},
        onExtend5Min: {},
        onGoHome: {},
        onOverride: {}
    )
}
