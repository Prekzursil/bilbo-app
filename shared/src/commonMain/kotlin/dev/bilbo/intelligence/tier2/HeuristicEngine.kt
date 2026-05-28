package dev.bilbo.intelligence.tier2

import dev.bilbo.domain.Emotion
import dev.bilbo.domain.EmotionalCheckIn
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.InsightType
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.domain.UsageSession
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Tier-2 batch analysis engine. Runs offline (on-device) after each week to generate
 * [HeuristicInsight] objects covering emotion-usage correlations, day-of-week trends,
 * and intent accuracy.
 */
class HeuristicEngine(
    private val correlationAnalyzer: CorrelationAnalyzer = CorrelationAnalyzer(),
    private val trendDetector: TrendDetector = TrendDetector(),
) {
    companion object {
        private const val INTENT_ACCURACY_POOR_THRESHOLD = 0.50f
        private const val INTENT_ACCURACY_GOOD_THRESHOLD = 0.80f
        private const val STRONG_CORRELATION_THRESHOLD = 0.60
        private const val INSIGHT_CONFIDENCE_HIGH = 0.9f
        private const val INSIGHT_CONFIDENCE_MEDIUM = 0.65f

        private const val PERCENT = 100
        private const val ACCURATE_DELTA = 0.20
        private const val WEEK_OVER_WEEK_DROP = -0.10
        private const val WEEK_OVER_WEEK_RISE = 0.20
        private const val OVERRIDE_RATE_THRESHOLD = 0.30f
        private const val STREAK_THRESHOLD = 3
        private const val LATE_NIGHT_END_HOUR = 4
        private const val SECONDS_PER_MINUTE = 60L
        private const val STRESSED_CHECKIN_THRESHOLD = 3
    }

    /**
     * Full week analysis. Returns a list of [HeuristicInsight] ordered by
     * confidence (descending), covering emotion correlations, day-of-week
     * trends, intent accuracy and anomalies for the supplied [sessions],
     * [checkIns] and [intents]. [priorWeekSessions] enables trend comparison.
     */
    fun analyzeWeek(
        sessions: List<UsageSession>,
        checkIns: List<EmotionalCheckIn>,
        intents: List<IntentDeclaration>,
        priorWeekSessions: List<UsageSession> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
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
        sessions: List<UsageSession>,
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()

        val emptyCorrelations = correlationAnalyzer.analyzeEmotionToEmptyCalorieUsage(checkIns, sessions)
        val nutritiveCorrelations = correlationAnalyzer.analyzeEmotionToNutritiveUsage(checkIns, sessions)

        emptyCorrelations.forEach { (emotion, strength) ->
            if (strength >= STRONG_CORRELATION_THRESHOLD) {
                insights +=
                    HeuristicInsight(
                        type = InsightType.CORRELATION,
                        message = buildEmotionCorrelationMessage(emotion, strength, isEmptyCalorie = true),
                        confidence = (strength * INSIGHT_CONFIDENCE_HIGH).toFloat().coerceIn(0f, 1f),
                    )
            }
        }

        nutritiveCorrelations.forEach { (emotion, strength) ->
            if (strength >= STRONG_CORRELATION_THRESHOLD) {
                insights +=
                    HeuristicInsight(
                        type = InsightType.CORRELATION,
                        message = buildEmotionCorrelationMessage(emotion, strength, isEmptyCalorie = false),
                        confidence = (strength * INSIGHT_CONFIDENCE_MEDIUM).toFloat().coerceIn(0f, 1f),
                    )
            }
        }

        return insights
    }

    private fun buildEmotionCorrelationMessage(
        emotion: Emotion,
        strength: Double,
        isEmptyCalorie: Boolean,
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
        timeZone: TimeZone,
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()
        val summary = trendDetector.analyzeWeek(sessions, priorWeekSessions, timeZone)

        // Spike days
        summary.spikeDays.forEach { day ->
            val dayLabel = day.date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            val abovePct =
                ((day.totalMinutes - summary.overallAverage) / summary.overallAverage * PERCENT).toInt()
            insights +=
                HeuristicInsight(
                    type = InsightType.ANOMALY,
                    message =
                        "$dayLabel was a high-usage day — ${day.totalMinutes} min total, " +
                            "$abovePct% above your weekly average.",
                    confidence = INSIGHT_CONFIDENCE_HIGH,
                )
        }

        // Busiest day-of-week pattern
        summary.busiestDayOfWeek?.let { dow ->
            val dowLabel = dow.name.lowercase().replaceFirstChar { it.uppercase() }
            insights +=
                HeuristicInsight(
                    type = InsightType.TREND,
                    message =
                        "${dowLabel}s tend to be your highest-usage day. " +
                            "Consider scheduling a focus block.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM,
                )
        }

        // Week-over-week improvement
        summary.weekOverWeekChange?.let { change ->
            if (change < WEEK_OVER_WEEK_DROP) {
                val pct = (-change * PERCENT).toInt()
                insights +=
                    HeuristicInsight(
                        type = InsightType.ACHIEVEMENT,
                        message = "Great week! Your scrolling time dropped $pct% compared to last week.",
                        confidence = INSIGHT_CONFIDENCE_HIGH,
                    )
            } else if (change > WEEK_OVER_WEEK_RISE) {
                val pct = (change * PERCENT).toInt()
                insights +=
                    HeuristicInsight(
                        type = InsightType.TREND,
                        message = "Heads up: scrolling time was up $pct% vs. last week.",
                        confidence = INSIGHT_CONFIDENCE_MEDIUM,
                    )
            }
        }

        // Streak
        if (summary.longestStreak >= STREAK_THRESHOLD) {
            insights +=
                HeuristicInsight(
                    type = InsightType.ACHIEVEMENT,
                    message =
                        "You stayed under your daily average for " +
                            "${summary.longestStreak} days in a row this week.",
                    confidence = INSIGHT_CONFIDENCE_HIGH,
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

        val accurate =
            completed.count { intent ->
                val declared = intent.declaredDurationMinutes
                val actual = intent.actualDurationMinutes ?: return@count false
                // "Accurate" = actual within 20% of declared
                val delta = kotlin.math.abs(actual - declared).toDouble() / declared
                delta <= ACCURATE_DELTA
            }

        val accuracy = accurate.toFloat() / completed.size.toFloat()

        when {
            accuracy >= INTENT_ACCURACY_GOOD_THRESHOLD -> {
                val accuracyPct = (accuracy * PERCENT).toInt()
                insights +=
                    HeuristicInsight(
                        type = InsightType.ACHIEVEMENT,
                        message =
                            "You stuck to your declared session times $accuracyPct% of the time " +
                                "this week. Excellent self-awareness!",
                        confidence = INSIGHT_CONFIDENCE_HIGH,
                    )
            }
            accuracy < INTENT_ACCURACY_POOR_THRESHOLD -> {
                insights +=
                    HeuristicInsight(
                        type = InsightType.TREND,
                        message =
                            "Your actual session times often exceeded what you planned. " +
                                "Try setting slightly shorter intentions.",
                        confidence = INSIGHT_CONFIDENCE_MEDIUM,
                    )
            }
        }

        // Override analysis
        val overrideRate = intents.count { it.wasOverridden }.toFloat() / intents.size.toFloat()
        if (overrideRate > OVERRIDE_RATE_THRESHOLD) {
            val overridePct = (overrideRate * PERCENT).toInt()
            insights +=
                HeuristicInsight(
                    type = InsightType.TREND,
                    message =
                        "You overrode Bilbo's time limits $overridePct% of the time. " +
                            "Consider switching to Nudge mode for a gentler approach.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM,
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
        timeZone: TimeZone,
    ): List<HeuristicInsight> {
        val insights = mutableListOf<HeuristicInsight>()

        // Late-night usage anomaly (sessions between midnight and 5 AM)
        val lateNightSessions =
            sessions.filter { session ->
                val hour = session.startTime.toLocalDateTime(timeZone).hour
                hour in 0..LATE_NIGHT_END_HOUR
            }
        if (lateNightSessions.isNotEmpty()) {
            val totalMinutes = lateNightSessions.sumOf { it.durationSeconds } / SECONDS_PER_MINUTE
            insights +=
                HeuristicInsight(
                    type = InsightType.ANOMALY,
                    message =
                        "You used your phone for $totalMinutes min between midnight and 5 AM " +
                            "this week. Late-night scrolling can disrupt sleep.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM,
                )
        }

        // Post-stress binge: stressed/anxious check-in followed by long empty-calorie session
        val stressedCheckIns =
            checkIns.filter {
                it.preSessionEmotion == Emotion.STRESSED || it.preSessionEmotion == Emotion.ANXIOUS
            }
        if (stressedCheckIns.size >= STRESSED_CHECKIN_THRESHOLD) {
            insights +=
                HeuristicInsight(
                    type = InsightType.CORRELATION,
                    message =
                        "You checked in feeling stressed or anxious ${stressedCheckIns.size} times " +
                            "this week. Consider a mindfulness suggestion next time.",
                    confidence = INSIGHT_CONFIDENCE_MEDIUM,
                )
        }

        // Achievement: zero empty-calorie sessions on any day
        val dailySummaries = trendDetector.buildDailySummaries(sessions, timeZone)
        val zeroDays = dailySummaries.count { it.emptyCalorieMinutes == 0L }
        if (zeroDays >= 1) {
            val dayWord = if (zeroDays > 1) "s" else ""
            insights +=
                HeuristicInsight(
                    type = InsightType.ACHIEVEMENT,
                    message =
                        "You had $zeroDays day$dayWord this week with zero scrolling app usage. " +
                            "Keep it up!",
                    confidence = INSIGHT_CONFIDENCE_HIGH,
                )
        }

        return insights
    }
}
