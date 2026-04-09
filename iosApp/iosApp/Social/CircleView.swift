import SwiftUI

// MARK: - Models

struct CircleDetailData: Identifiable {
    let id: String
    let name: String
    let goal: String
    let daysRemaining: Int
    let inviteCode: String
    var members: [CircleMemberData]
    let aggregateProgressPercent: Int
    let isAdmin: Bool
}

struct CircleMemberData: Identifiable {
    let id: String
    let displayName: String
    let isCurrentUser: Bool
    let fpBalance: Int?
    let streakDays: Int?
    let nutritiveMinutes: Int?
}

struct CircleListData: Identifiable {
    let id: String
    let name: String
    let memberCount: Int
    let goalSummary: String
    let daysRemaining: Int?
    let inviteCode: String
}

// MARK: - CircleView

/// SwiftUI circle management view — mirrors the Android CircleScreen.
/// Handles list, detail, create, and join flows.
struct CircleView: View {

    var circles: [CircleListData] = []
    var selectedCircle: CircleDetailData? = nil

    var onCreateCircle: (String, String, Int, Int) -> Void = { _, _, _, _ in }
    var onJoinCircle: (String) -> Void = { _ in }
    var onLeaveCircle: (String) -> Void = { _ in }
    var onCircleTap: (String) -> Void = { _ in }
    var onBack: (() -> Void)? = nil

    @State private var showCreateSheet = false
    @State private var showJoinSheet = false
    @State private var showLeaveAlert = false

    // MARK: Body

    var body: some View {
        NavigationStack {
            List {
                // Actions
                Section {
                    HStack(spacing: 12) {
                        Button { showCreateSheet = true } label: {
                            Label("Create", systemImage: "plus.circle")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)

                        Button { showJoinSheet = true } label: {
                            Label("Join", systemImage: "link")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.bordered)
                    }
                    .listRowInsets(.init())
                    .listRowBackground(Color.clear)
                }

                if circles.isEmpty {
                    Section {
                        VStack(spacing: 12) {
                            Image(systemName: "circle.dotted")
                                .font(.system(size: 36))
                                .foregroundStyle(.secondary.opacity(0.4))
                            Text("No circles yet. Create one or join with an invite code!")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 24)
                        .listRowBackground(Color.clear)
                    }
                } else {
                    Section("Your Circles") {
                        ForEach(circles) { circle in
                            NavigationLink {
                                if let detail = selectedCircle, detail.id == circle.id {
                                    CircleDetailView(
                                        circle: detail,
                                        onLeave: { onLeaveCircle(circle.id) }
                                    )
                                } else {
                                    ProgressView("Loading…")
                                        .onAppear { onCircleTap(circle.id) }
                                }
                            } label: {
                                CircleListRow(circle: circle)
                            }
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Focus Circles")
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
            .sheet(isPresented: $showCreateSheet) {
                CreateCircleSheet(onCreate: { name, goal, duration, maxMembers in
                    onCreateCircle(name, goal, duration, maxMembers)
                    showCreateSheet = false
                })
            }
            .sheet(isPresented: $showJoinSheet) {
                JoinCircleSheet(onJoin: { code in
                    onJoinCircle(code)
                    showJoinSheet = false
                })
            }
        }
    }
}

// MARK: - Circle list row

private struct CircleListRow: View {
    let circle: CircleListData

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.blue.opacity(0.12))
                .frame(width: 44, height: 44)
                .overlay(
                    Image(systemName: "person.3.fill")
                        .font(.system(size: 16))
                        .foregroundStyle(.blue)
                )

            VStack(alignment: .leading, spacing: 3) {
                Text(circle.name)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                Text("\(circle.memberCount) members · \(circle.goalSummary)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                if let days = circle.daysRemaining {
                    Text("\(days) days remaining")
                        .font(.caption2)
                        .foregroundStyle(.orange)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Circle detail view

private struct CircleDetailView: View {
    let circle: CircleDetailData
    var onLeave: () -> Void = {}

    @State private var showLeaveAlert = false
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        List {
            // Header card
            Section {
                VStack(alignment: .leading, spacing: 10) {
                    Text(circle.name)
                        .font(.title2)
                        .fontWeight(.bold)
                    if !circle.goal.isEmpty {
                        Label(circle.goal, systemImage: "target")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                    HStack {
                        Label("\(circle.daysRemaining) days remaining", systemImage: "calendar")
                            .font(.caption)
                            .foregroundStyle(.orange)
                        Spacer()
                    }
                }
                .padding(.vertical, 4)
                .listRowBackground(Color.orange.opacity(0.06))
            }

            // Group progress
            Section("Group Progress") {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text("\(circle.aggregateProgressPercent)%")
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundStyle(.orange)
                        Spacer()
                    }
                    ProgressView(value: Double(circle.aggregateProgressPercent) / 100.0)
                        .tint(.orange)
                        .scaleEffect(x: 1, y: 1.5, anchor: .center)
                }
                .padding(.vertical, 4)
            }

            // Invite code
            Section("Invite Code") {
                HStack {
                    Text(circle.inviteCode)
                        .font(.system(.body, design: .monospaced))
                        .fontWeight(.bold)
                        .tracking(2)
                    Spacer()
                    ShareLink(item: "Join my Spark circle '\(circle.name)' using code \(circle.inviteCode)") {
                        Image(systemName: "square.and.arrow.up")
                            .foregroundStyle(.orange)
                    }
                }
            }

            // Members
            Section("Members (\(circle.members.count))") {
                ForEach(circle.members) { member in
                    MemberRow(member: member)
                }
            }

            // Leave circle
            if !circle.isAdmin {
                Section {
                    Button(role: .destructive) {
                        showLeaveAlert = true
                    } label: {
                        Label("Leave Circle", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .navigationTitle(circle.name)
        .navigationBarTitleDisplayMode(.inline)
        .alert("Leave Circle?", isPresented: $showLeaveAlert) {
            Button("Leave", role: .destructive) {
                onLeave()
                dismiss()
            }
            Button("Cancel", role: .cancel) {}
        } message: {
            Text("You'll need an invite code to rejoin.")
        }
    }
}

// MARK: - Member row

private struct MemberRow: View {
    let member: CircleMemberData

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(member.isCurrentUser ? Color.orange.opacity(0.15) : Color.blue.opacity(0.12))
                .frame(width: 38, height: 38)
                .overlay(
                    Text(String(member.displayName.prefix(1)).uppercased())
                        .font(.subheadline)
                        .fontWeight(.bold)
                        .foregroundStyle(member.isCurrentUser ? .orange : .blue)
                )

            VStack(alignment: .leading, spacing: 3) {
                HStack(spacing: 6) {
                    Text(member.displayName)
                        .font(.subheadline)
                        .fontWeight(member.isCurrentUser ? .bold : .regular)
                    if member.isCurrentUser {
                        Text("You")
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundStyle(.orange)
                            .padding(.horizontal, 5)
                            .padding(.vertical, 2)
                            .background(Color.orange.opacity(0.12), in: Capsule())
                    }
                }

                let stats: [String] = {
                    var parts: [String] = []
                    if let fp = member.fpBalance { parts.append("\(fp) FP") }
                    if let streak = member.streakDays { parts.append("\(streak)d streak") }
                    if let nutritive = member.nutritiveMinutes { parts.append("\(nutritive)m nutritive") }
                    return parts
                }()

                if !stats.isEmpty {
                    Text(stats.joined(separator: " · "))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()
        }
        .padding(.vertical, 2)
    }
}

// MARK: - Create circle sheet

private struct CreateCircleSheet: View {
    var onCreate: (String, String, Int, Int) -> Void = { _, _, _, _ in }
    @Environment(\.dismiss) private var dismiss

    @State private var name = ""
    @State private var goal = ""
    @State private var duration = 14
    @State private var maxMembers = 5

    private let durationOptions = [7, 14, 30]
    private let maxMemberOptions = [3, 4, 5, 6, 7]

    var body: some View {
        NavigationStack {
            Form {
                Section("Circle Details") {
                    TextField("Circle name", text: $name)
                    TextField("Goal (optional)", text: $goal)
                }

                Section("Duration") {
                    Picker("Duration", selection: $duration) {
                        ForEach(durationOptions, id: \.self) { days in
                            Text("\(days) days").tag(days)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("Max Members") {
                    Picker("Max Members", selection: $maxMembers) {
                        ForEach(maxMemberOptions, id: \.self) { count in
                            Text("\(count)").tag(count)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section {
                    Button("Create Circle") {
                        if !name.trimmingCharacters(in: .whitespaces).isEmpty {
                            onCreate(name, goal, duration, maxMembers)
                        }
                    }
                    .frame(maxWidth: .infinity)
                    .disabled(name.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
            .navigationTitle("Create Circle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Join circle sheet

private struct JoinCircleSheet: View {
    var onJoin: (String) -> Void = { _ in }
    @Environment(\.dismiss) private var dismiss

    @State private var code = ""

    var body: some View {
        NavigationStack {
            VStack(spacing: 24) {
                Image(systemName: "link.circle.fill")
                    .font(.system(size: 56))
                    .foregroundStyle(.orange)

                Text("Enter Invite Code")
                    .font(.title2)
                    .fontWeight(.bold)

                Text("Ask the circle admin for their 8-character invite code.")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
                    .multilineTextAlignment(.center)

                TextField("XXXXXXXX", text: $code)
                    .font(.system(size: 28, design: .monospaced))
                    .multilineTextAlignment(.center)
                    .tracking(4)
                    .textCase(.uppercase)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.characters)
                    .onChange(of: code) { code = String($0.prefix(8)).uppercased() }
                    .padding()
                    .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
                    .padding(.horizontal)

                Button("Join Circle") {
                    if code.count == 8 { onJoin(code) }
                }
                .buttonStyle(.borderedProminent)
                .disabled(code.count != 8)

                Spacer()
            }
            .padding(.top, 32)
            .navigationTitle("Join Circle")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Preview

#Preview {
    CircleView(
        circles: [
            CircleListData(id: "c1", name: "Morning Focus", memberCount: 4, goalSummary: "Under 2h daily", daysRemaining: 12, inviteCode: "ABCD1234"),
            CircleListData(id: "c2", name: "Weekend Warriors", memberCount: 6, goalSummary: "No scrolling after 9PM", daysRemaining: 5, inviteCode: "XY789ZWQ"),
        ],
        selectedCircle: CircleDetailData(
            id: "c1",
            name: "Morning Focus",
            goal: "Under 2h screen time daily",
            daysRemaining: 12,
            inviteCode: "ABCD1234",
            members: [
                CircleMemberData(id: "u1", displayName: "You", isCurrentUser: true, fpBalance: 312, streakDays: 5, nutritiveMinutes: 87),
                CircleMemberData(id: "u2", displayName: "Alex", isCurrentUser: false, fpBalance: 245, streakDays: 3, nutritiveMinutes: nil),
                CircleMemberData(id: "u3", displayName: "Sam", isCurrentUser: false, fpBalance: nil, streakDays: 7, nutritiveMinutes: nil),
            ],
            aggregateProgressPercent: 68,
            isAdmin: false
        )
    )
}
