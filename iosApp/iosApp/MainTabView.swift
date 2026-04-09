// MainTabView.swift
// Spark — iOS Root Navigation
//
// Root SwiftUI TabView with 5 tabs:
//   1. Dashboard   — today's overview
//   2. Focus       — Intent Gatekeeper settings
//   3. Insights    — weekly analytics
//   4. Social      — buddies, circles, challenges
//   5. Settings    — full app configuration

import SwiftUI

// MARK: - Tab identifiers

enum SparkTab: Int, CaseIterable {
    case dashboard
    case focus
    case insights
    case social
    case settings

    var title: String {
        switch self {
        case .dashboard: return "Dashboard"
        case .focus:     return "Focus"
        case .insights:  return "Insights"
        case .social:    return "Social"
        case .settings:  return "Settings"
        }
    }

    var icon: String {
        switch self {
        case .dashboard: return "square.grid.2x2.fill"
        case .focus:     return "shield.fill"
        case .insights:  return "chart.bar.fill"
        case .social:    return "person.2.fill"
        case .settings:  return "gearshape.fill"
        }
    }
}

// MARK: - Root view

struct MainTabView: View {

    @StateObject private var familyControls = FamilyControlsManager.shared
    @State private var selectedTab: SparkTab = .dashboard

    var body: some View {
        TabView(selection: $selectedTab) {
            dashboardTab
            focusTab
            insightsTab
            socialTab
            settingsTab
        }
        .tint(Color("SparkTeal"))
        .task {
            await familyControls.requestAuthorizationIfNeeded()
        }
    }

    // MARK: - Tab Views

    private var dashboardTab: some View {
        NavigationStack {
            DashboardView()
                .navigationTitle("Dashboard")
                .navigationBarTitleDisplayMode(.large)
        }
        .tabItem { Label(SparkTab.dashboard.title, systemImage: SparkTab.dashboard.icon) }
        .tag(SparkTab.dashboard)
    }

    private var focusTab: some View {
        NavigationStack {
            FocusView()
                .navigationTitle("Focus")
                .navigationBarTitleDisplayMode(.large)
        }
        .tabItem { Label(SparkTab.focus.title, systemImage: SparkTab.focus.icon) }
        .tag(SparkTab.focus)
    }

    private var insightsTab: some View {
        NavigationStack {
            InsightsView()
                .navigationTitle("Insights")
                .navigationBarTitleDisplayMode(.large)
        }
        .tabItem { Label(SparkTab.insights.title, systemImage: SparkTab.insights.icon) }
        .tag(SparkTab.insights)
    }

    private var socialTab: some View {
        NavigationStack {
            SocialHubView()
                .navigationTitle("Social")
                .navigationBarTitleDisplayMode(.large)
        }
        .tabItem { Label(SparkTab.social.title, systemImage: SparkTab.social.icon) }
        .tag(SparkTab.social)
    }

    private var settingsTab: some View {
        NavigationStack {
            SettingsView()
                .navigationTitle("Settings")
                .navigationBarTitleDisplayMode(.large)
        }
        .tabItem { Label(SparkTab.settings.title, systemImage: SparkTab.settings.icon) }
        .tag(SparkTab.settings)
    }
}

// MARK: - Placeholder stub views (implemented in their own files)

/// Gatekeeper/enforcement configuration
struct FocusView: View {
    var body: some View {
        List {
            Section("Enforcement") {
                Text("Configure per-app blocking modes here.")
                    .foregroundStyle(.secondary)
            }
            Section("Family Controls") {
                FamilyControlsStatusRow()
            }
        }
    }
}

struct FamilyControlsStatusRow: View {
    @ObservedObject private var fcManager = FamilyControlsManager.shared

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text("Screen Time Access")
                    .font(.body)
                Text(statusLabel)
                    .font(.caption)
                    .foregroundStyle(statusColor)
            }
            Spacer()
            if fcManager.authState != .authorized {
                Button("Enable") {
                    Task { await fcManager.requestAuthorization() }
                }
                .buttonStyle(.borderedProminent)
                .controlSize(.small)
            } else {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
            }
        }
    }

    private var statusLabel: String {
        switch fcManager.authState {
        case .authorized:     return "Authorized — enforcement active"
        case .denied:         return "Denied — tracking only mode"
        case .notDetermined:  return "Not yet requested"
        }
    }

    private var statusColor: Color {
        switch fcManager.authState {
        case .authorized:    return .green
        case .denied:        return .orange
        case .notDetermined: return .secondary
        }
    }
}

/// Weekly analytics placeholder
struct InsightsView: View {
    var body: some View {
        Text("Insights coming soon")
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

/// Social hub placeholder
struct SocialHubView: View {
    var body: some View {
        Text("Social hub coming soon")
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}

// MARK: - Preview

#Preview {
    MainTabView()
}
