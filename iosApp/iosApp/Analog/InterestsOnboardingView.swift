import SwiftUI

// MARK: - Constants

private let minSelections = 2

// MARK: - Interest entry

private struct InterestEntry: Identifiable {
    let id: SuggestionCategory
    let emoji: String
    let label: String
}

private let allInterests: [InterestEntry] = [
    InterestEntry(id: .reading,        emoji: "📚", label: "Reading"),
    InterestEntry(id: .exercise,       emoji: "💪", label: "Exercise"),
    InterestEntry(id: .cooking,        emoji: "🍳", label: "Cooking"),
    InterestEntry(id: .creative,       emoji: "🎨", label: "Art"),
    InterestEntry(id: .music,          emoji: "🎵", label: "Music"),
    InterestEntry(id: .nature,         emoji: "🌿", label: "Nature"),
    InterestEntry(id: .social,         emoji: "👥", label: "Social"),
    InterestEntry(id: .mindfulness,    emoji: "🧘", label: "Mindfulness"),
    InterestEntry(id: .gamingPhysical, emoji: "🎲", label: "Physical Games"),
    InterestEntry(id: .learning,       emoji: "📖", label: "Learning"),
]

// MARK: - InterestsOnboardingView

/// First-run screen for selecting offline interests.
/// Mirrors the Android `InterestsOnboardingScreen`.
///
/// Displays a flow grid of selectable chips — one per `SuggestionCategory`.
/// "Continue" is enabled once at least `minSelections` categories are chosen.
/// Calls `onContinue` with the final selection set, which should be persisted
/// to `UserDefaults` (iOS equivalent of `BilboPreferences`).
struct InterestsOnboardingView: View {

    /// Categories pre-selected (pass populated set when re-entering this screen).
    var initialSelections: Set<SuggestionCategory> = []
    var onContinue: (Set<SuggestionCategory>) -> Void = { _ in }

    @State private var selected: Set<SuggestionCategory> = []

    // Chip palette
    private let chipSelected     = Color(red: 0.176, green: 0.416, blue: 0.310)  // #2D6A4F
    private let chipSelectedText = Color(red: 0.847, green: 0.953, blue: 0.863)  // #D8F3DC

    private var canContinue: Bool { selected.count >= minSelections }

    // MARK: Body

    var body: some View {
        VStack(spacing: 0) {
            ScrollView {
                VStack(spacing: 32) {

                    // ── Header ────────────────────────────────────────
                    VStack(spacing: 8) {
                        Text("What do you enjoy offline?")
                            .font(.largeTitle)
                            .fontWeight(.bold)
                            .multilineTextAlignment(.center)

                        Text(
                            "We'll suggest activities that match your interests " +
                            "when you feel like reaching for your phone."
                        )
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                    }
                    .padding(.top, 40)
                    .padding(.horizontal, 24)

                    // ── Interest chip grid ────────────────────────────
                    InterestChipGrid(
                        interests: allInterests,
                        selected: $selected,
                        chipSelected: chipSelected,
                        chipSelectedText: chipSelectedText
                    )
                    .padding(.horizontal, 20)

                    Spacer(minLength: 40)
                }
            }

            // ── Sticky bottom button ──────────────────────────────────
            VStack(spacing: 0) {
                Divider()
                Button(action: { onContinue(selected) }) {
                    Text(
                        canContinue
                            ? "Continue (\(selected.count) selected)"
                            : "Select at least \(minSelections) interests"
                    )
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 14)
                    .background(canContinue ? chipSelected : Color(.systemGray4))
                    .foregroundColor(canContinue ? chipSelectedText : .secondary)
                    .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .disabled(!canContinue)
                .animation(.easeInOut(duration: 0.2), value: canContinue)
                .padding(.horizontal, 24)
                .padding(.vertical, 16)
                .padding(.bottom, 8)
            }
            .background(Color(.systemBackground))
        }
        .onAppear {
            selected = initialSelections
        }
    }
}

// MARK: - Interest Chip Grid

private struct InterestChipGrid: View {
    let interests: [InterestEntry]
    @Binding var selected: Set<SuggestionCategory>
    let chipSelected: Color
    let chipSelectedText: Color

    var body: some View {
        FlowLayout(spacing: 10) {
            ForEach(interests) { entry in
                InterestChip(
                    entry: entry,
                    isSelected: selected.contains(entry.id),
                    chipSelected: chipSelected,
                    chipSelectedText: chipSelectedText
                ) {
                    withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                        if selected.contains(entry.id) {
                            selected.remove(entry.id)
                        } else {
                            selected.insert(entry.id)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Interest Chip

private struct InterestChip: View {
    let entry: InterestEntry
    let isSelected: Bool
    let chipSelected: Color
    let chipSelectedText: Color
    let action: () -> Void

    private var containerColor: Color {
        isSelected ? chipSelected : Color(.systemGray5)
    }
    private var textColor: Color {
        isSelected ? chipSelectedText : .primary
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                Text(entry.emoji)
                    .font(.body)
                Text(entry.label)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(containerColor)
            .foregroundColor(textColor)
            .clipShape(RoundedRectangle(cornerRadius: 12))
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        isSelected ? chipSelected.opacity(0.6) : Color(.systemGray4),
                        lineWidth: 1.5
                    )
            )
            .scaleEffect(isSelected ? 1.04 : 1.0)
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.25, dampingFraction: 0.65), value: isSelected)
    }
}

// MARK: - Preview

#Preview("Empty Selection") {
    InterestsOnboardingView()
}

#Preview("Pre-selected") {
    InterestsOnboardingView(
        initialSelections: [.reading, .exercise, .nature]
    )
}
