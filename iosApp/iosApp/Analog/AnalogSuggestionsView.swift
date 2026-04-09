import SwiftUI

// MARK: - Domain models (mirroring shared KMP types)

enum SuggestionCategory: String, CaseIterable, Identifiable {
    case reading         = "READING"
    case exercise        = "EXERCISE"
    case cooking         = "COOKING"
    case creative        = "CREATIVE"
    case music           = "MUSIC"
    case nature          = "NATURE"
    case social          = "SOCIAL"
    case mindfulness     = "MINDFULNESS"
    case gamingPhysical  = "GAMING_PHYSICAL"
    case learning        = "LEARNING"

    var id: String { rawValue }

    var emoji: String {
        switch self {
        case .reading:        return "📚"
        case .exercise:       return "💪"
        case .cooking:        return "🍳"
        case .creative:       return "🎨"
        case .music:          return "🎵"
        case .nature:         return "🌿"
        case .social:         return "👥"
        case .mindfulness:    return "🧘"
        case .gamingPhysical: return "🎲"
        case .learning:       return "📖"
        }
    }

    var label: String {
        switch self {
        case .reading:        return "Reading"
        case .exercise:       return "Exercise"
        case .cooking:        return "Cooking"
        case .creative:       return "Creative"
        case .music:          return "Music"
        case .nature:         return "Nature"
        case .social:         return "Social"
        case .mindfulness:    return "Mindfulness"
        case .gamingPhysical: return "Physical Games"
        case .learning:       return "Learning"
        }
    }
}

enum SuggestionTimeOfDay: String, CaseIterable {
    case morning   = "MORNING"
    case afternoon = "AFTERNOON"
    case evening   = "EVENING"
    case night     = "NIGHT"

    var label: String { rawValue.capitalized }
}

struct AnalogSuggestion: Identifiable {
    let id: Int64
    var text: String
    var category: SuggestionCategory
    var tags: [String]
    var timeOfDay: SuggestionTimeOfDay?
    var timesShown: Int
    var timesAccepted: Int
    var isCustom: Bool
}

// MARK: - UI State

struct AnalogSuggestionsUiState {
    var activeSuggestions: [AnalogSuggestion]  = []
    var customSuggestions: [AnalogSuggestion]  = []
    var isLoading: Bool                        = false
}

// MARK: - AnalogSuggestionsView

/// Full-screen analog suggestions browser.
/// Mirrors the Android `AnalogSuggestionsScreen`.
struct AnalogSuggestionsView: View {

    @State var state: AnalogSuggestionsUiState
    var onAccept: (Int64) -> Void       = { _ in }
    var onShowAnother: (Int64) -> Void  = { _ in }
    var onAddCustom: (AnalogSuggestion) -> Void = { _ in }
    var onDeleteCustom: (Int64) -> Void = { _ in }
    var onBack: (() -> Void)?           = nil

    @State private var showAddDialog = false

    // MARK: Body

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16, pinnedViews: []) {

                    // ── Header ────────────────────────────────────────
                    InspirationHeader()
                        .padding(.horizontal, 16)
                        .padding(.top, 8)

                    // ── Active suggestion cards ───────────────────────
                    ForEach(state.activeSuggestions) { suggestion in
                        AnalogSuggestionCard(
                            suggestion: suggestion,
                            onAccept:   { onAccept(suggestion.id) },
                            onShowAnother: { onShowAnother(suggestion.id) }
                        )
                        .padding(.horizontal, 16)
                        .transition(.move(edge: .bottom).combined(with: .opacity))
                    }

                    // ── Custom suggestions section ────────────────────
                    if !state.customSuggestions.isEmpty {
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Your Custom Suggestions")
                                .font(.headline)
                                .fontWeight(.semibold)
                                .padding(.horizontal, 16)

                            ForEach(state.customSuggestions) { suggestion in
                                CustomSuggestionRow(
                                    suggestion: suggestion,
                                    onDelete: { onDeleteCustom(suggestion.id) }
                                )
                                .padding(.horizontal, 16)
                            }
                        }
                    }

                    Spacer(minLength: 100)
                }
                .padding(.bottom, 24)
            }
            .navigationTitle("Analog Alternatives")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                if let back = onBack {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: back) {
                            Image(systemName: "chevron.left")
                        }
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showAddDialog = true }) {
                        Image(systemName: "plus")
                            .font(.system(size: 16, weight: .semibold))
                    }
                }
            }
            .sheet(isPresented: $showAddDialog) {
                CustomSuggestionSheet { newSuggestion in
                    onAddCustom(newSuggestion)
                    showAddDialog = false
                } onDismiss: {
                    showAddDialog = false
                }
            }
        }
    }
}

// MARK: - Inspiration Header

private struct InspirationHeader: View {
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text("Need inspiration?")
                .font(.largeTitle)
                .fontWeight(.bold)

            Text("Step away from the screen — here are some ideas.")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }
}

// MARK: - Analog Suggestion Card

struct AnalogSuggestionCard: View {

    let suggestion: AnalogSuggestion
    var onAccept: () -> Void
    var onShowAnother: () -> Void

    // Card flip state
    @State private var flipping = false
    @State private var rotationDegrees: Double = 0

    // Nature-inspired green palette
    private let cardGreen       = Color(red: 0.176, green: 0.416, blue: 0.310)  // #2D6A4F
    private let cardGreenLight  = Color(red: 0.322, green: 0.718, blue: 0.533)  // #52B788
    private let cardOnGreen     = Color(red: 0.847, green: 0.953, blue: 0.863)  // #D8F3DC
    private let cardSubtle      = Color(red: 0.718, green: 0.894, blue: 0.780)  // #B7E4C7

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {

            // ── Category badge ────────────────────────────────────────
            HStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(cardGreenLight.opacity(0.3))
                        .frame(width: 36, height: 36)
                    Text(suggestion.category.emoji)
                        .font(.title3)
                }

                Text(suggestion.category.label)
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(cardSubtle)

                Spacer()

                if let tod = suggestion.timeOfDay {
                    Text(tod.label)
                        .font(.caption2)
                        .foregroundColor(cardSubtle)
                }
            }

            // ── Suggestion text ───────────────────────────────────────
            Text(suggestion.text)
                .font(.body)
                .fontWeight(.medium)
                .foregroundColor(cardOnGreen)

            Spacer(minLength: 4)

            // ── Accept button ─────────────────────────────────────────
            Button(action: onAccept) {
                Text("I'll do this! (+5 FP)")
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .background(cardGreenLight)
                    .foregroundColor(.white)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
            }

            // ── Show another button ───────────────────────────────────
            Button(action: triggerFlip) {
                Text("Show another")
                    .font(.subheadline)
                    .foregroundColor(cardSubtle)
                    .frame(maxWidth: .infinity)
            }
        }
        .padding(20)
        .background(cardGreen)
        .clipShape(RoundedRectangle(cornerRadius: 20))
        .shadow(color: .black.opacity(0.12), radius: 4, x: 0, y: 2)
        .rotation3DEffect(
            .degrees(rotationDegrees),
            axis: (x: 1, y: 0, z: 0),
            perspective: 0.3
        )
    }

    private func triggerFlip() {
        guard !flipping else { return }
        flipping = true
        withAnimation(.easeInOut(duration: 0.22)) {
            rotationDegrees = 90
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.22) {
            onShowAnother()
            rotationDegrees = -90
            withAnimation(.easeInOut(duration: 0.22)) {
                rotationDegrees = 0
            }
            flipping = false
        }
    }
}

// MARK: - Custom Suggestion Row

private struct CustomSuggestionRow: View {
    let suggestion: AnalogSuggestion
    let onDelete: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            Text(suggestion.category.emoji)
                .font(.title3)

            VStack(alignment: .leading, spacing: 2) {
                Text(suggestion.text)
                    .font(.subheadline)
                    .lineLimit(2)

                Text(suggestion.category.label)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()

            Button(action: onDelete) {
                Image(systemName: "trash")
                    .foregroundColor(.red)
                    .font(.system(size: 16))
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14))
    }
}

// MARK: - Add Custom Suggestion Sheet

private struct CustomSuggestionSheet: View {
    var onSave: (AnalogSuggestion) -> Void
    var onDismiss: () -> Void

    @State private var text: String = ""
    @State private var selectedCategory: SuggestionCategory? = nil
    @State private var selectedTimeOfDay: SuggestionTimeOfDay? = nil
    @State private var textError: String? = nil
    @State private var categoryError: String? = nil

    private let chipGreen     = Color(red: 0.176, green: 0.416, blue: 0.310)
    private let chipGreenText = Color(red: 0.847, green: 0.953, blue: 0.863)

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {

                    // ── Text input ────────────────────────────────────
                    VStack(alignment: .leading, spacing: 4) {
                        Text("What will you do?")
                            .font(.subheadline).fontWeight(.medium)

                        TextField("e.g. Make a cup of tea and read a chapter", text: $text, axis: .vertical)
                            .lineLimit(3, reservesSpace: true)
                            .padding(12)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .onChange(of: text) { _, _ in textError = nil }

                        if let err = textError {
                            Text(err).font(.caption).foregroundColor(.red)
                        }
                    }

                    // ── Category chips ────────────────────────────────
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Category")
                            .font(.subheadline).fontWeight(.medium)

                        if let err = categoryError {
                            Text(err).font(.caption).foregroundColor(.red)
                        }

                        FlowLayout(spacing: 8) {
                            ForEach(SuggestionCategory.allCases) { cat in
                                let selected = selectedCategory == cat
                                Button(action: {
                                    selectedCategory = selected ? nil : cat
                                    categoryError = nil
                                }) {
                                    Text("\(cat.emoji) \(cat.label)")
                                        .font(.caption)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(selected ? chipGreen : Color(.systemGray5))
                                        .foregroundColor(selected ? chipGreenText : .primary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }

                    // ── Time of day chips ─────────────────────────────
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Time of day (optional)")
                            .font(.subheadline).fontWeight(.medium)

                        FlowLayout(spacing: 8) {
                            // "Any time" option
                            let anySelected = selectedTimeOfDay == nil
                            Button(action: { selectedTimeOfDay = nil }) {
                                Text("⏰ Any time")
                                    .font(.caption)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 6)
                                    .background(anySelected ? chipGreen : Color(.systemGray5))
                                    .foregroundColor(anySelected ? chipGreenText : .primary)
                                    .clipShape(Capsule())
                            }

                            ForEach(SuggestionTimeOfDay.allCases, id: \.rawValue) { tod in
                                let selected = selectedTimeOfDay == tod
                                Button(action: { selectedTimeOfDay = selected ? nil : tod }) {
                                    Text(tod.label)
                                        .font(.caption)
                                        .padding(.horizontal, 12)
                                        .padding(.vertical, 6)
                                        .background(selected ? chipGreen : Color(.systemGray5))
                                        .foregroundColor(selected ? chipGreenText : .primary)
                                        .clipShape(Capsule())
                                }
                            }
                        }
                    }
                }
                .padding(20)
            }
            .navigationTitle("Add Your Own")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel", action: onDismiss)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") { trySave() }
                        .fontWeight(.semibold)
                }
            }
        }
    }

    private func trySave() {
        textError     = validateText()
        categoryError = selectedCategory == nil ? "Pick a category." : nil
        guard textError == nil, categoryError == nil else { return }

        let newSuggestion = AnalogSuggestion(
            id: 0,
            text: text.trimmingCharacters(in: .whitespacesAndNewlines),
            category: selectedCategory!,
            tags: [],
            timeOfDay: selectedTimeOfDay,
            timesShown: 0,
            timesAccepted: 0,
            isCustom: true
        )
        onSave(newSuggestion)
    }

    private func validateText() -> String? {
        let trimmed = text.trimmingCharacters(in: .whitespacesAndNewlines)
        if trimmed.isEmpty { return "Please enter a suggestion." }
        if trimmed.count < 5 { return "Too short — be a bit more descriptive." }
        if trimmed.count > 200 { return "Keep it under 200 characters." }
        return nil
    }
}

// MARK: - FlowLayout helper

/// Simple flow (wrap) layout for chips.
private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let containerWidth = proposal.width ?? 300
        var rows: [[LayoutSubview]] = [[]]
        var rowWidth: CGFloat = 0

        for subview in subviews {
            let width = subview.sizeThatFits(.unspecified).width + spacing
            if rowWidth + width > containerWidth && !rows[rows.count - 1].isEmpty {
                rows.append([subview])
                rowWidth = width
            } else {
                rows[rows.count - 1].append(subview)
                rowWidth += width
            }
        }

        let height = rows.reduce(0.0) { acc, row in
            acc + (row.map { $0.sizeThatFits(.unspecified).height }.max() ?? 0) + spacing
        }
        return CGSize(width: containerWidth, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var rows: [[LayoutSubview]] = [[]]
        var rowWidth: CGFloat = 0

        for subview in subviews {
            let width = subview.sizeThatFits(.unspecified).width + spacing
            if rowWidth + width > bounds.width && !rows[rows.count - 1].isEmpty {
                rows.append([subview])
                rowWidth = width
            } else {
                rows[rows.count - 1].append(subview)
                rowWidth += width
            }
        }

        var y = bounds.minY
        for row in rows {
            var x = bounds.minX
            let rowHeight = row.map { $0.sizeThatFits(.unspecified).height }.max() ?? 0
            for subview in row {
                let size = subview.sizeThatFits(.unspecified)
                subview.place(at: CGPoint(x: x, y: y), proposal: .unspecified)
                x += size.width + spacing
            }
            y += rowHeight + spacing
        }
    }
}

// MARK: - Preview

#Preview {
    AnalogSuggestionsView(
        state: AnalogSuggestionsUiState(
            activeSuggestions: [
                AnalogSuggestion(
                    id: 1,
                    text: "Step outside for a 10-minute walk around the block.",
                    category: .exercise,
                    tags: ["outdoors", "quick"],
                    timeOfDay: .morning,
                    timesShown: 3,
                    timesAccepted: 1,
                    isCustom: false
                ),
                AnalogSuggestion(
                    id: 2,
                    text: "Make a cup of tea and read for 15 minutes.",
                    category: .reading,
                    tags: ["calm", "cozy"],
                    timeOfDay: nil,
                    timesShown: 1,
                    timesAccepted: 0,
                    isCustom: false
                ),
            ],
            customSuggestions: [
                AnalogSuggestion(
                    id: 100,
                    text: "Water the plants and tidy the windowsill.",
                    category: .nature,
                    tags: [],
                    timeOfDay: nil,
                    timesShown: 0,
                    timesAccepted: 0,
                    isCustom: true
                )
            ]
        )
    )
}
