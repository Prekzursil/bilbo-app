package dev.spark.intelligence.tier2

import dev.spark.domain.*
import kotlinx.datetime.*

/**
 * Tier-2 batch analysis engine. Runs offline (on-device) after each week to generate
 * [HeuristicInsight] objects covering emotion-usage correlations, day-of-week trends,
 * and intent accuracy.
 */
class HeuristicEngine(
    private val correlationAnalyzer: CorrelationAnalyzer = CorrelationAnalyzer(),
    private val trendDetector: TrendDetector = TrendDetector(),
    private val gamingDetector: GamingDetector = GamingDetector()
) {

    companion object {
        private const val INTENT_ACCURACY_POOR_THRESHOLD = 0.50f
        private const val INTENT_ACCURACY_GOOD_THRESHOLD = 0.80f
        private const val STRONG_CORRELATION_THRESHOLD = 0.60
        private const val INSIGHT_CONFIDENCE_HIGH = 0.9f
        private const val INSIGHT_CONFIDENCE_MEDIUM = 0.65f
        private const val INSIGHT_CONFIDENCE_LOW = 0.4f
    }

    /**
     * Full week analysis. Returns a list of [HeuristicInsight] ordered by confidence (descending).
     *
     * @param weekStart The Monday (or Sunday) of the week being analyzed.
     * @param sessions All usage sessions recorded during the week.
     * @param checkIns All emotional check-ins recorded during the week.
     * @param intents All intent declarations recorded during the week.
     * @param priorWeekSessions Sessions from the previous week (used for trend comparison).
     */
    fun analyzeWeek(
        weekStart: LocalDate,
        sessions: List<UsageSession>,
        checkIns: List<EmotionalCheckIn>,
        intents: List<IntentDeclaration>,
        priorWeekSessions: List<UsageSession> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()

        insights += analyzeEmotionCorrelations(checkIns, sessions)
        insights += analyzeDayOfWeekTrends(sessions, priorWeekSessions, timeZone)
        insights += analyzeIntentAccuracy(intents)
        insights += detectAnomalies(sessions, checkIns, timeZone)

        return insights.sortedByDescending { it.confidence }
    }

    // -------------------------------------------------------------------------
    // Emotion–Usage Correlation Analysis
    // -------------------------------------------------------------------------

    private fun analyzeEmotionCorrelations(
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()

        val emptyCorrelations = correlationAnalyzer.analyzeEmotionToEmptyCalorieUsage(checkIns, sessions)
        val nutritiveCorrelations = correlationAnalyzer.analyzeEmotionToNutritiveUsage(checkIns, sessions)

        emptyCorrelations.forEach { (emotion, strength) ->
            if (strength >= STRONG_CORRELATION_THRESHOLD) {
                insights += HeuristicInsight(
                    type = InsightType.CORRELATION,
                    message = buildEmotionCorrelationMessage(emotion, strength, isEmptyCalorie = true),
                    confidence = (strength * INSIGHT_CONFIDENCE_HIGH).toFloat().coerceIn(0f, 1f)
                )
            }
        }

        nutritiveCorrelations.forEach { (emotion, strength) ->
            if (strength >= STRONG_CORRELATION_THRESHOLD) {
                insights += HeuristicInsight(
                    type = InsightType.CORRELATION,
                    message = buildEmotionCorrelationMessage(emotion, strength, isEmptyCalorie = false),
                    confidence = (strength * INSIGHT_CONFIDENCE_MEDIUM).toFloat().coerceIn(0f, 1f)
                )
            }
        }

        return insights
    }

    private fun buildEmotionCorrelationMessage(
        emotion: Emotion,
        strength: Double,
        isEmptyCalorie: Boolean
    ): String {
        val emotionLabel = emotion.name.lowercase().replaceFirstChar { it.uppercase() }
        val usageType = if (isEmptyCalorie) "scrolling apps" else "productive apps"
        val strengthLabel = correlationAnalyzer.describeCorrelation(strength)
        return "When you feel $emotionLabel, there's a $strengthLabel correlation with using $usageType more."
    }

    // -------------------------------------------------------------------------
    // Day-of-Week & Weekly Trend Analysis
    // -------------------------------------------------------------------------

    private fun analyzeDayOfWeekTrends(
        sessions: List<UsageSession>,
        priorWeekSessions: List<UsageSession>,
        timeZone: TimeZone
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()
        val summary = trendDetector.analyzeWeek(sessions, priorWeekSessions, timeZone)

        // Spike days
        summary.spikeDays.forEach { day ->
            insights += HeuristicInsight(
                type = InsightType.ANOMALY,
                message = "${day.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }} was a " +
                    "high-usage day — ${day.totalMinutes} min total, " +
                    "${((day.totalMinutes - summary.overallAverage) / summary.overallAverage * 100).toInt()}% " +
                    "above your weekly average.",
                confidence = INSIGHT_CONFIDENCE_HIGH
            )
        }

        // Busiest day-of-week pattern
        summary.busiestDayOfWeek?.let { dow ->
            insights += HeuristicInsight(
                type = InsightType.TREND,
                message = "${dow.name.lowercase().replaceFirstChar { it.uppercase() }}s tend to be your highest-usage day. " +
                    "Consider scheduling a focus block.",
                confidence = INSIGHT_CONFIDENCE_MEDIUM
            )
        }

        // Week-over-week improvement
        summary.weekOverWeekChange?.let { change ->
            if (change < -0.10) {
                val pct = (-change * 100).toInt()
                insights += HeuristicInsight(
                    type = InsightType.ACHIEVEMENT,
                    message = "Great week! Your scrolling time dropped $pct% compared to last week.",
                    confidence = INSIGHT_CONFIDENCE_HIGH
                )
            } else if (change > 0.20) {
                val pct = (change * 100).toInt()
                insights += HeuristicInsight(
                    type = InsightType.TREND,
                    message = "Heads up: scrolling time was up $pct% vs. last week.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM
                )
            }
        }

        // Streak
        if (summary.longestStreak >= 3) {
            insights += HeuristicInsight(
                type = InsightType.ACHIEVEMENT,
                message = "You stayed under your daily average for ${summary.longestStreak} days in a row this week.",
                confidence = INSIGHT_CONFIDENCE_HIGH
            )
        }

        return insights
    }

    // -------------------------------------------------------------------------
    // Intent Accuracy Tracking
    // -------------------------------------------------------------------------

    private fun analyzeIntentAccuracy(intents: List<IntentDeclaration>): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()
        if (intents.isEmpty()) return insights

        val completed = intents.filter { it.actualDurationMinutes != null }
        if (completed.isEmpty()) return insights

        val accurate = completed.count { intent ->
            val declared = intent.declaredDurationMinutes
            val actual = intent.actualDurationMinutes ?: return@count false
            // "Accurate" = actual within 20% of declared
            val delta = kotlin.math.abs(actual - declared).toDouble() / declared
            delta <= 0.20
        }

        val accuracy = accurate.toFloat() / completed.size.toFloat()

        when {
            accuracy >= INTENT_ACCURACY_GOOD_THRESHOLD -> {
                insights += HeuristicInsight(
                    type = InsightType.ACHIEVEMENT,
                    message = "You stuck to your declared session times ${(accuracy * 100).toInt()}% of the time this week. Excellent self-awareness!",
                    confidence = INSIGHT_CONFIDENCE_HIGH
                )
            }
            accuracy < INTENT_ACCURACY_POOR_THRESHOLD -> {
                insights += HeuristicInsight(
                    type = InsightType.TREND,
                    message = "Your actual session times often exceeded what you planned. Try setting slightly shorter intentions.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM
                )
            }
        }

        // Override analysis
        val overrideRate = intents.count { it.wasOverridden }.toFloat() / intents.size.toFloat()
        if (overrideRate > 0.30f) {
            insights += HeuristicInsight(
                type = InsightType.TREND,
                message = "You overrode Spark's time limits ${(overrideRate * 100).toInt()}% of the time. " +
                    "Consider switching to Nudge mode for a gentler approach.",
                confidence = INSIGHT_CONFIDENCE_MEDIUM
            )
        }

        return insights
    }

    // -------------------------------------------------------------------------
    // Anomaly Detection
    // -------------------------------------------------------------------------

    private fun detectAnomalies(
        sessions: List<UsageSession>,
        checkIns: List<EmotionalCheckIn>,
        timeZone: TimeZone
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()

        // Late-night usage anomaly (sessions between midnight and 5 AM)
        val lateNightSessions = sessions.filter { session ->
            val hour = session.startTime.toLocalDateTime(timeZone).hour
            hour in 0..4
        }
        if (lateNightSessions.isNotEmpty()) {
            val totalMinutes = lateNightSessions.sumOf { it.durationSeconds } / 60L
            insights += HeuristicInsight(
                type = InsightType.ANOMALY,
                message = "You used your phone for $totalMinutes min between midnight and 5 AM this week. Late-night scrolling can disrupt sleep.",
                confidence = INSIGHT_CONFIDENCE_MEDIUM
            )
        }

        // Post-stress binge: stressed/anxious check-in followed by long empty-calorie session
        val stressedCheckIns = checkIns.filter {
            it.preSessionEmotion == Emotion.STRESSED || it.preSessionEmotion == Emotion.ANXIOUS
        }
        if (stressedCheckIns.size >= 3) {
            insights += HeuristicInsight(
                type = InsightType.CORRELATION,
                message = "You checked in feeling stressed or anxious ${stressedCheckIns.size} times this week. Consider a mindfulness suggestion next time.",
                confidence = INSIGHT_CONFIDENCE_MEDIUM
            )
        }

        // Achievement: zero empty-calorie sessions on any day
        val dailySummaries = trendDetector.buildDailySummaries(sessions, timeZone)
        val zeroDays = dailySummaries.count { it.emptyCalorieMinutes == 0L }
        if (zeroDays >= 1) {
            insights += HeuristicInsight(
                type = InsightType.ACHIEVEMENT,
                message = "You had $zeroDays day${if (zeroDays > 1) "s" else ""} this week with zero scrolling app usage. Keep it up!",
                confidence = INSIGHT_CONFIDENCE_HIGH
            )
        }

        return insights
    }
}
