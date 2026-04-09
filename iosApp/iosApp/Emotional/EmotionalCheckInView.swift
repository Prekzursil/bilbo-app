import SwiftUI

// MARK: - Emotion

/// Maps to the shared KMP `Emotion` enum.
enum SparkEmotion: String, CaseIterable, Identifiable {
    case happy    = "HAPPY"
    case calm     = "CALM"
    case bored    = "BORED"
    case stressed = "STRESSED"
    case anxious  = "ANXIOUS"
    case sad      = "SAD"
    case lonely   = "LONELY"

    var id: String { rawValue }

    var emoji: String {
        switch self {
        case .happy:    return "😊"
        case .calm:     return "😌"
        case .bored:    return "😑"
        case .stressed: return "😫"
        case .anxious:  return "😰"
        case .sad:      return "😢"
        case .lonely:   return "😔"
        }
    }

    var label: String {
        switch self {
        case .happy:    return "Happy"
        case .calm:     return "Calm"
        case .bored:    return "Bored"
        case .stressed: return "Stressed"
        case .anxious:  return "Anxious"
        case .sad:      return "Sad"
        case .lonely:   return "Lonely"
        }
    }

    var isNegative: Bool {
        switch self {
        case .bored, .stressed, .anxious, .sad, .lonely: return true
        case .happy, .calm: return false
        }
    }
}

// MARK: - EmotionalCheckInView

/// SwiftUI grid of emotion buttons matching the Android `EmotionalCheckInScreen`.
/// Inserted between the gatekeeper and app launch.
struct EmotionalCheckInView: View {

    let onEmotionSelected: (SparkEmotion) -> Void
    let onSkip: () -> Void

    @State private var selectedEmotion: SparkEmotion? = nil
    @State private var isVisible = false

    // Palette
    private let bgTop      = Color(red: 0.06, green: 0.08, blue: 0.13)
    private let bgBot      = Color(red: 0.05, green: 0.11, blue: 0.16)
    private let cardBg     = Color(red: 0.11, green: 0.15, blue: 0.21)
    private let selected   = Color(red: 0.28, green: 0.72, blue: 0.63)
    private let selectedBg = Color(red: 0.28, green: 0.72, blue: 0.63).opacity(0.13)
    private let onSurface  = Color(red: 0.88, green: 0.93, blue: 0.96)
    private let subtle     = Color(red: 0.54, green: 0.69, blue: 0.77)
    private let surface    = Color(red: 0.14, green: 0.20, blue: 0.27)

    private let columns = [
        GridItem(.flexible(), spacing: 12),
        GridItem(.flexible(), spacing: 12),
    ]

    var body: some View {
        ZStack {
            LinearGradient(
                colors: [bgTop, bgBot],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView {
                VStack(spacing: 0) {
                    Spacer().frame(height: 56)

                    // Header
                    Text("🧠")
                        .font(.system(size: 40))

                    Spacer().frame(height: 16)

                    Text("How are you feeling\nright now?")
                        .font(.system(size: 24, weight: .semibold, design: .rounded))
                        .foregroundColor(onSurface)
                        .multilineTextAlignment(.center)
                        .lineSpacing(6)

                    Spacer().frame(height: 8)

                    Text("Be honest — it helps Spark support you better.")
                        .font(.system(size: 14))
                        .foregroundColor(subtle)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)

                    Spacer().frame(height: 32)

                    // Emotion grid
                    LazyVGrid(columns: columns, spacing: 12) {
                        ForEach(SparkEmotion.allCases) { emotion in
                            EmotionGridCard(
                                emotion: emotion,
                                isSelected: selectedEmotion == emotion,
                                cardBg: cardBg,
                                selectedBg: selectedBg,
                                selectedColor: selected,
                                onSurface: onSurface,
                                surface: surface,
                                onTap: {
                                    withAnimation(.spring(response: 0.3)) {
                                        selectedEmotion = emotion
                                    }
                                    DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                                        onEmotionSelected(emotion)
                                    }
                                }
                            )
                        }
                    }
                    .padding(.horizontal, 24)

                    Spacer().frame(height: 28)

                    // Skip
                    Button(action: onSkip) {
                        Text("Skip")
                            .font(.system(size: 16, weight: .medium, design: .rounded))
                            .foregroundColor(subtle)
                            .padding(.vertical, 12)
                            .padding(.horizontal, 32)
                    }

                    Spacer().frame(height: 40)
                }
            }
        }
        .opacity(isVisible ? 1 : 0)
        .onAppear {
            withAnimation(.easeIn(duration: 0.3)) { isVisible = true }
        }
    }
}

// MARK: - EmotionGridCard

private struct EmotionGridCard: View {
    let emotion: SparkEmotion
    let isSelected: Bool
    let cardBg: Color
    let selectedBg: Color
    let selectedColor: Color
    let onSurface: Color
    let surface: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                Text(emotion.emoji)
                    .font(.system(size: 30))
                Text(emotion.label)
                    .font(.system(size: 14, weight: isSelected ? .semibold : .regular, design: .rounded))
                    .foregroundColor(isSelected ? selectedColor : onSurface)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 20)
            .background(isSelected ? selectedBg : cardBg)
            .cornerRadius(16)
            .overlay(
                RoundedRectangle(cornerRadius: 16)
                    .stroke(
                        isSelected ? selectedColor : surface,
                        lineWidth: isSelected ? 2 : 1
                    )
            )
            .scaleEffect(isSelected ? 1.03 : 1.0)
            .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isSelected)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Preview

#Preview {
    EmotionalCheckInView(
        onEmotionSelected: { _ in },
        onSkip: {}
    )
}
