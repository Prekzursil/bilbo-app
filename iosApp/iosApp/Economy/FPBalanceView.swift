import SwiftUI

// MARK: - FP colour thresholds

private let fpHighThreshold  = 30
private let fpLowThreshold   = 10
private let fpDailyCap: Double = 60

// MARK: - FPBalanceView

/// Reusable SwiftUI component mirroring the Android `FPBalanceWidget`.
///
/// Shows:
/// - A circular progress ring: earned FP vs the daily cap (60).
/// - The current balance as a large, colour-coded number.
/// - A "Focus Points" label below the number.
///
/// Usage:
/// ```swift
/// FPBalanceView(currentBalance: 38, fpEarned: 25)
/// ```
struct FPBalanceView: View {

    let currentBalance: Int
    let fpEarned: Int
    var size: CGFloat = 120

    // Animated values
    @State private var animatedProgress: Double = 0
    @State private var animatedBalance: Int = 0

    // MARK: Derived helpers

    private var balanceColor: Color {
        if currentBalance > fpHighThreshold { return .fpGreen }
        if currentBalance > fpLowThreshold  { return .fpYellow }
        return .fpRed
    }

    private var progressFraction: Double {
        let clamped = Double(max(0, min(fpEarned, Int(fpDailyCap))))
        return clamped / fpDailyCap
    }

    private var ringLineWidth: CGFloat { size * 0.07 }

    // MARK: Body

    var body: some View {
        ZStack {
            // ── Background track ──────────────────────────────────────────
            Circle()
                .stroke(balanceColor.opacity(0.18), lineWidth: ringLineWidth)

            // ── Progress arc ──────────────────────────────────────────────
            Circle()
                .trim(from: 0, to: animatedProgress)
                .stroke(
                    balanceColor,
                    style: StrokeStyle(
                        lineWidth: ringLineWidth,
                        lineCap: .round
                    )
                )
                .rotationEffect(.degrees(-90))
                .animation(.easeInOut(duration: 0.8), value: animatedProgress)

            // ── Centre content ────────────────────────────────────────────
            VStack(spacing: 2) {
                Text("\(animatedBalance)")
                    .font(.system(size: size * 0.25, weight: .bold, design: .rounded))
                    .foregroundColor(balanceColor)
                    .animation(.easeInOut(duration: 0.6), value: animatedBalance)
                    .contentTransition(.numericText())

                Text("Focus Points")
                    .font(.system(size: size * 0.1, weight: .medium))
                    .foregroundColor(.secondary)
            }
        }
        .frame(width: size, height: size)
        .onAppear {
            animatedProgress = progressFraction
            animatedBalance  = currentBalance
        }
        .onChange(of: currentBalance) { newValue in
            withAnimation(.easeInOut(duration: 0.6)) {
                animatedBalance = newValue
            }
        }
        .onChange(of: fpEarned) { _ in
            withAnimation(.easeInOut(duration: 0.8)) {
                animatedProgress = progressFraction
            }
        }
    }
}

// MARK: - Color extensions

extension Color {
    static let fpGreen  = Color(red: 0.298, green: 0.686, blue: 0.314)  // #4CAF50
    static let fpYellow = Color(red: 1.0,   green: 0.757, blue: 0.027)  // #FFC107
    static let fpRed    = Color(red: 0.898, green: 0.224, blue: 0.208)  // #E53935
}

// MARK: - Preview

#Preview("High Balance") {
    FPBalanceView(currentBalance: 45, fpEarned: 30, size: 140)
        .padding()
}

#Preview("Low Balance") {
    FPBalanceView(currentBalance: 7, fpEarned: 8, size: 140)
        .padding()
}

#Preview("Empty Balance") {
    FPBalanceView(currentBalance: 0, fpEarned: 2, size: 140)
        .padding()
}
