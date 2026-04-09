import SwiftUI

// MARK: - Models

struct BuddyPairDetailData: Identifiable {
    let id: String
    let buddyDisplayName: String
    var sharingLevel: SharingLevel
    let statusSummary: String
    let isOnline: Bool
}

struct NudgeData: Identifiable {
    let id: String
    let fromName: String
    let message: String
    let timeAgo: String
    let isUnread: Bool
}

// MARK: - BuddyPairView

/// SwiftUI buddy management screen — mirrors the Android BuddyPairScreen.
/// Features:
///  - List of buddy pairs (max 3) with sharing-level selector
///  - Invite a Buddy → generates 6-char code + share sheet
///  - Enter Code → input dialog
///  - Nudge inbox
///  - Send encouragement (100-char)
struct BuddyPairView: View {

    var pairs: [BuddyPairDetailData] = []
    var nudges: [NudgeData] = []
    var generatedInviteCode: String? = nil
    var isGeneratingCode: Bool = false

    var onInviteBuddy: () -> Void = {}
    var onEnterCode: (String) -> Void = { _ in }
    var onSharingLevelChange: (String, SharingLevel) -> Void = { _, _ in }
    var onRemovePair: (String) -> Void = { _ in }
    var onSendEncouragement: (String, String) -> Void = { _, _ in }
    var onDismissNudge: (String) -> Void = { _ in }
    var onBack: (() -> Void)? = nil

    @State private var showEnterCodeSheet = false
    @State private var showInviteSheet = false
    @State private var codeInput = ""
    @State private var encouragementTarget: String? = nil
    @State private var encouragementText = ""

    private let maxPairs = 3

    // MARK: Body

    var body: some View {
        NavigationStack {
            List {
                // Action buttons
                Section {
                    HStack(spacing: 12) {
                        Button {
                            onInviteBuddy()
                            if generatedInviteCode != nil { showInviteSheet = true }
                        } label: {
                            if isGeneratingCode {
                                ProgressView()
                                    .frame(maxWidth: .infinity)
                            } else {
                                Label("Invite a Buddy", systemImage: "person.badge.plus")
                                    .frame(maxWidth: .infinity)
                            }
                        }
                        .buttonStyle(.borderedProminent)
                        .disabled(pairs.count >= maxPairs || isGeneratingCode)

                        Button { showEnterCodeSheet = true } label: {
                            Label("Enter Code", systemImage: "qrcode")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    .listRowInsets(.init())
                    .listRowBackground(Color.clear)
                }

                // Max pairs notice
                if pairs.count >= maxPairs {
                    Section {
                        HStack(spacing: 8) {
                            Image(systemName: "info.circle.fill")
                                .foregroundStyle(.orange)
                            Text("Maximum \(maxPairs) buddy pairs reached.")
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        .listRowBackground(Color.orange.opacity(0.08))
                    }
                }

                // Buddy pairs
                if pairs.isEmpty {
                    Section {
                        VStack(spacing: 12) {
                            Image(systemName: "person.2")
                                .font(.system(size: 36))
                                .foregroundStyle(.secondary.opacity(0.4))
                            Text("Invite a friend to become accountability buddies.")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                        .listRowBackground(Color.clear)
                    }
                } else {
                    Section("Your Buddies") {
                        ForEach(pairs) { pair in
                            BuddyDetailCard(
                                pair: pair,
                                onSharingLevelChange: { level in onSharingLevelChange(pair.id, level) },
                                onRemove: { onRemovePair(pair.id) },
                                onSendEncouragement: {
                                    encouragementTarget = pair.id
                                    encouragementText = ""
                                }
                            )
                        }
                    }
                }

                // Nudge inbox
                if !nudges.isEmpty {
                    Section("Nudges") {
                        ForEach(nudges) { nudge in
                            NudgeRow(nudge: nudge, onDismiss: { onDismissNudge(nudge.id) })
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Accountability Buddies")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                if let back = onBack {
                    ToolbarItem(placement: .navigationBarLeading) {
                        Button(action: back) {
                            Image(systemName: "chevron.left")
                        }
                    }
                }
            }
            // Enter code sheet
            .sheet(isPresented: $showEnterCodeSheet) {
                EnterCodeSheet(code: $codeInput, onSubmit: { code in
                    onEnterCode(code)
                    showEnterCodeSheet = false
                    codeInput = ""
                })
            }
            // Invite code sheet
            .sheet(item: Binding(get: { generatedInviteCode.map { InviteCodeItem(code: $0) } }, set: { _ in })) { item in
                InviteCodeSheet(code: item.code, onDone: { showInviteSheet = false })
            }
            // Encouragement sheet
            .sheet(item: Binding(
                get: { encouragementTarget.map { EncouragementTarget(pairId: $0, name: pairs.first(where: { $0.id == $0.id })?.buddyDisplayName ?? "") } },
                set: { if $0 == nil { encouragementTarget = nil } }
            )) { target in
                EncouragementSheet(
                    toName: target.name,
                    text: $encouragementText,
                    onSend: {
                        onSendEncouragement(target.pairId, encouragementText)
                        encouragementTarget = nil
                    },
                    onCancel: { encouragementTarget = nil }
                )
            }
        }
    }

    // Identifiable wrappers for .sheet(item:)
    private struct InviteCodeItem: Identifiable { let id = UUID(); let code: String }
    private struct EncouragementTarget: Identifiable { let id = UUID(); let pairId: String; let name: String }
}

// MARK: - Buddy detail card

private struct BuddyDetailCard: View {
    let pair: BuddyPairDetailData
    var onSharingLevelChange: (SharingLevel) -> Void = { _ in }
    var onRemove: () -> Void = {}
    var onSendEncouragement: () -> Void = {}

    @State private var showLevelPicker = false
    @State private var showRemoveAlert = false

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header row
            HStack(spacing: 12) {
                ZStack(alignment: .bottomTrailing) {
                    Circle()
                        .fill(Color.orange.opacity(0.15))
                        .frame(width: 48, height: 48)
                        .overlay(
                            Text(String(pair.buddyDisplayName.prefix(1)).uppercased())
                                .font(.title3)
                                .fontWeight(.bold)
                                .foregroundStyle(.orange)
                        )
                    if pair.isOnline {
                        Circle()
                            .fill(Color.green)
                            .frame(width: 12, height: 12)
                            .overlay(Circle().stroke(Color(.systemBackground), lineWidth: 2))
                    }
                }

                VStack(alignment: .leading, spacing: 3) {
                    Text(pair.buddyDisplayName)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text(pair.statusSummary)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                Menu {
                    Button(role: .destructive) { showRemoveAlert = true } label: {
                        Label("Remove buddy", systemImage: "person.badge.minus")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                        .foregroundStyle(.secondary)
                }
            }

            // Sharing level
            HStack {
                Text("Sharing:")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Button {
                    showLevelPicker = true
                } label: {
                    HStack(spacing: 4) {
                        Text(pair.sharingLevel.displayName)
                            .font(.caption)
                            .fontWeight(.semibold)
                        Image(systemName: "chevron.down")
                            .font(.caption2)
                    }
                    .foregroundStyle(.orange)
                }
                Spacer()
            }

            // Send encouragement button
            Button(action: onSendEncouragement) {
                Label("Send encouragement", systemImage: "heart")
                    .font(.callout)
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(.pink)
        }
        .padding(.vertical, 4)
        // Sharing level picker
        .sheet(isPresented: $showLevelPicker) {
            SharingLevelPickerSheet(
                currentLevel: pair.sharingLevel,
                onSelect: { level in
                    onSharingLevelChange(level)
                    showLevelPicker = false
                }
            )
            .presentationDetents([.medium])
        }
        // Remove confirmation
        .alert("Remove Buddy?", isPresented: $showRemoveAlert) {
            Button("Remove", role: .destructive, action: onRemove)
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("Are you sure you want to remove \(pair.buddyDisplayName) as a buddy?")
        }
    }
}

// MARK: - Nudge row

private struct NudgeRow: View {
    let nudge: NudgeData
    var onDismiss: () -> Void = {}

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            if nudge.isUnread {
                Circle()
                    .fill(Color.orange)
                    .frame(width: 8, height: 8)
                    .padding(.top, 6)
            }

            VStack(alignment: .leading, spacing: 4) {
                Text(nudge.fromName)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundStyle(.orange)
                Text(nudge.message)
                    .font(.subheadline)
                Text(nudge.timeAgo)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            Button(action: onDismiss) {
                Image(systemName: "xmark")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .buttonStyle(.plain)
        }
        .listRowBackground(nudge.isUnread ? Color.orange.opacity(0.08) : nil)
    }
}

// MARK: - Sheets

private struct EnterCodeSheet: View {
    @Binding var code: String
    var onSubmit: (String) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("Ask your buddy for their 6-character code.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                TextField("XXXXXX", text: $code)
                    .font(.system(size: 28, design: .monospaced))
                    .multilineTextAlignment(.center)
                    .tracking(6)
                    .textCase(.uppercase)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.characters)
                    .onChange(of: code) { newValue in
                        code = String(newValue.prefix(6)).uppercased()
                    }
                    .padding()
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))

                Button("Join") {
                    onSubmit(code)
                }
                .buttonStyle(.borderedProminent)
                .disabled(code.count != 6)

                Spacer()
            }
            .padding(24)
            .navigationTitle("Enter Invite Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

private struct InviteCodeSheet: View {
    let code: String
    var onDone: () -> Void = {}
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Text("Share this code with your buddy. It expires in 48 hours.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                Text(code)
                    .font(.system(size: 40, weight: .bold, design: .monospaced))
                    .tracking(8)
                    .foregroundStyle(.orange)
                    .padding(20)
                    .frame(maxWidth: .infinity)
                    .background(Color.orange.opacity(0.1), in: RoundedRectangle(cornerRadius: 16))

                ShareLink(item: "Join me on Spark! Use code \(code) to connect as accountability buddies.") {
                    Label("Share Code", systemImage: "square.and.arrow.up")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)

                Spacer()
            }
            .padding(24)
            .navigationTitle("Your Invite Code")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss(); onDone() }
                }
            }
        }
    }
}

private struct SharingLevelPickerSheet: View {
    let currentLevel: SharingLevel
    var onSelect: (SharingLevel) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            List {
                ForEach(SharingLevel.allCases, id: \.self) { level in
                    Button {
                        onSelect(level)
                    } label: {
                        HStack {
                            VStack(alignment: .leading, spacing: 3) {
                                Text(level.displayName)
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundStyle(.primary)
                                Text(level.description)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            if level == currentLevel {
                                Image(systemName: "checkmark")
                                    .foregroundStyle(.orange)
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Sharing Level")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

private struct EncouragementSheet: View {
    let toName: String
    @Binding var text: String
    var onSend: () -> Void = {}
    var onCancel: () -> Void = {}
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 16) {
                if !toName.isEmpty {
                    Text("To: \(toName)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                TextEditor(text: $text)
                    .frame(height: 120)
                    .padding(8)
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
                    .overlay(alignment: .bottomTrailing) {
                        Text("\(text.count)/100")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .padding(6)
                    }
                    .onChange(of: text) { if text.count > 100 { text = String(text.prefix(100)) } }

                Button("Send", action: { onSend(); dismiss() })
                    .buttonStyle(.borderedProminent)
                    .disabled(text.trimmingCharacters(in: .whitespaces).isEmpty)
                    .frame(maxWidth: .infinity)

                Spacer()
            }
            .padding(20)
            .navigationTitle("Send Encouragement")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onCancel(); dismiss() }
                }
            }
        }
    }
}

// MARK: - Preview

#Preview {
    BuddyPairView(
        pairs: [
            BuddyPairDetailData(id: "1", buddyDisplayName: "Alex", sharingLevel: .standard, statusSummary: "312 FP · 5-day streak", isOnline: true),
            BuddyPairDetailData(id: "2", buddyDisplayName: "Sam", sharingLevel: .basic, statusSummary: "128 FP", isOnline: false),
        ],
        nudges: [
            NudgeData(id: "n1", fromName: "Alex", message: "Keep going! You're doing great 💪", timeAgo: "2 hours ago", isUnread: true),
        ],
        generatedInviteCode: nil
    )
}
