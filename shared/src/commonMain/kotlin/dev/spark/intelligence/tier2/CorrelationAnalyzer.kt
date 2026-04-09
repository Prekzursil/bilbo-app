package dev.spark.intelligence.tier2

import dev.spark.domain.*
import kotlin.math.sqrt

/**
 * Computes Pearson correlation between emotional states and app usage patterns.
 * Requires a minimum of 14 data points before returning meaningful results.
 */
class CorrelationAnalyzer {

    companion object {
        const val MIN_DATA_POINTS = 14
        const val STRONG_CORRELATION_THRESHOLD = 0.6f
        const val MODERATE_CORRELATION_THRESHOLD = 0.3f
    }

    /**
     * Represents a paired data point: an emotion score and an associated usage value.
     */
    data class EmotionUsagePair(
        val emotionScore: Double,   // numeric encoding of Emotion enum
        val usageMinutes: Double
    )

    /**
     * Encodes an [Emotion] to a numeric scale for correlation computation.
     * Negative-valence emotions → lower scores; positive/calm → higher scores.
     */
    fun encodeEmotion(emotion: Emotion): Double = when (emotion) {
        Emotion.HAPPY  -> 6.0
        Emotion.CALM   -> 5.0
        Emotion.BORED  -> 3.0
        Emotion.LONELY -> 2.0
        Emotion.SAD    -> 2.0
        Emotion.ANXIOUS -> 1.0
        Emotion.STRESSED -> 0.0
    }

    /**
     * Computes the Pearson correlation coefficient between emotion scores and usage minutes.
     * Returns null if there are fewer than [MIN_DATA_POINTS] observations.
     *
     * @return correlation strength in range 0.0–1.0 (absolute value), or null if insufficient data.
     */
    fun computeCorrelation(pairs: List<EmotionUsagePair>): Double? {
        if (pairs.size < MIN_DATA_POINTS) return null

        val n = pairs.size.toDouble()
        val meanX = pairs.sumOf { it.emotionScore } / n
        val meanY = pairs.sumOf { it.usageMinutes } / n

        var numerator = 0.0
        var denomX = 0.0
        var denomY = 0.0

        for (pair in pairs) {
            val dx = pair.emotionScore - meanX
            val dy = pair.usageMinutes - meanY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }

        val denominator = sqrt(denomX * denomY)
        if (denominator == 0.0) return 0.0

        // Return absolute value so result is in [0.0, 1.0]
        return minOf(1.0, kotlin.math.abs(numerator / denominator))
    }

    /**
     * Analyzes correlations between each emotion and empty-calorie usage.
     * Returns a map of [Emotion] → correlation strength (0.0–1.0) for emotions
     * with sufficient data.
     */
    fun analyzeEmotionToEmptyCalorieUsage(
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>
    ): Map<Emotion, Double> {
        // Build a timeline map: hour-of-day → emotion
        val emotionBySession = buildEmotionSessionMap(checkIns, sessions)

        return Emotion.entries.mapNotNull { emotion ->
            val pairs = emotionBySession
                .filter { it.preEmotion == emotion }
                .map { pair ->
                    EmotionUsagePair(
                        emotionScore = encodeEmotion(emotion),
                        usageMinutes = pair.usageMinutes
                    )
                }
            val correlation = computeCorrelation(pairs) ?: return@mapNotNull null
            emotion to correlation
        }.toMap()
    }

    /**
     * Analyzes how each emotion correlates with nutritive usage minutes.
     */
    fun analyzeEmotionToNutritiveUsage(
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>
    ): Map<Emotion, Double> {
        val emotionBySession = buildNutritiveSessionMap(checkIns, sessions)

        return Emotion.entries.mapNotNull { emotion ->
            val pairs = emotionBySession
                .filter { it.preEmotion == emotion }
                .map { pair ->
                    EmotionUsagePair(
                        emotionScore = encodeEmotion(emotion),
                        usageMinutes = pair.usageMinutes
                    )
                }
            val correlation = computeCorrelation(pairs) ?: return@mapNotNull null
            emotion to correlation
        }.toMap()
    }

    /**
     * Returns a human-readable description of a correlation strength.
     */
    fun describeCorrelation(strength: Double): String = when {
        strength >= STRONG_CORRELATION_THRESHOLD -> "strong"
        strength >= MODERATE_CORRELATION_THRESHOLD -> "moderate"
        else -> "weak"
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private data class EmotionSessionPair(
        val preEmotion: Emotion,
        val usageMinutes: Double
    )

    private fun buildEmotionSessionMap(
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>
    ): List<EmotionSessionPair> {
        return checkIns.map { checkIn ->
            // Find sessions that started within 5 minutes after the check-in
            val windowStartEpoch = checkIn.timestamp.epochSeconds
            val windowEndEpoch = windowStartEpoch + 5 * 60L

            val totalEmptyMinutes = sessions
                .filter { s ->
                    s.category == AppCategory.EMPTY_CALORIES &&
                    s.startTime.epochSeconds >= windowStartEpoch &&
                    s.startTime.epochSeconds < windowEndEpoch
                }
                .sumOf { it.durationSeconds / 60.0 }

            EmotionSessionPair(checkIn.preSessionEmotion, totalEmptyMinutes)
        }
    }

    private fun buildNutritiveSessionMap(
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>
    ): List<EmotionSessionPair> {
        return checkIns.map { checkIn ->
            val windowStartEpoch = checkIn.timestamp.epochSeconds
            val windowEndEpoch = windowStartEpoch + 5 * 60L

            val totalNutritiveMinutes = sessions
                .filter { s ->
                    s.category == AppCategory.NUTRITIVE &&
                    s.startTime.epochSeconds >= windowStartEpoch &&
                    s.startTime.epochSeconds < windowEndEpoch
                }
                .sumOf { it.durationSeconds / 60.0 }

            EmotionSessionPair(checkIn.preSessionEmotion, totalNutritiveMinutes)
        }
    }
}
