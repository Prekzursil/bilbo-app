import SwiftUI

// MARK: - CoolingOffView

/// SwiftUI breathing circle animation — a hard 10-second non-dismissible screen.
///
/// Mirrors the Android `CoolingOffScreen`:
/// - 4s inhale (circle expands), 4s exhale (circle contracts), 2s hold
/// - Cannot be dismissed or skipped
/// - Awards +3 FP via [onComplete]
struct CoolingOffView: View {

    /// Called after the 10-second cycle completes.  Caller should award +3 FP and open the app.
    let onComplete: () -> Void

    // ── State ─────────────────────────────────────────────────────────────────

    @State private var circleScale: CGFloat  = 0.4
    @State private var circleOpacity: Double = 0.6
    @State private var instruction = "Breathe in..."
    @State private var remainingSecs = 10
    @State private var phase: BreathPhase = .inhale
    @State private var progress: Double = 0.0
    @State private var countdownTimer: Timer? = nil

    // ── Palette ───────────────────────────────────────────────────────────────

    private let bgTop      = Color(red: 0.04, green: 0.10, blue: 0.14)
    private let bgBot      = Color(red: 0.05, green: 0.13, blue: 0.20)
    private let tealInhale = Color(red: 0.28, green: 0.72, blue: 0.63)
    private let blueExhale = Color(red: 0.43, green: 0.66, blue: 0.78)
    private let onSurface  = Color(red: 0.83, green: 0.93, blue: 0.96)
    private let subtle     = Color(red: 0.50, green: 0.71, blue: 0.79)

    // ── Phase ─────────────────────────────────────────────────────────────────

    private enum BreathPhase { case inhale, exhale, hold }

    private var circleColor: Color {
        switch phase {
        case .inhale: return tealInhale
        case .exhale: return blueExhale
        case .hold:   return tealInhale.opacity(0.75)
        }
    }

    // ── Body ──────────────────────────────────────────────────────────────────

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [bgTop, bgBot],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            VStack(spacing: 0) {
                Spacer()

                // Instruction
                Text(instruction)
                    .font(.system(size: 28, weight: .light, design: .rounded))
                    .foregroundColor(onSurface)
                    .kerning(0.5)
                    .animation(.easeInOut(duration: 0.5), value: instruction)

                Spacer().frame(height: 48)

                // Breathing circle
                BreathingCircleView(
                    scale: circleScale,
                    color: circleColor,
                    glowColor: circleColor.opacity(0.15)
                )
                .frame(width: 220, height: 220)

                Spacer().frame(height: 48)

                // Countdown number
                Text("\(remainingSecs)")
                    .font(.system(size: 52, weight: .thin, design: .monospaced))
                    .foregroundColor(subtle)
                    .contentTransition(.numericText())
                    .animation(.spring(), value: remainingSecs)

                Text("seconds")
                    .font(.system(size: 14))
                    .foregroundColor(subtle.opacity(0.7))
                    .padding(.top, 4)

                Spacer().frame(height: 32)

                // Progress bar
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 2)
                            .fill(Color.white.opacity(0.1))
                            .frame(height: 4)

                        RoundedRectangle(cornerRadius: 2)
                            .fill(tealInhale)
                            .frame(width: geo.size.width * progress, height: 4)
                            .animation(.linear(duration: 0.5), value: progress)
                    }
                }
                .frame(height: 4)
                .padding(.horizontal, 40)

                Spacer().frame(height: 20)

                Text("+3 FP on completion")
                    .font(.system(size: 12))
                    .foregroundColor(subtle.opacity(0.6))

                Spacer()
            }
        }
        // Block all system navigation gestures
        .navigationBarHidden(true)
        .interactiveDismissDisabled(true)
        .onAppear { startBreathing() }
        .onDisappear { countdownTimer?.invalidate() }
    }

    // ── Animation sequence ────────────────────────────────────────────────────

    private func startBreathing() {
        // Countdown ticker
        var elapsed = 0
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { timer in
            elapsed += 1
            remainingSecs = max(0, 10 - elapsed)
            progress = Double(elapsed) / 10.0

            if elapsed >= 10 {
                timer.invalidate()
                countdownTimer = nil
            }
        }

        // Phase 1: Inhale (0–4 s) — expand
        phase = .inhale
        instruction = "Breathe in..."
        withAnimation(.easeInOut(duration: 4.0)) {
            circleScale = 1.0
        }

        // Phase 2: Exhale (4–8 s) — contract
        DispatchQueue.main.asyncAfter(deadline: .now() + 4.0) {
            guard remainingSecs > 0 else { return }
            phase = .exhale
            instruction = "Breathe out..."
            withAnimation(.easeInOut(duration: 4.0)) {
                circleScale = 0.4
            }
        }

        // Phase 3: Hold (8–10 s) — static
        DispatchQueue.main.asyncAfter(deadline: .now() + 8.0) {
            guard remainingSecs > 0 else { return }
            phase = .hold
            instruction = "Hold..."
        }

        // Complete
        DispatchQueue.main.asyncAfter(deadline: .now() + 10.0) {
            progress = 1.0
            remainingSecs = 0
            onComplete()
        }
    }
}

// MARK: - BreathingCircleView

private struct BreathingCircleView: View {
    let scale: CGFloat
    let color: Color
    let glowColor: Color

    @State private var glowScale: CGFloat = 1.0

    var body: some View {
        ZStack {
            // Outer glow ring
            Circle()
                .fill(glowColor)
                .scaleEffect(scale * 1.25)

            // Mid ring
            Circle()
                .fill(color.opacity(0.12))
                .scaleEffect(scale * 1.10)

            // Main circle — radial fill simulation
            Circle()
                .fill(
                    RadialGradient(
                        gradient: Gradient(colors: [
                            color.opacity(0.55),
                            color.opacity(0.20),
                            color.opacity(0.05),
                        ]),
                        center: .center,
                        startRadius: 0,
                        endRadius: 110
                    )
                )
                .scaleEffect(scale)

            // Stroke border
            Circle()
                .stroke(color.opacity(0.6), lineWidth: 2)
                .scaleEffect(scale)
        }
        .animation(.easeInOut(duration: 0.3), value: color)
    }
}

// MARK: - Preview

#Preview {
    CoolingOffView(onComplete: {})
}
