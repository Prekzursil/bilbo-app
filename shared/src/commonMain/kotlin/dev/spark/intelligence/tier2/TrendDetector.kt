package dev.spark.intelligence.tier2

import dev.spark.domain.*
import kotlinx.datetime.*

/**
 * Detects weekly and daily usage trends. Flags days with usage more than 40% above the
 * user's personal average.
 */
class TrendDetector {

    companion object {
        /** A day is flagged as a spike if it exceeds the average by this proportion. */
        const val SPIKE_THRESHOLD = 0.40

        /** Minimum days with data before trend detection is considered reliable. */
        const val MIN_DAYS_FOR_TREND = 5
    }

    data class DailyUsageSummary(
        val date: LocalDate,
        val totalMinutes: Long,
        val emptyCalorieMinutes: Long,
        val nutritiveMinutes: Long,
        val neutralMinutes: Long,
        val sessionCount: Int
    )

    data class TrendResult(
        val dayOfWeek: DayOfWeek,
        val averageMinutes: Double,
        val isSpike: Boolean,
        val percentAboveAverage: Double
    )

    data class WeeklyTrendSummary(
        val dailySummaries: List<DailyUsageSummary>,
        val overallAverage: Double,
        val spikeDays: List<DailyUsageSummary>,
        val dayOfWeekTrends: Map<DayOfWeek, TrendResult>,
        val weekOverWeekChange: Double?,  // null if no prior week data
        val longestStreak: Int,           // consecutive days under average
        val busiestDayOfWeek: DayOfWeek?
    )

    /**
     * Aggregates [UsageSession] records by date.
     */
    fun buildDailySummaries(
        sessions: List<UsageSession>,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<DailyUsageSummary> {
        return sessions
            .groupBy { it.startTime.toLocalDateTime(timeZone).date }
            .map { (date, daySessions) ->
                DailyUsageSummary(
                    date = date,
                    totalMinutes = daySessions.sumOf { it.durationSeconds } / 60L,
                    emptyCalorieMinutes = daySessions
                        .filter { it.category == AppCategory.EMPTY_CALORIES }
                        .sumOf { it.durationSeconds } / 60L,
                    nutritiveMinutes = daySessions
                        .filter { it.category == AppCategory.NUTRITIVE }
                        .sumOf { it.durationSeconds } / 60L,
                    neutralMinutes = daySessions
                        .filter { it.category == AppCategory.NEUTRAL }
                        .sumOf { it.durationSeconds } / 60L,
                    sessionCount = daySessions.size
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Detects spike days — days where total usage exceeds the personal average by >[SPIKE_THRESHOLD].
     */
    fun detectSpikeDays(
        dailySummaries: List<DailyUsageSummary>
    ): List<DailyUsageSummary> {
        if (dailySummaries.size < MIN_DAYS_FOR_TREND) return emptyList()

        val average = dailySummaries.map { it.totalMinutes }.average()
        val threshold = average * (1 + SPIKE_THRESHOLD)

        return dailySummaries.filter { it.totalMinutes > threshold }
    }

    /**
     * Computes per-day-of-week averages and flags days that consistently run high.
     */
    fun analyzeDayOfWeekTrends(
        dailySummaries: List<DailyUsageSummary>
    ): Map<DayOfWeek, TrendResult> {
        if (dailySummaries.isEmpty()) return emptyMap()

        val overallAverage = dailySummaries.map { it.totalMinutes }.average()

        return dailySummaries
            .groupBy { it.date.dayOfWeek }
            .mapValues { (dow, entries) ->
                val avg = entries.map { it.totalMinutes }.average()
                val pctAbove = if (overallAverage > 0) (avg - overallAverage) / overallAverage else 0.0
                TrendResult(
                    dayOfWeek = dow,
                    averageMinutes = avg,
                    isSpike = pctAbove > SPIKE_THRESHOLD,
                    percentAboveAverage = pctAbove
                )
            }
    }

    /**
     * Counts the longest streak of consecutive days where usage stayed at or below average.
     */
    fun computeUnderAverageStreak(dailySummaries: List<DailyUsageSummary>): Int {
        if (dailySummaries.isEmpty()) return 0
        val average = dailySummaries.map { it.totalMinutes }.average()
        var maxStreak = 0
        var currentStreak = 0
        for (summary in dailySummaries) {
            if (summary.totalMinutes <= average) {
                currentStreak++
                if (currentStreak > maxStreak) maxStreak = currentStreak
            } else {
                currentStreak = 0
            }
        }
        return maxStreak
    }

    /**
     * Computes the week-over-week change in empty-calorie usage as a percentage.
     * Returns null if the prior week has no data.
     */
    fun computeWeekOverWeekChange(
        currentWeek: List<DailyUsageSummary>,
        priorWeek: List<DailyUsageSummary>
    ): Double? {
        if (priorWeek.isEmpty()) return null
        val prior = priorWeek.sumOf { it.emptyCalorieMinutes }.toDouble()
        if (prior == 0.0) return null
        val current = currentWeek.sumOf { it.emptyCalorieMinutes }.toDouble()
        return (current - prior) / prior
    }

    /**
     * Full weekly trend analysis.
     */
    fun analyzeWeek(
        currentWeekSessions: List<UsageSession>,
        priorWeekSessions: List<UsageSession> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): WeeklyTrendSummary {
        val dailySummaries = buildDailySummaries(currentWeekSessions, timeZone)
        val priorSummaries = buildDailySummaries(priorWeekSessions, timeZone)

        val overallAverage = if (dailySummaries.isNotEmpty())
            dailySummaries.map { it.totalMinutes }.average() else 0.0

        val spikeDays = detectSpikeDays(dailySummaries)
        val dowTrends = analyzeDayOfWeekTrends(dailySummaries)
        val streak = computeUnderAverageStreak(dailySummaries)
        val weekChange = computeWeekOverWeekChange(dailySummaries, priorSummaries)
        val busiestDay = dowTrends.entries
            .maxByOrNull { it.value.averageMinutes }
            ?.key

        return WeeklyTrendSummary(
            dailySummaries = dailySummaries,
            overallAverage = overallAverage,
            spikeDays = spikeDays,
            dayOfWeekTrends = dowTrends,
            weekOverWeekChange = weekChange,
            longestStreak = streak,
            busiestDayOfWeek = busiestDay
        )
    }
}
