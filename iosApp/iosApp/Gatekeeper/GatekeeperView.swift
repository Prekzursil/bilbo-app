import SwiftUI

// MARK: - GatekeeperView

/// Overlay screen that prompts the user to declare their intention before
/// opening a tracked app.
///
/// Mirrors the Android `GatekeeperScreen` in layout and UX:
/// - App icon placeholder + name
/// - Optional free-text intention field (≤100 chars)
/// - Duration picker using capsule chips (5, 10, 15, 20, 30, 60 minutes)
/// - "Start" button → creates an `IntentDeclaration` and notifies the manager
/// - "Not now" text button → dismisses without recording
struct GatekeeperView: View {

    // ── Config ────────────────────────────────────────────────────────────────

    let appName: String
    let packageBundleId: String
    let onStart: (String, Int) -> Void   // intention, durationMinutes
    let onDismiss: () -> Void

    // ── State ─────────────────────────────────────────────────────────────────

    @State private var intention: String = ""
    @State private var selectedDuration: Int = 15
    @State private var isPresented: Bool = false

    private let durationOptions = [5, 10, 15, 20, 30, 60]

    // ── Palette ───────────────────────────────────────────────────────────────

    private let cardBackground  = Color(red: 0.10, green: 0.17, blue: 0.24)
    private let primaryTeal     = Color(red: 0.28, green: 0.72, blue: 0.63)
    private let surfaceColor    = Color(red: 0.14, green: 0.20, blue: 0.27)
    private let onSurface       = Color(red: 0.88, green: 0.93, blue: 0.96)
    private let subtleGray      = Color(red: 0.54, green: 0.69, blue: 0.77)
    private let scrim           = Color.black.opacity(0.55)

    // ── Body ──────────────────────────────────────────────────────────────────

    var body: some View {
        ZStack {
            scrim
                .ignoresSafeArea()
                .onTapGesture { /* prevent tap-through */ }

            VStack {
                Spacer()

                cardContent
                    .offset(y: isPresented ? 0 : UIScreen.main.bounds.height)
                    .animation(.spring(response: 0.42, dampingFraction: 0.82), value: isPresented)
            }
        }
        .onAppear {
            withAnimation { isPresented = true }
        }
    }

    // ── Card ──────────────────────────────────────────────────────────────────

    private var cardContent: some View {
        VStack(spacing: 0) {
            // Drag handle
            RoundedRectangle(cornerRadius: 2)
                .fill(subtleGray.opacity(0.5))
                .frame(width: 40, height: 4)
                .padding(.top, 16)

            ScrollView {
                VStack(alignment: .leading, spacing: 20) {

                    // App icon + name
                    appHeader

                    // Intention input
                    intentionField

                    // Duration picker
                    durationPicker

                    // Action buttons
                    actionButtons
                        .padding(.bottom, 8)
                }
                .padding(.horizontal, 24)
                .padding(.top, 20)
                .padding(.bottom, 12)
            }
        }
        .background(cardBackground)
        .cornerRadius(28, corners: [.topLeft, .topRight])
        .shadow(color: Color.black.opacity(0.4), radius: 20, x: 0, y: -4)
    }

    // ── App header ────────────────────────────────────────────────────────────

    private var appHeader: some View {
        HStack(spacing: 16) {
            // Icon placeholder: two-letter monogram in a circle
            ZStack {
                Circle()
                    .fill(primaryTeal.opacity(0.18))
                    .frame(width: 56, height: 56)

                Text(appInitials)
                    .font(.system(size: 18, weight: .bold, design: .rounded))
                    .foregroundColor(primaryTeal)
            }

            VStack(alignment: .leading, spacing: 2) {
                Text(appName)
                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                    .foregroundColor(onSurface)

                Text(packageBundleId)
                    .font(.system(size: 11, weight: .regular))
                    .foregroundColor(subtleGray)
                    .lineLimit(1)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // ── Intention text field ──────────────────────────────────────────────────

    private var intentionField: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("What's your intention?")
                .font(.system(size: 15, weight: .medium, design: .rounded))
                .foregroundColor(onSurface)

            ZStack(alignment: .topLeading) {
                RoundedRectangle(cornerRadius: 14)
                    .fill(surfaceColor)
                    .overlay(
                        RoundedRectangle(cornerRadius: 14)
                            .stroke(intention.isEmpty ? subtleGray.opacity(0.3) : primaryTeal, lineWidth: 1.5)
                    )

                if intention.isEmpty {
                    Text("e.g. check my messages (optional)")
                        .font(.system(size: 14))
                        .foregroundColor(subtleGray)
                        .padding(.horizontal, 14)
                        .padding(.top, 12)
                        .allowsHitTesting(false)
                }

                TextEditor(text: $intention)
                    .font(.system(size: 14))
                    .foregroundColor(onSurface)
                    .scrollContentBackground(.hidden)
                    .background(Color.clear)
                    .padding(.horizontal, 10)
                    .padding(.vertical, 8)
                    .frame(minHeight: 68, maxHeight: 90)
                    .onChange(of: intention) { newValue in
                        if newValue.count > 100 {
                            intention = String(newValue.prefix(100))
                        }
                    }
            }

            HStack {
                Spacer()
                Text("\(intention.count)/100")
                    .font(.system(size: 11))
                    .foregroundColor(subtleGray)
            }
        }
    }

    // ── Duration picker ───────────────────────────────────────────────────────

    private var durationPicker: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("How long?")
                .font(.system(size: 15, weight: .medium, design: .rounded))
                .foregroundColor(onSurface)

            // Wrap chips in a flow-like layout using a lazy HGrid-style approach
            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 3),
                spacing: 8
            ) {
                ForEach(durationOptions, id: \.self) { minutes in
                    DurationChip(
                        label: minutes < 60 ? "\(minutes)m" : "1h",
                        isSelected: minutes == selectedDuration,
                        primaryColor: primaryTeal,
                        surfaceColor: surfaceColor,
                        subtleColor: subtleGray,
                        onTap: { selectedDuration = minutes }
                    )
                }
            }
        }
    }

    // ── Buttons ───────────────────────────────────────────────────────────────

    private var actionButtons: some View {
        VStack(spacing: 8) {
            Button {
                onStart(intention.trimmingCharacters(in: .whitespacesAndNewlines), selectedDuration)
            } label: {
                Text("Start \(selectedDuration) min")
                    .font(.system(size: 16, weight: .semibold, design: .rounded))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 52)
                    .background(primaryTeal)
                    .cornerRadius(14)
            }

            Button(action: onDismiss) {
                Text("Not now")
                    .font(.system(size: 15, weight: .regular, design: .rounded))
                    .foregroundColor(subtleGray)
                    .frame(maxWidth: .infinity)
                    .frame(height: 44)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private var appInitials: String {
        let words = appName.split(separator: " ")
        if words.count >= 2 {
            return String(words[0].prefix(1) + words[1].prefix(1)).uppercased()
        }
        return String(appName.prefix(2)).uppercased()
    }
}

// MARK: - DurationChip

private struct DurationChip: View {
    let label: String
    let isSelected: Bool
    let primaryColor: Color
    let surfaceColor: Color
    let subtleColor: Color
    let onTap: () -> Void

    var body: some View {
        Button(action: onTap) {
            Text(label)
                .font(.system(size: 13, weight: isSelected ? .bold : .regular, design: .rounded))
                .foregroundColor(isSelected ? .white : subtleColor)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 10)
                .background(isSelected ? primaryColor : surfaceColor)
                .cornerRadius(20)
                .overlay(
                    RoundedRectangle(cornerRadius: 20)
                        .stroke(isSelected ? primaryColor : subtleColor.opacity(0.3), lineWidth: 1.5)
                )
        }
    }
}

// MARK: - RoundedCorner helper

private struct RoundedCorner: Shape {
    var radius: CGFloat
    var corners: UIRectCorner

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

private extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

// MARK: - Preview

#Preview {
    GatekeeperView(
        appName: "Instagram",
        packageBundleId: "com.burbn.instagram",
        onStart: { _, _ in },
        onDismiss: {}
    )
    .background(Color.black.opacity(0.4))
}
