import SwiftUI

// MARK: - AIInterventionView

/// SwiftUI card shown after a negative emotion check-in when the user is about
/// to open an Empty Calorie app.  Mirrors the Android `AIInterventionCard`.
///
/// Presents a heuristic-backed pattern observation (emotion → app → typical
/// duration → post-mood) and offers a choice:
/// - "Yes, let me breathe" → navigates to `CoolingOffView`
/// - "Continue to {app}" → proceeds normally (with optional cooling-off)
struct AIInterventionView: View {

    let emotion: BilboEmotion
    let appName: String
    let avgDurationMins: Int
    let postMood: BilboEmotion?
    let onBreathe: () -> Void
    let onContinue: () -> Void

    @State private var isVisible = false

    // ── Palette ───────────────────────────────────────────────────────────────

    private let bgScrim     = Color.black.opacity(0.55)
    private let cardBg      = Color(red: 0.12, green: 0.08, blue: 0.17)
    private let purple      = Color(red: 0.69, green: 0.55, blue: 1.0)
    private let purpleDim   = Color(red: 0.48, green: 0.36, blue: 0.75)
    private let onSurface   = Color(red: 0.93, green: 0.91, blue: 0.97)
    private let subtle      = Color(red: 0.60, green: 0.54, blue: 0.72)
    private let green       = Color(red: 0.42, green: 0.80, blue: 0.47)
    private let neutral     = Color(red: 0.35, green: 0.35, blue: 0.42)

    // ── Body ──────────────────────────────────────────────────────────────────

    var body: some View {
        ZStack {
            bgScrim
                .ignoresSafeArea()
                .allowsHitTesting(false)

            VStack {
                Spacer()

                cardContent
                    .offset(y: isVisible ? 0 : 120)
                    .opacity(isVisible ? 1 : 0)
                    .animation(.spring(response: 0.45, dampingFraction: 0.80), value: isVisible)
                    .padding(.horizontal, 20)
                    .padding(.bottom, 32)
            }
        }
        .onAppear {
            withAnimation { isVisible = true }
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────

    private var cardContent: some View {
        VStack(spacing: 0) {
            VStack(spacing: 20) {

                // Icon
                Text("✨")
                    .font(.system(size: 34))

                // Pattern observation
                patternText
                    .multilineTextAlignment(.center)

                Divider()
                    .background(purpleDim.opacity(0.3))

                // Prompt
                Text("Would you like to try 2 minutes\nof breathing instead?")
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundColor(onSurface)
                    .multilineTextAlignment(.center)
                    .lineSpacing(4)

                // Breathe CTA
                Button(action: onBreathe) {
                    HStack(spacing: 8) {
                        Text("Yes, let me breathe 🌬️")
                            .font(.system(size: 16, weight: .semibold, design: .rounded))
                    }
                    .foregroundColor(Color(red: 0, green: 0.1, blue: 0.05))
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(green)
                    .cornerRadius(14)
                }

                // Continue
                Button(action: onContinue) {
                    Text("Continue to \(appName)")
                        .font(.system(size: 15, weight: .regular, design: .rounded))
                        .foregroundColor(subtle)
                        .frame(maxWidth: .infinity)
                        .frame(height: 48)
                        .overlay(
                            RoundedRectangle(cornerRadius: 14)
                                .stroke(neutral.opacity(0.5), lineWidth: 1)
                        )
                }
            }
            .padding(28)
        }
        .background(cardBg)
        .cornerRadius(24)
        .shadow(color: Color.black.opacity(0.45), radius: 20, x: 0, y: 8)
    }

    // ── Pattern text ──────────────────────────────────────────────────────────

    private var patternText: some View {
        VStack(spacing: 4) {
            Group {
                Text("When you feel ")
                    .foregroundColor(subtle)
                + Text(emotion.emoji + " " + emotion.label.lowercased())
                    .foregroundColor(purple)
                    .fontWeight(.semibold)
                + Text(", you tend to use ")
                    .foregroundColor(subtle)
                + Text(appName)
                    .foregroundColor(purple)
                    .fontWeight(.semibold)
                + Text(" for about ")
                    .foregroundColor(subtle)
                + Text("\(avgDurationMins) min")
                    .foregroundColor(purple)
                    .fontWeight(.semibold)
                + Text(".")
                    .foregroundColor(subtle)
            }
            .font(.system(size: 14))
            .lineSpacing(4)

            if let post = postMood {
                Spacer().frame(height: 4)
                Group {
                    Text("Afterward you usually feel ")
                        .foregroundColor(subtle)
                    + Text(post.emoji + " " + post.label.lowercased())
                        .foregroundColor(purple)
                        .fontWeight(.semibold)
                    + Text(".")
                        .foregroundColor(subtle)
                }
                .font(.system(size: 14))
            }
        }
    }
}

// MARK: - Preview

#Preview {
    ZStack {
        Color.black.ignoresSafeArea()
        AIInterventionView(
            emotion: .anxious,
            appName: "TikTok",
            avgDurationMins: 23,
            postMood: .sad,
            onBreathe: {},
            onContinue: {}
        )
    }
}
