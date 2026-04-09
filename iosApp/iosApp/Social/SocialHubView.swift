import SwiftUI

// MARK: - Domain models

enum SharingLevel: Int, CaseIterable {
    case minimal  = 0
    case basic    = 1
    case standard = 2
    case detailed = 3

    var displayName: String {
        switch self {
        case .minimal:  return "Minimal"
        case .basic:    return "Basic"
        case .standard: return "Standard"
        case .detailed: return "Detailed"
        }
    }

    var description: String {
        switch self {
        case .minimal:  return "Presence only"
        case .basic:    return "FP balance + streak"
        case .standard: return "Daily FP summary"
        case .detailed: return "Full breakdown"
        }
    }
}

struct BuddyPairSocialItem: Identifiable {
    let id: String
    let buddyDisplayName: String
    let sharingLevel: SharingLevel
    let statusSummary: String
    let isOnline: Bool
}

struct CircleSocialItem: Identifiable {
    let id: String
    let name: String
    let memberCount: Int
    let goalSummary: String
    let daysRemaining: Int?
}

struct ChallengeSocialItem: Identifiable {
    let id: String
    let title: String
    let typeLabel: String
    let isTeam: Bool
    let progressPercent: Int
    let daysRemaining: Int
}

// MARK: - SocialHubView

/// SwiftUI TabView with Buddies, Circles, Challenges sections.
/// Mirrors the Android SocialHubScreen.
struct SocialHubView: View {

    var buddyPairs: [BuddyPairSocialItem] = []
    var circles: [CircleSocialItem] = []
    var challenges: [ChallengeSocialItem] = []

    var onInviteBuddy: () -> Void = {}
    var onEnterBuddyCode: () -> Void = {}
    var onBuddyPairTap: (String) -> Void = { _ in }
    var onCreateCircle: () -> Void = {}
    var onJoinCircle: () -> Void = {}
    var onCircleTap: (String) -> Void = { _ in }
    var onCreateChallenge: () -> Void = {}
    var onChallengeTap: (String) -> Void = { _ in }
    var onBack: (() -> Void)? = nil

    @State private var selectedTab: Int = 0

    // MARK: Body

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Custom tab bar
                HStack {
                    ForEach(Array(tabs.enumerated()), id: \.offset) { idx, tab in
                        Button {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                selectedTab = idx
                            }
                        } label: {
                            VStack(spacing: 4) {
                                Image(systemName: selectedTab == idx ? tab.filledIcon : tab.icon)
                                    .font(.system(size: 18))
                                Text(tab.label)
                                    .font(.caption2)
                                    .fontWeight(selectedTab == idx ? .semibold : .regular)
                            }
                            .foregroundStyle(selectedTab == idx ? Color.orange : Color.secondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                        }
                    }
                }
                .background(Color(.systemBackground))
                .overlay(Divider(), alignment: .bottom)

                // Content
                TabView(selection: $selectedTab) {
                    BuddiesTabContent(
                        pairs: buddyPairs,
                        onInvite: onInviteBuddy,
                        onEnterCode: onEnterBuddyCode,
                        onPairTap: onBuddyPairTap
                    )
                    .tag(0)

                    CirclesTabContent(
                        circles: circles,
                        onCreate: onCreateCircle,
                        onJoin: onJoinCircle,
                        onCircleTap: onCircleTap
                    )
                    .tag(1)

                    ChallengesTabContent(
                        challenges: challenges,
                        onCreate: onCreateChallenge,
                        onChallengeTap: onChallengeTap
                    )
                    .tag(2)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
            }
            .navigationTitle("Social")
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
        }
    }

    private struct TabInfo {
        let label: String
        let icon: String
        let filledIcon: String
    }

    private let tabs: [TabInfo] = [
        TabInfo(label: "Buddies",    icon: "person.2",       filledIcon: "person.2.fill"),
        TabInfo(label: "Circles",    icon: "circle.dotted",  filledIcon: "circle.fill"),
        TabInfo(label: "Challenges", icon: "trophy",         filledIcon: "trophy.fill"),
    ]
}

// MARK: - Buddies Tab

private struct BuddiesTabContent: View {
    let pairs: [BuddyPairSocialItem]
    var onInvite: () -> Void = {}
    var onEnterCode: () -> Void = {}
    var onPairTap: (String) -> Void = { _ in }

    private let maxPairs = 3

    var body: some View {
        List {
            Section {
                HStack(spacing: 12) {
                    Button(action: onInvite) {
                        Label("Invite Buddy", systemImage: "person.badge.plus")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(pairs.count >= maxPairs)

                    Button(action: onEnterCode) {
                        Label("Enter Code", systemImage: "qrcode")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                .listRowInsets(.init())
                .listRowBackground(Color.clear)
                .padding(.vertical, 4)
            }

            if pairs.isEmpty {
                Section {
                    SocialEmptyStateView(icon: "person.2", message: "No buddies yet. Invite a friend to get started!")
                }
            } else {
                Section("Your Buddies") {
                    ForEach(pairs) { pair in
                        Button { onPairTap(pair.id) } label: {
                            BuddyRowView(pair: pair)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

private struct BuddyRowView: View {
    let pair: BuddyPairSocialItem

    var body: some View {
        HStack(spacing: 12) {
            ZStack(alignment: .bottomTrailing) {
                Circle()
                    .fill(Color.orange.opacity(0.15))
                    .frame(width: 44, height: 44)
                    .overlay(
                        Text(String(pair.buddyDisplayName.prefix(1)).uppercased())
                            .font(.headline)
                            .fontWeight(.bold)
                            .foregroundStyle(.orange)
                    )
                if pair.isOnline {
                    Circle()
                        .fill(Color.green)
                        .frame(width: 11, height: 11)
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

            Text(pair.sharingLevel.displayName)
                .font(.caption2)
                .fontWeight(.semibold)
                .foregroundStyle(.orange)
                .padding(.horizontal, 8)
                .padding(.vertical, 3)
                .background(Color.orange.opacity(0.12), in: Capsule())

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Circles Tab

private struct CirclesTabContent: View {
    let circles: [CircleSocialItem]
    var onCreate: () -> Void = {}
    var onJoin: () -> Void = {}
    var onCircleTap: (String) -> Void = { _ in }

    var body: some View {
        List {
            Section {
                HStack(spacing: 12) {
                    Button(action: onCreate) {
                        Label("Create", systemImage: "plus.circle")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)

                    Button(action: onJoin) {
                        Label("Join", systemImage: "link")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                }
                .listRowInsets(.init())
                .listRowBackground(Color.clear)
                .padding(.vertical, 4)
            }

            if circles.isEmpty {
                Section {
                    SocialEmptyStateView(icon: "circle.dotted", message: "No circles yet. Create one or join with an invite code!")
                }
            } else {
                Section("Your Circles") {
                    ForEach(circles) { circle in
                        Button { onCircleTap(circle.id) } label: {
                            CircleRowView(circle: circle)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

private struct CircleRowView: View {
    let circle: CircleSocialItem

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 10)
                .fill(Color.blue.opacity(0.12))
                .frame(width: 44, height: 44)
                .overlay(
                    Image(systemName: "circle.fill")
                        .font(.system(size: 18))
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

            Spacer()

            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Challenges Tab

private struct ChallengesTabContent: View {
    let challenges: [ChallengeSocialItem]
    var onCreate: () -> Void = {}
    var onChallengeTap: (String) -> Void = { _ in }

    var body: some View {
        List {
            Section {
                Button(action: onCreate) {
                    Label("Create Challenge", systemImage: "plus.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .listRowInsets(.init())
                .listRowBackground(Color.clear)
                .padding(.vertical, 4)
            }

            if challenges.isEmpty {
                Section {
                    SocialEmptyStateView(icon: "trophy", message: "No active challenges. Create one to stay motivated!")
                }
            } else {
                Section("Active Challenges") {
                    ForEach(challenges) { challenge in
                        Button { onChallengeTap(challenge.id) } label: {
                            ChallengeRowView(challenge: challenge)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
    }
}

private struct ChallengeRowView: View {
    let challenge: ChallengeSocialItem

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                VStack(alignment: .leading, spacing: 3) {
                    Text(challenge.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    HStack(spacing: 6) {
                        Text(challenge.typeLabel)
                            .font(.caption2)
                            .fontWeight(.semibold)
                            .foregroundStyle(.orange)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(Color.orange.opacity(0.12), in: Capsule())

                        Text(challenge.isTeam ? "Team" : "Solo")
                            .font(.caption2)
                            .foregroundStyle(.secondary)

                        Text("·")
                            .font(.caption2)
                            .foregroundStyle(.secondary)

                        Text("\(challenge.daysRemaining)d left")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            ProgressView(value: Double(challenge.progressPercent) / 100.0)
                .tint(.orange)

            Text("\(challenge.progressPercent)% complete")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Shared empty state

private struct SocialEmptyStateView: View {
    let icon: String
    let message: String

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 40))
                .foregroundStyle(.secondary.opacity(0.4))
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 32)
        .listRowBackground(Color.clear)
    }
}

// MARK: - Preview

#Preview {
    SocialHubView(
        buddyPairs: [
            BuddyPairSocialItem(id: "1", buddyDisplayName: "Alex", sharingLevel: .standard, statusSummary: "312 FP · 5-day streak", isOnline: true),
            BuddyPairSocialItem(id: "2", buddyDisplayName: "Sam", sharingLevel: .basic, statusSummary: "128 FP", isOnline: false),
        ],
        circles: [
            CircleSocialItem(id: "c1", name: "Morning Focus", memberCount: 4, goalSummary: "Under 2h daily", daysRemaining: 12),
        ],
        challenges: [
            ChallengeSocialItem(id: "ch1", title: "Nutritive Sprint", typeLabel: "Earn Nutritive", isTeam: true, progressPercent: 67, daysRemaining: 4),
        ]
    )
}
