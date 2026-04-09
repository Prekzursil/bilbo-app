package dev.spark.domain.social

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

/**
 * A time-boxed wellness challenge between two or more users.
 * Challenges track progress toward a shared or competing goal.
 */
data class Challenge(
    val id: Long = 0,
    val remoteId: String,
    val title: String,
    val description: String? = null,
    val type: ChallengeType,
    val mode: ChallengeMode,
    val status: ChallengeStatus,
    val goal: ChallengeGoal,
    val creatorId: String,
    val participantIds: List<String>,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val createdAt: Instant,
    val completedAt: Instant? = null,
    val winnerId: String? = null,
    val localUserProgress: Float = 0f,
    val leaderboard: List<ChallengeParticipantProgress> = emptyList()
)

/**
 * Defines what the challenge measures.
 */
enum class ChallengeType {
    /** Accumulate the most Focus Points over the challenge window. */
    MOST_FP_EARNED,

    /** Maintain the longest unbroken daily streak. */
    LONGEST_STREAK,

    /** Log the most nutritive-app minutes. */
    MOST_NUTRITIVE_MINUTES,

    /** Keep empty-calorie usage below a shared target. */
    LOWEST_EMPTY_CALORIE_MINUTES,

    /** Hit a target number of accepted analog suggestions. */
    MOST_ANALOG_ACCEPTED,

    /** Achieve the highest intent-accuracy percentage. */
    BEST_INTENT_ACCURACY,

    /** Complete a set number of breathing exercises. */
    BREATHING_EXERCISES_COUNT
}

/**
 * Whether participants compete against each other or cooperate toward a shared target.
 */
enum class ChallengeMode {
    /** The participant with the best result wins. */
    COMPETITIVE,

    /** All participants collectively must reach the combined goal. */
    COOPERATIVE
}

/**
 * Lifecycle state of a Challenge.
 */
enum class ChallengeStatus {
    /** Created but start date has not been reached. */
    PENDING,

    /** Active — start date passed, end date not yet reached. */
    ACTIVE,

    /** End date passed; winner/result is determined. */
    COMPLETED,

    /** Creator or all participants cancelled before completion. */
    CANCELLED
}

/**
 * Quantified target for a Challenge.
 *
 * @param targetValue The numeric threshold to reach (e.g. 500 FP, 300 minutes, 5 exercises).
 * @param unit Human-readable unit label for display (e.g. "FP", "minutes", "exercises").
 * @param perParticipant If true and mode is COOPERATIVE, targetValue is per-person;
 *        if false, targetValue is the aggregate group target.
 */
data class ChallengeGoal(
    val targetValue: Int,
    val unit: String,
    val perParticipant: Boolean = true
)

/**
 * Snapshot of a single participant's progress within a Challenge leaderboard.
 */
data class ChallengeParticipantProgress(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val currentValue: Float,
    val rank: Int,
    val lastUpdatedAt: Instant
)
