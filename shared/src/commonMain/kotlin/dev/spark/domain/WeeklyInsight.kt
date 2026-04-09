package dev.spark.domain

import kotlinx.datetime.LocalDate

data class WeeklyInsight(
    val id: Long = 0,
    val weekStart: LocalDate,
    val tier2Insights: List<HeuristicInsight>,
    val tier3Narrative: String? = null,
    val totalScreenTimeMinutes: Int,
    val nutritiveMinutes: Int,
    val emptyCalorieMinutes: Int,
    val fpEarned: Int,
    val fpSpent: Int,
    val intentAccuracyPercent: Float,
    val streakDays: Int
)

data class HeuristicInsight(
    val type: InsightType,
    val message: String,
    val confidence: Float
)

enum class InsightType {
    CORRELATION, TREND, ANOMALY, ACHIEVEMENT
}
