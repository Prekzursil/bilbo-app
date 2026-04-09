import SwiftUI
import Charts

// MARK: - Domain models (mirrors Kotlin shared layer)

enum InsightType: String {
    case correlation = "CORRELATION"
    case trend       = "TREND"
    case anomaly     = "ANOMALY"
    case achievement = "ACHIEVEMENT"
}

struct HeuristicInsightItem: Identifiable {
    let id = UUID()
    let type: InsightType
    let message: String
    let confidence: Float
}

struct WeeklyInsightData {
    var weekStart: String          = ""
    var tier3Narrative: String?    = nil
    var isCloudInsight: Bool       = false
    var totalScreenTimeMinutes: Int = 0
    var nutritiveMinutes: Int      = 0
    var emptyCalorieMinutes: Int   = 0
    var fpEarned: Int              = 0
    var fpSpent: Int               = 0
    var intentAccuracyPercent: Float = 0
    var streakDays: Int            = 0
    var heuristicInsights: [HeuristicInsightItem] = []
    var dailyMinutes: [Int]        = Array(repeating: 0, count: 7)
    var canRefresh: Bool           = true
}

// MARK: - WeeklyInsightView

/// SwiftUI weekly insight screen — mirrors the Android WeeklyInsightScreen.
/// Sections: hero narrative card, stats, 7-day chart, correlation cards, streak.
struct WeeklyInsightView: View {

    let data: WeeklyInsightData
    var onRefresh: () async -> Void = {}
    var onBack: (() -> Void)? = nil

    @State private var isRefreshing = false

    // MARK: Body

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {

                    // 1. Hero narrative card
                    narrativeHeroCard

                    // 2. Stats row
                    statsRow

                    // 3. 7-day trend chart
                    trendChartCard

                    // 4. Insight cards (top 3)
                    let topInsights = data.heuristicInsights
                        .sorted { $0.confidence > $1.confidence }
                        .prefix(3)

                    if !topInsights.isEmpty {
                        HStack {
                            Text("Insights")
                                .font(.headline)
                            Spacer()
                        }
                        .padding(.horizontal)

                        ForEach(topInsights) { insight in
                            InsightCardView(insight: insight)
                                .padding(.horizontal)
                        }
                    }

                    // 5. Streak card
                    streakCard

                    // 6. Rate limit banner
                    if !data.canRefresh {
                        rateLimitBanner
                    }
                }
                .padding(.vertical, 12)
            }
            .refreshable {
                guard data.canRefresh else { return }
                isRefreshing = true
                await onRefresh()
                isRefreshing = false
            }
            .navigationTitle("Weekly Insight")
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

    // MARK: - Hero narrative card

    @ViewBuilder
    private var narrativeHeroCard: some View {
        let narrative = data.tier3Narrative ?? fallbackNarrative

        if data.isCloudInsight {
            // Cloud AI styled card
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 6) {
                    Text("✨")
                    Text("AI Insight")
                        .font(.caption)
                        .fontWeight(.semibold)
                        .foregroundStyle(.purple)
                }
                .padding(.horizontal, 10)
                .padding(.vertical, 5)
                .background(Color.purple.opacity(0.12), in: Capsule())

                Text(narrative)
                    .font(.body)
                    .italic()
                    .lineSpacing(6)
                    .foregroundStyle(.primary)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                LinearGradient(
                    colors: [Color.purple.opacity(0.08), Color.blue.opacity(0.06)],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 20)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 20)
                    .strokeBorder(Color.purple.opacity(0.2), lineWidth: 1)
            )
            .padding(.horizontal)
        } else {
            // Local Tier-2 card
            VStack(alignment: .leading, spacing: 10) {
                Image(systemName: "lightbulb.fill")
                    .font(.title3)
                    .foregroundStyle(.orange)

                Text(narrative)
                    .font(.body)
                    .lineSpacing(5)
                    .foregroundStyle(.primary)
            }
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 20))
            .padding(.horizontal)
        }
    }

    // MARK: - Stats row

    private var statsRow: some View {
        let hours = data.totalScreenTimeMinutes / 60
        let mins  = data.totalScreenTimeMinutes % 60
        let timeLabel = hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"

        let nutritivePct = data.totalScreenTimeMinutes > 0
            ? data.nutritiveMinutes * 100 / data.totalScreenTimeMinutes : 0
        let emptyPct = data.totalScreenTimeMinutes > 0
            ? data.emptyCalorieMinutes * 100 / data.totalScreenTimeMinutes : 0
        let fpNet = data.fpEarned - data.fpSpent

        return ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                StatCardView(label: "Screen Time",  value: timeLabel,         icon: "timer",            color: .orange)
                StatCardView(label: "Nutritive",    value: "\(nutritivePct)%", icon: "leaf.fill",        color: .green)
                StatCardView(label: "Empty Cal.",   value: "\(emptyPct)%",    icon: "iphone",           color: .red)
                StatCardView(label: "FP Balance",   value: "\(fpNet > 0 ? "+" : "")\(fpNet)", icon: "star.fill", color: .yellow)
            }
            .padding(.horizontal)
        }
    }

    // MARK: - 7-day trend chart

    private var trendChartCard: some View {
        let days = ["M", "T", "W", "T", "F", "S", "S"]
        let points = data.dailyMinutes.prefix(7).enumerated().map { (i, v) in
            (day: days[min(i, 6)], index: i, minutes: v)
        }

        return VStack(alignment: .leading, spacing: 12) {
            Text("7-Day Screen Time")
                .font(.headline)

            if #available(iOS 16.0, *) {
                Chart {
                    ForEach(points, id: \.index) { point in
                        AreaMark(
                            x: .value("Day", point.index),
                            y: .value("Minutes", point.minutes)
                        )
                        .foregroundStyle(
                            LinearGradient(
                                colors: [Color.orange.opacity(0.4), Color.orange.opacity(0.05)],
                                startPoint: .top, endPoint: .bottom
                            )
                        )
                        LineMark(
                            x: .value("Day", point.index),
                            y: .value("Minutes", point.minutes)
                        )
                        .foregroundStyle(Color.orange)
                        .lineStyle(StrokeStyle(lineWidth: 2.5, lineCap: .round))
                        PointMark(
                            x: .value("Day", point.index),
                            y: .value("Minutes", point.minutes)
                        )
                        .foregroundStyle(Color.orange)
                        .symbolSize(30)
                    }
                }
                .chartXAxis {
                    AxisMarks(values: .stride(by: 1)) { value in
                        AxisValueLabel {
                            if let idx = value.as(Int.self), idx < days.count {
                                Text(days[idx])
                                    .font(.caption2)
                            }
                        }
                    }
                }
                .chartYAxis {
                    AxisMarks { value in
                        AxisValueLabel {
                            if let v = value.as(Int.self) {
                                Text("\(v)m")
                                    .font(.caption2)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .frame(height: 130)
            } else {
                // Fallback bar chart for iOS < 16
                HStack(alignment: .bottom, spacing: 8) {
                    let maxVal = CGFloat(data.dailyMinutes.max() ?? 1)
                    ForEach(Array(data.dailyMinutes.prefix(7).enumerated()), id: \.offset) { i, v in
                        VStack(spacing: 4) {
                            Spacer()
                            RoundedRectangle(cornerRadius: 4)
                                .fill(Color.orange)
                                .frame(
                                    height: maxVal > 0 ? CGFloat(v) / maxVal * 100 : 4
                                )
                            Text(days[min(i, 6)])
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .frame(height: 130)
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Streak card

    private var streakCard: some View {
        let totalDots = 28
        let activeDots = min(data.streakDays, totalDots)

        return VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Text("🔥")
                    .font(.title2)
                Text("\(data.streakDays)-day streak")
                    .font(.headline)
                    .fontWeight(.bold)
            }

            // 4-week dot grid
            VStack(spacing: 6) {
                ForEach(0..<4, id: \.self) { week in
                    HStack(spacing: 6) {
                        ForEach(0..<7, id: \.self) { day in
                            let dotIndex = week * 7 + day
                            let isActive = dotIndex >= (totalDots - activeDots)
                            Circle()
                                .fill(isActive ? Color.orange : Color.gray.opacity(0.3))
                                .frame(width: 10, height: 10)
                        }
                        Spacer()
                    }
                }
            }

            Text("4-week history")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal)
    }

    // MARK: - Rate limit banner

    private var rateLimitBanner: some View {
        HStack(spacing: 10) {
            Image(systemName: "info.circle")
                .foregroundStyle(.secondary)
            Text("AI insights refresh once per week. Check back next Sunday.")
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.tertiarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 12))
        .padding(.horizontal)
    }

    // MARK: - Helpers

    private var fallbackNarrative: String {
        let hours = data.totalScreenTimeMinutes / 60
        let mins  = data.totalScreenTimeMinutes % 60
        let timeStr = hours > 0 ? "\(hours)h \(mins)m" : "\(mins)m"
        if data.streakDays >= 3 {
            return "This week you spent \(timeStr) on your phone. You kept a \(data.streakDays)-day streak — great work!"
        }
        return "This week you spent \(timeStr) on your phone. Keep building your streak — every day counts."
    }
}

// MARK: - InsightCardView

struct InsightCardView: View {
    let insight: HeuristicInsightItem

    var body: some View {
        let (iconName, color) = iconAndColor

        let confidenceLabel: String
        let confidenceColor: Color
        switch insight.confidence {
        case 0.8...:
            confidenceLabel = "High"
            confidenceColor = .green
        case 0.6..<0.8:
            confidenceLabel = "Medium"
            confidenceColor = .orange
        default:
            confidenceLabel = "Low"
            confidenceColor = .secondary
        }

        return HStack(alignment: .top, spacing: 12) {
            ZStack {
                Circle()
                    .fill(color.opacity(0.12))
                    .frame(width: 40, height: 40)
                Image(systemName: iconName)
                    .font(.system(size: 16))
                    .foregroundStyle(color)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text(insight.message)
                    .font(.subheadline)
                    .fixedSize(horizontal: false, vertical: true)

                Text("\(confidenceLabel) confidence")
                    .font(.caption2)
                    .fontWeight(.medium)
                    .foregroundStyle(confidenceColor)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(confidenceColor.opacity(0.12), in: Capsule())
            }

            Spacer()
        }
        .padding(14)
        .background(Color(.systemBackground), in: RoundedRectangle(cornerRadius: 14))
        .shadow(color: .black.opacity(0.06), radius: 6, y: 2)
    }

    private var iconAndColor: (String, Color) {
        switch insight.type {
        case .correlation: return ("arrow.triangle.2.circlepath", .blue)
        case .trend:       return ("chart.line.uptrend.xyaxis", .orange)
        case .anomaly:     return ("exclamationmark.triangle", .red)
        case .achievement: return ("trophy.fill", .green)
        }
    }
}

// MARK: - StatCardView

struct StatCardView: View {
    let label: String
    let value: String
    let icon: String
    let color: Color

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Image(systemName: icon)
                .font(.system(size: 16))
                .foregroundStyle(color)

            Text(value)
                .font(.headline)
                .fontWeight(.bold)

            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .padding(14)
        .frame(width: 106)
        .background(Color(.secondarySystemGroupedBackground), in: RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Preview

#Preview {
    WeeklyInsightView(
        data: WeeklyInsightData(
            weekStart: "2026-04-07",
            tier3Narrative: "This was a mindful week. You reduced your scrolling time by 12% and maintained a strong streak.",
            isCloudInsight: true,
            totalScreenTimeMinutes: 312,
            nutritiveMinutes: 87,
            emptyCalorieMinutes: 148,
            fpEarned: 105,
            fpSpent: 60,
            intentAccuracyPercent: 0.78,
            streakDays: 5,
            heuristicInsights: [
                HeuristicInsightItem(type: .achievement, message: "You stayed under your daily average for 5 days in a row.", confidence: 0.92),
                HeuristicInsightItem(type: .correlation, message: "When you feel Stressed, scrolling apps usage increases significantly.", confidence: 0.78),
                HeuristicInsightItem(type: .trend, message: "Mondays tend to be your highest-usage day.", confidence: 0.65),
            ],
            dailyMinutes: [45, 62, 38, 71, 28, 35, 33]
        )
    )
}
