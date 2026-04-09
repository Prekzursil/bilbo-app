// DashboardView.swift
// Spark — iOS Dashboard
//
// Today's overview:
//   • Large FP balance display
//   • Quick stats row (screen time, intent accuracy, streak)
//   • Recent activity timeline (last 5 sessions)
//   • Weekly mini chart
//   • Quick action buttons: Check In & Browse Suggestions

import SwiftUI
import Charts

// MARK: - View Models / Data types

struct ActivitySession: Identifiable {
    let id = UUID()
    let appName: String
    let bundleId: String
    let startTime: Date
    let durationMinutes: Double
    let wasIntentional: Bool
}

struct WeeklyBarDatum: Identifiable {
    let id = UUID()
    let day: String     // "Mon", "Tue", …
    let screenMinutes: Double
    let fpEarned: Double
}

// MARK: - ViewModel

@MainActor
final class DashboardViewModel: ObservableObject {
    @Published var fpBalance: Int = 0
    @Published var screenTimeMinutesToday: Double = 0
    @Published var intentAccuracyPercent: Double = 0
    @Published var streakDays: Int = 0
    @Published var recentSessions: [ActivitySession] = []
    @Published var weeklyData: [WeeklyBarDatum] = []
    @Published var isLoading: Bool = false

    private let defaults = UserDefaults(suiteName: "group.dev.spark.app") ?? .standard

    func load() async {
        isLoading = true
        defer { isLoading = false }
        // In production these come from the shared KMP module via Swift-Kotlin bridge.
        // Populated here with representative sample data.
        fpBalance = defaults.integer(forKey: "fp_balance")
        screenTimeMinutesToday = 142
        intentAccuracyPercent = 0.74
        streakDays = 5

        recentSessions = [
            ActivitySession(appName: "Instagram",    bundleId: "com.burbn.instagram",  startTime: Date().addingTimeInterval(-3600),  durationMinutes: 12, wasIntentional: false),
            ActivitySession(appName: "Safari",       bundleId: "com.apple.mobilesafari", startTime: Date().addingTimeInterval(-7200), durationMinutes: 8,  wasIntentional: true),
            ActivitySession(appName: "YouTube",      bundleId: "com.google.ios.youtube", startTime: Date().addingTimeInterval(-10800),durationMinutes: 22, wasIntentional: false),
            ActivitySession(appName: "Slack",        bundleId: "com.tinyspeck.itsalive", startTime: Date().addingTimeInterval(-14400),durationMinutes: 6,  wasIntentional: true),
            ActivitySession(appName: "TikTok",       bundleId: "com.zhiliaoapp.musically",startTime: Date().addingTimeInterval(-18000),durationMinutes: 31, wasIntentional: false),
        ]

        weeklyData = [
            WeeklyBarDatum(day: "Mon", screenMinutes: 185, fpEarned: 40),
            WeeklyBarDatum(day: "Tue", screenMinutes: 160, fpEarned: 55),
            WeeklyBarDatum(day: "Wed", screenMinutes: 210, fpEarned: 30),
            WeeklyBarDatum(day: "Thu", screenMinutes: 142, fpEarned: 60),
            WeeklyBarDatum(day: "Fri", screenMinutes: 0,   fpEarned: 0),
            WeeklyBarDatum(day: "Sat", screenMinutes: 0,   fpEarned: 0),
            WeeklyBarDatum(day: "Sun", screenMinutes: 0,   fpEarned: 0),
        ]
    }
}

// MARK: - Root Dashboard View

struct DashboardView: View {
    @StateObject private var vm = DashboardViewModel()
    @State private var showCheckIn = false
    @State private var showSuggestions = false

    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                fpBalanceSection
                quickStatsSection
                quickActionsSection
                recentActivitySection
                weeklyChartSection
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
        }
        .background(Color(.systemGroupedBackground))
        .refreshable { await vm.load() }
        .task { await vm.load() }
        .sheet(isPresented: $showCheckIn) {
            CheckInSheet()
        }
        .sheet(isPresented: $showSuggestions) {
            SuggestionsSheet()
        }
    }

    // MARK: - FP Balance

    private var fpBalanceSection: some View {
        VStack(spacing: 4) {
            Text("Focus Points")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .textCase(.uppercase)

            HStack(alignment: .lastTextBaseline, spacing: 4) {
                Text("⚡")
                    .font(.system(size: 36))
                Text("\(vm.fpBalance)")
                    .font(.system(size: 72, weight: .black, design: .rounded))
                    .foregroundStyle(Color("SparkPurple"))
            }
            .animation(.spring(response: 0.4), value: vm.fpBalance)

            Text("Today's Balance")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 28)
        .background(
            RoundedRectangle(cornerRadius: 20)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.06), radius: 8, x: 0, y: 2)
        )
    }

    // MARK: - Quick Stats

    private var quickStatsSection: some View {
        HStack(spacing: 12) {
            QuickStatCard(
                label: "Screen Time",
                value: formatMinutes(vm.screenTimeMinutesToday),
                icon: "clock.fill",
                color: .orange
            )
            QuickStatCard(
                label: "Intent Accuracy",
                value: "\(Int(vm.intentAccuracyPercent * 100))%",
                icon: "target",
                color: Color("SparkTeal")
            )
            QuickStatCard(
                label: "Streak",
                value: "\(vm.streakDays)d",
                icon: "flame.fill",
                color: .red
            )
        }
    }

    // MARK: - Quick Actions

    private var quickActionsSection: some View {
        HStack(spacing: 12) {
            Button {
                showCheckIn = true
            } label: {
                Label("Check In", systemImage: "checkmark.circle.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(Color("SparkPurple"))

            Button {
                showSuggestions = true
            } label: {
                Label("Browse Suggestions", systemImage: "lightbulb.fill")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.bordered)
            .tint(Color("SparkTeal"))
        }
    }

    // MARK: - Recent Activity

    private var recentActivitySection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "Recent Activity")

            VStack(spacing: 0) {
                ForEach(vm.recentSessions) { session in
                    ActivityRow(session: session)
                    if session.id != vm.recentSessions.last?.id {
                        Divider().padding(.leading, 52)
                    }
                }
            }
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 1)
            )
        }
    }

    // MARK: - Weekly Chart

    @ViewBuilder
    private var weeklyChartSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            SectionHeader(title: "This Week")

            Chart(vm.weeklyData) { datum in
                BarMark(
                    x: .value("Day", datum.day),
                    y: .value("Minutes", datum.screenMinutes)
                )
                .foregroundStyle(
                    datum.screenMinutes < 120
                        ? Color("SparkTeal").gradient
                        : Color("SparkPurple").gradient
                )
                .cornerRadius(6)
            }
            .frame(height: 140)
            .chartYAxis {
                AxisMarks(values: .stride(by: 60)) { value in
                    AxisGridLine()
                    AxisValueLabel {
                        if let minutes = value.as(Double.self) {
                            Text(formatMinutes(minutes))
                                .font(.caption2)
                        }
                    }
                }
            }
            .padding()
            .background(
                RoundedRectangle(cornerRadius: 16)
                    .fill(Color(.systemBackground))
                    .shadow(color: .black.opacity(0.05), radius: 6, x: 0, y: 1)
            )
        }
    }

    // MARK: - Helpers

    private func formatMinutes(_ m: Double) -> String {
        let h = Int(m) / 60
        let min = Int(m) % 60
        if h > 0 { return "\(h)h \(min)m" }
        return "\(min)m"
    }
}

// MARK: - Supporting Views

struct QuickStatCard: View {
    let label: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(spacing: 6) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(color)
            Text(value)
                .font(.system(.title3, design: .rounded, weight: .bold))
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(
            RoundedRectangle(cornerRadius: 14)
                .fill(Color(.systemBackground))
                .shadow(color: .black.opacity(0.05), radius: 4, x: 0, y: 1)
        )
    }
}

struct SectionHeader: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.headline)
            .foregroundStyle(.primary)
    }
}

struct ActivityRow: View {
    let session: ActivitySession

    var body: some View {
        HStack(spacing: 12) {
            // App icon placeholder
            RoundedRectangle(cornerRadius: 10)
                .fill(session.wasIntentional ? Color("SparkTeal").opacity(0.15) : Color.orange.opacity(0.15))
                .frame(width: 40, height: 40)
                .overlay {
                    Image(systemName: session.wasIntentional ? "checkmark.circle" : "exclamationmark.circle")
                        .foregroundStyle(session.wasIntentional ? Color("SparkTeal") : .orange)
                }

            VStack(alignment: .leading, spacing: 2) {
                Text(session.appName)
                    .font(.subheadline).bold()
                Text(session.wasIntentional ? "Intentional" : "Unintentional")
                    .font(.caption)
                    .foregroundStyle(session.wasIntentional ? Color("SparkTeal") : .orange)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text("\(Int(session.durationMinutes))m")
                    .font(.subheadline).bold()
                Text(session.startTime, style: .time)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
    }
}

// MARK: - Sheet stubs

struct CheckInSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                Text("How are you feeling right now?")
                    .font(.title3).bold()
                    .multilineTextAlignment(.center)
                    .padding(.top, 24)
                Spacer()
            }
            .padding()
            .navigationTitle("Check In")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
        }
    }
}

struct SuggestionsSheet: View {
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            Text("Analog suggestions coming soon")
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .navigationTitle("Browse Suggestions")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Close") { dismiss() }
                    }
                }
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        DashboardView()
            .navigationTitle("Dashboard")
    }
}
