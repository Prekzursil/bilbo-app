import SwiftUI
import Charts

// MARK: - UI State

/// All display data for the budget dashboard.
/// In production this is owned by an `ObservableObject` view model.
struct BudgetDashboardUiState {
    var currentBalance: Int        = 0
    var fpEarned: Int              = 0
    var fpSpent: Int               = 0
    var fpBonus: Int               = 0
    var nutritiveMinutes: Int      = 0
    var neutralMinutes: Int        = 0
    var emptyCalorieMinutes: Int   = 0
    var streakDays: Int            = 0
    /// Daily FP balances, index 0 = oldest (6 days ago), 6 = today.
    var weeklyBalances: [Int]      = Array(repeating: 0, count: 7)
    var isRefreshing: Bool         = false
}

// MARK: - BudgetDashboardView

/// SwiftUI full-screen dopamine economy dashboard.
/// Mirrors the Android `BudgetDashboardScreen` layout and features.
struct BudgetDashboardView: View {

    let state: BudgetDashboardUiState
    var onRefresh: () async -> Void = {}
    var onBack: (() -> Void)? = nil

    // MARK: Body

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 16) {
                    // 1. FP Balance widget ────────────────────────────────
                    FPBalanceView(
                        currentBalance: state.currentBalance,
                        fpEarned: state.fpEarned,
                        size: 160
                    )
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 8)

                    // 2. Today's Activity ─────────────────────────────────
                    SectionCard(title: "Today's Activity") {
                        TodayActivitySection(
                            earned: state.fpEarned,
                            spent: state.fpSpent,
                            bonus: state.fpBonus
                        )
                    }

                    // 3. This Week ────────────────────────────────────────
                    SectionCard(title: "This Week") {
                        WeeklyChartView(balances: state.weeklyBalances)
                            .frame(height: 120)
                    }

                    // 4. Time Breakdown ───────────────────────────────────
                    SectionCard(title: "Time Breakdown") {
                        TimeBreakdownSection(
                            nutritiveMinutes: state.nutritiveMinutes,
                            neutralMinutes: state.neutralMinutes,
                            emptyCalorieMinutes: state.emptyCalorieMinutes
                        )
                    }

                    // 5. Streak ───────────────────────────────────────────
                    SectionCard(title: "Streak") {
                        StreakSection(streakDays: state.streakDays)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.bottom, 24)
            }
            .refreshable { await onRefresh() }
            .navigationTitle("Focus Economy")
            .navigationBarTitleDisplayMode(.large)
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
}

// MARK: - Today's Activity

private struct TodayActivitySection: View {
    let earned: Int
    let spent: Int
    let bonus: Int

    private var total: Int { max(earned + spent, 1) }
    private var earnedFrac: Double { Double(earned) / Double(total) }
    private var spentFrac: Double  { Double(spent)  / Double(total) }

    @State private var appeared = false

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            // Split bar
            GeometryReader { geo in
                HStack(spacing: 0) {
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.fpGreen)
                        .frame(
                            width: appeared ? geo.size.width * earnedFrac : 0,
                            height: 12
                        )
                    RoundedRectangle(cornerRadius: 6)
                        .fill(Color.fpRed)
                        .frame(
                            width: appeared ? geo.size.width * spentFrac : 0,
                            height: 12
                        )
                    Spacer()
                }
                .background(Color(.systemGray5), in: RoundedRectangle(cornerRadius: 6))
                .animation(.easeInOut(duration: 0.7), value: appeared)
            }
            .frame(height: 12)

            Text("+\(earned) earned  |  -\(spent) spent  |  +\(bonus) bonus")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .onAppear { appeared = true }
    }
}

// MARK: - Weekly Chart

private struct WeeklyChartView: View {
    let balances: [Int]

    private var data: [(index: Int, label: String, value: Int)] {
        let labels = ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"]
        let padded  = Array(repeating: 0, count: max(0, 7 - balances.count)) + balances.suffix(7)
        return padded.enumerated().map { (i, v) in
            (index: i, label: labels[i % 7], value: v)
        }
    }

    var body: some View {
        Chart(data, id: \.index) { point in
            LineMark(
                x: .value("Day", point.label),
                y: .value("FP", point.value)
            )
            .foregroundStyle(Color.accentColor.opacity(0.6))
            .interpolationMethod(.catmullRom)

            PointMark(
                x: .value("Day", point.label),
                y: .value("FP", point.value)
            )
            .foregroundStyle(
                point.index == data.count - 1 ? Color.fpGreen : Color.accentColor
            )
            .symbolSize(point.index == data.count - 1 ? 120 : 50)
        }
        .chartXAxis {
            AxisMarks(values: .automatic) { value in
                AxisValueLabel()
                    .font(.caption2)
            }
        }
        .chartYAxis {
            AxisMarks(values: .automatic(desiredCount: 3)) { value in
                AxisGridLine()
                AxisValueLabel()
                    .font(.caption2)
            }
        }
    }
}

// MARK: - Time Breakdown

private struct TimeBreakdownSection: View {
    let nutritiveMinutes: Int
    let neutralMinutes: Int
    let emptyCalorieMinutes: Int

    var body: some View {
        VStack(spacing: 8) {
            TimeBar(label: "Nutritive",      minutes: nutritiveMinutes,     color: .fpGreen)
            TimeBar(label: "Neutral",        minutes: neutralMinutes,       color: Color(.systemGray))
            TimeBar(label: "Empty Calories", minutes: emptyCalorieMinutes,  color: .fpRed)
        }
    }
}

private struct TimeBar: View {
    let label: String
    let minutes: Int
    let color: Color

    private let maxMinutes: Double = 480   // 8 hours scale
    @State private var appeared = false

    private var fraction: Double {
        min(Double(minutes) / maxMinutes, 1)
    }

    var body: some View {
        HStack(spacing: 8) {
            Text(label)
                .font(.caption)
                .frame(width: 110, alignment: .leading)
                .foregroundColor(.primary)

            GeometryReader { geo in
                ZStack(alignment: .leading) {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(color.opacity(0.15))
                        .frame(height: 8)

                    RoundedRectangle(cornerRadius: 4)
                        .fill(color)
                        .frame(
                            width: appeared ? geo.size.width * fraction : 0,
                            height: 8
                        )
                        .animation(.easeInOut(duration: 0.7), value: appeared)
                }
            }
            .frame(height: 8)

            Text("\(minutes)m")
                .font(.caption2)
                .foregroundColor(.secondary)
                .frame(width: 36, alignment: .trailing)
        }
        .onAppear { appeared = true }
    }
}

// MARK: - Streak

private struct StreakSection: View {
    let streakDays: Int

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "flame.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 32, height: 32)
                .foregroundColor(.fpYellow)

            VStack(alignment: .leading, spacing: 2) {
                Text("\(streakDays) day streak")
                    .font(.headline)
                    .fontWeight(.bold)

                if streakDays >= 7 {
                    Text("7-day bonus: +20 FP")
                        .font(.caption)
                        .foregroundColor(.fpYellow)
                }
            }
            Spacer()
        }
    }
}

// MARK: - Section Card

private struct SectionCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: () -> Content

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(title)
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundColor(.primary)

            content()
        }
        .padding(16)
        .background(Color(.secondarySystemBackground), in: RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Preview

#Preview {
    BudgetDashboardView(
        state: BudgetDashboardUiState(
            currentBalance: 38,
            fpEarned: 25,
            fpSpent: 10,
            fpBonus: 5,
            nutritiveMinutes: 40,
            neutralMinutes: 60,
            emptyCalorieMinutes: 30,
            streakDays: 9,
            weeklyBalances: [20, 35, 28, 42, 15, 50, 38]
        )
    )
}
