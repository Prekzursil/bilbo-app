// SettingsView.swift
// Bilbo — iOS Settings
//
// Full settings screen covering all configuration sections:
//   Enforcement · Economy · Emotional · AI · Social
//   Notifications · Data · About

import SwiftUI

// MARK: - Settings ViewModel

@MainActor
final class SettingsViewModel: ObservableObject {

    // Enforcement
    @Published var defaultMode: EnforcementMode = .softLock
    @Published var cooldownMinutes: Double = 15
    @Published var bypassList: [String] = []

    // Economy
    @Published var fpEnabled: Bool = true
    @Published var dailyBaselineFP: Int = 60
    @Published var antiGamingEnabled: Bool = true

    // Emotional
    @Published var checkInEnabled: Bool = true
    @Published var coolingOffEnabled: Bool = true

    // AI
    @Published var cloudInsightsEnabled: Bool = true
    @Published var viewAnonymization: Bool = true

    // Social
    @Published var sharingLevel: SettingsSharingLevel = .friends
    @Published var buddies: [String] = []
    @Published var circles: [String] = []

    // Notifications
    @Published var nudgeNotifications: Bool = true
    @Published var insightNotifications: Bool = true
    @Published var challengeNotifications: Bool = true
    @Published var quietHoursStart: Date = Calendar.current.date(from: DateComponents(hour: 22, minute: 0)) ?? Date()
    @Published var quietHoursEnd: Date = Calendar.current.date(from: DateComponents(hour: 8, minute: 0)) ?? Date()
    @Published var quietHoursEnabled: Bool = true

    // State
    @Published var showDeleteConfirm: Bool = false
    @Published var showDeleteAccountConfirm: Bool = false
    @Published var isExporting: Bool = false
    @Published var exportedData: String?

    func exportData() async {
        isExporting = true
        defer { isExporting = false }
        // In production, serialize from shared KMP module.
        let stub: [String: Any] = [
            "exported_at": ISO8601DateFormatter().string(from: Date()),
            "fp_balance": 0,
            "sessions": [],
            "check_ins": []
        ]
        exportedData = (try? JSONSerialization.data(withJSONObject: stub, options: .prettyPrinted))
            .flatMap { String(data: $0, encoding: .utf8) }
    }

    func deleteAllData() {
        // Calls shared KMP repository clear methods in production.
        UserDefaults(suiteName: "group.dev.bilbo.app")?.removePersistentDomain(forName: "group.dev.bilbo.app")
    }

    func deleteAccount() {
        deleteAllData()
        // Sign out & delete Supabase user in production.
    }
}

enum EnforcementMode: String, CaseIterable, Identifiable {
    case softLock = "Soft Lock"
    case hardLock = "Hard Lock"
    case trackOnly = "Track Only"
    var id: String { rawValue }
}

/// Settings-scoped sharing level (display-only strings). Distinct from the
/// domain `SharingLevel` enum used by the Social feature.
enum SettingsSharingLevel: String, CaseIterable, Identifiable {
    case `private` = "Private"
    case friends   = "Friends"
    case circle    = "Circle"
    case `public`  = "Public"
    var id: String { rawValue }
}

// MARK: - Root SettingsView

struct SettingsView: View {
    @StateObject private var vm = SettingsViewModel()

    var body: some View {
        List {
            enforcementSection
            economySection
            emotionalSection
            aiSection
            socialSection
            notificationsSection
            dataSection
            aboutSection
        }
        .listStyle(.insetGrouped)
        .confirmationDialog(
            "Delete all data? This cannot be undone.",
            isPresented: $vm.showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("Delete All Data", role: .destructive) { vm.deleteAllData() }
            Button("Cancel", role: .cancel) {}
        }
        .confirmationDialog(
            "Delete your account? All data will be permanently removed.",
            isPresented: $vm.showDeleteAccountConfirm,
            titleVisibility: .visible
        ) {
            Button("Delete Account", role: .destructive) { vm.deleteAccount() }
            Button("Cancel", role: .cancel) {}
        }
        .sheet(item: $vm.exportedData.optionalBinding()) { data in
            ShareSheet(items: [data])
        }
    }

    // MARK: - Enforcement

    private var enforcementSection: some View {
        Section {
            Picker("Default Mode", selection: $vm.defaultMode) {
                ForEach(EnforcementMode.allCases) { mode in
                    Text(mode.rawValue).tag(mode)
                }
            }

            VStack(alignment: .leading, spacing: 6) {
                HStack {
                    Text("Cooldown Duration")
                    Spacer()
                    Text("\(Int(vm.cooldownMinutes)) min")
                        .foregroundStyle(.secondary)
                }
                Slider(value: $vm.cooldownMinutes, in: 5...60, step: 5)
                    .tint(Color("BilboPurple"))
            }

            NavigationLink("Per-App Overrides") {
                PerAppOverridesView()
            }
        } header: {
            Text("Enforcement")
        } footer: {
            Text("Soft Lock shows a reminder. Hard Lock requires FP to override.")
        }
    }

    // MARK: - Economy

    private var economySection: some View {
        Section {
            Toggle("Enable Focus Points", isOn: $vm.fpEnabled)
                .tint(Color("BilboTeal"))

            if vm.fpEnabled {
                Stepper("Daily Baseline: \(vm.dailyBaselineFP) FP",
                        value: $vm.dailyBaselineFP, in: 0...200, step: 10)

                Toggle("Anti-Gaming Protection", isOn: $vm.antiGamingEnabled)
                    .tint(Color("BilboTeal"))
            }
        } header: {
            Text("Economy")
        } footer: {
            Text("Focus Points are earned for intentional use and spent on overrides.")
        }
    }

    // MARK: - Emotional

    private var emotionalSection: some View {
        Section {
            Toggle("Enable Emotional Check-In", isOn: $vm.checkInEnabled)
                .tint(Color("BilboTeal"))

            if vm.checkInEnabled {
                Toggle("Cooling-Off Mode", isOn: $vm.coolingOffEnabled)
                    .tint(Color("BilboTeal"))
            }
        } header: {
            Text("Emotional")
        } footer: {
            Text("Check-ins help Bilbo understand your mood before and after phone use.")
        }
    }

    // MARK: - AI

    private var aiSection: some View {
        Section {
            Toggle("Cloud AI Insights", isOn: $vm.cloudInsightsEnabled)
                .tint(Color("BilboTeal"))

            if vm.cloudInsightsEnabled {
                Toggle("Anonymize Before Sending", isOn: $vm.viewAnonymization)
                    .tint(Color("BilboTeal"))

                NavigationLink("Preview Data Sent to AI") {
                    DataSentPreviewView(anonymized: vm.viewAnonymization)
                }
            }
        } header: {
            Text("AI Insights")
        } footer: {
            Text("When enabled, anonymized usage data is sent to generate personalized insights.")
        }
    }

    // MARK: - Social

    private var socialSection: some View {
        Section {
            Picker("Sharing Level", selection: $vm.sharingLevel) {
                ForEach(SettingsSharingLevel.allCases) { level in
                    Text(level.rawValue).tag(level)
                }
            }

            NavigationLink("Manage Accountability Buddies") {
                BuddyManagementView()
            }

            NavigationLink("Manage Circles") {
                CircleManagementView()
            }
        } header: {
            Text("Social")
        }
    }

    // MARK: - Notifications

    private var notificationsSection: some View {
        Section {
            Toggle("Nudge Notifications", isOn: $vm.nudgeNotifications)
                .tint(Color("BilboTeal"))
            Toggle("Weekly Insight Ready", isOn: $vm.insightNotifications)
                .tint(Color("BilboTeal"))
            Toggle("Challenge Updates", isOn: $vm.challengeNotifications)
                .tint(Color("BilboTeal"))

            Toggle("Quiet Hours", isOn: $vm.quietHoursEnabled)
                .tint(Color("BilboTeal"))

            if vm.quietHoursEnabled {
                DatePicker("Start", selection: $vm.quietHoursStart, displayedComponents: .hourAndMinute)
                DatePicker("End", selection: $vm.quietHoursEnd, displayedComponents: .hourAndMinute)
            }
        } header: {
            Text("Notifications")
        }
    }

    // MARK: - Data

    private var dataSection: some View {
        Section {
            Button {
                Task { await vm.exportData() }
            } label: {
                HStack {
                    Label("Export All Data as JSON", systemImage: "square.and.arrow.up")
                    Spacer()
                    if vm.isExporting {
                        ProgressView()
                    }
                }
            }
            .disabled(vm.isExporting)

            Button(role: .destructive) {
                vm.showDeleteConfirm = true
            } label: {
                Label("Delete All Data", systemImage: "trash")
            }

            Button(role: .destructive) {
                vm.showDeleteAccountConfirm = true
            } label: {
                Label("Delete Account", systemImage: "person.crop.circle.badge.minus")
            }
        } header: {
            Text("Data")
        } footer: {
            Text("Deleting your account is permanent and cannot be recovered.")
        }
    }

    // MARK: - About

    private var aboutSection: some View {
        Section {
            HStack {
                Text("Version")
                Spacer()
                Text(appVersion)
                    .foregroundStyle(.secondary)
            }

            NavigationLink("Open-Source Licenses") {
                LicensesView()
            }

            Link(destination: URL(string: "https://getbilbo.app/privacy")!) {
                HStack {
                    Text("Privacy Policy")
                    Spacer()
                    Image(systemName: "arrow.up.right.square")
                        .foregroundStyle(.secondary)
                }
            }
        } header: {
            Text("About")
        }
    }

    private var appVersion: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "1.0"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "1"
        return "\(version) (\(build))"
    }
}

// MARK: - Placeholder sub-screens

struct PerAppOverridesView: View {
    var body: some View {
        List {
            Text("Per-app overrides coming soon")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Per-App Overrides")
    }
}

struct DataSentPreviewView: View {
    let anonymized: Bool
    var body: some View {
        ScrollView {
            Text(anonymized ? "{ \"sessions\": [\"***\", \"***\"] }" : "{ \"sessions\": [\"Instagram: 12m\", \"YouTube: 22m\"] }")
                .font(.system(.caption, design: .monospaced))
                .padding()
        }
        .navigationTitle("Data Preview")
    }
}

struct BuddyManagementView: View {
    var body: some View {
        List {
            Text("No buddies yet. Invite someone!")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Accountability Buddies")
    }
}

struct CircleManagementView: View {
    var body: some View {
        List {
            Text("No circles yet.")
                .foregroundStyle(.secondary)
        }
        .navigationTitle("Circles")
    }
}

struct LicensesView: View {
    var body: some View {
        List {
            LabeledContent("Kotlin Multiplatform", value: "Apache 2.0")
            LabeledContent("Supabase Swift", value: "MIT")
            LabeledContent("Charts (Swift)", value: "Apache 2.0")
        }
        .navigationTitle("Licenses")
    }
}

// MARK: - ShareSheet

struct ShareSheet: UIViewControllerRepresentable {
    let items: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: items, applicationActivities: nil)
    }

    func updateUIViewController(_ vc: UIActivityViewController, context: Context) {}
}

// MARK: - Optional Binding extension

extension Optional where Wrapped == String {
    func optionalBinding() -> Binding<Wrapped?> {
        Binding<Wrapped?>(
            get: { self },
            set: { _ in }
        )
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        SettingsView()
            .navigationTitle("Settings")
    }
}
