package dev.spark.social

import dev.spark.domain.*
import kotlinx.datetime.*

/**
 * Manages focus challenges within circles and between buddy pairs.
 *
 * Supports:
 *  - Creating challenges with configurable goals and durations.
 *  - Tracking participant progress.
 *  - Determining winners / completions.
 */
class ChallengeEngine {

    // -------------------------------------------------------------------------
    // Domain models
    // -------------------------------------------------------------------------

    enum class ChallengeType {
        /** Reduce total empty-calorie minutes below a target. */
        REDUCE_EMPTY_CALORIES,
        /** Accumulate at least N nutritive minutes. */
        EARN_NUTRITIVE_MINUTES,
        /** Reach a target FP balance. */
        REACH_FP_BALANCE,
        /** Maintain a FP balance streak (consecutive days above threshold). */
        DAILY_STREAK,
        /** Collectively earn N FP as a group. */
        GROUP_FP_POOL,
        /** Complete a given number of analog activities. */
        ANALOG_COMPLETIONS
    }

    enum class ChallengeScope { CIRCLE, BUDDY_PAIR }

    enum class ChallengeStatus { UPCOMING, ACTIVE, COMPLETED, CANCELLED }

    data class Challenge(
        val challengeId: String,
        val title: String,
        val description: String,
        val type: ChallengeType,
        val scope: ChallengeScope,
        val scopeId: String,            // circleId or pairId
        val createdByUserId: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
        val targetValue: Int,           // minutes, FP, days, or count depending on type
        val status: ChallengeStatus,
        val createdAt: Instant,
        val isTeamChallenge: Boolean    // if true, all members share a single progress pool
    )

    data class ChallengeParticipant(
        val challengeId: String,
        val userId: String,
        val joinedAt: Instant,
        val currentProgress: Int,        // accumulated value toward targetValue
        val hasCompleted: Boolean = false,
        val completedAt: Instant? = null
    )

    data class ChallengeResult(
        val challengeId: String,
        val winners: List<String>,       // userIds who completed the challenge
        val participantCount: Int,
        val finalProgress: Map<String, Int>,  // userId → final progress value
        val completedAt: Instant
    )

    // In-memory state
    private val challenges = mutableMapOf<String, Challenge>()
    private val participants = mutableListOf<ChallengeParticipant>()

    // -------------------------------------------------------------------------
    // Challenge lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates a new challenge. The creator is automatically added as a participant.
     */
    fun createChallenge(
        title: String,
        description: String = "",
        type: ChallengeType,
        scope: ChallengeScope,
        scopeId: String,
        createdByUserId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        targetValue: Int,
        isTeamChallenge: Boolean = false,
        clock: Clock = Clock.System
    ): Challenge {
        require(title.isNotBlank()) { "Challenge title must not be blank." }
        require(endDate >= startDate) { "End date must be on or after start date." }
        require(targetValue > 0) { "Target value must be positive." }

        val now = clock.now()
        val today = now.toLocalDateTime(TimeZone.currentSystemDefault()).date
        val status = when {
            startDate > today -> ChallengeStatus.UPCOMING
            else -> ChallengeStatus.ACTIVE
        }

        val challenge = Challenge(
            challengeId = generateId(),
            title = title.trim(),
            description = description.trim(),
            type = type,
            scope = scope,
            scopeId = scopeId,
            createdByUserId = createdByUserId,
            startDate = startDate,
            endDate = endDate,
            targetValue = targetValue,
            status = status,
            createdAt = now,
            isTeamChallenge = isTeamChallenge
        )
        challenges[challenge.challengeId] = challenge

        // Auto-enroll creator
        participants += ChallengeParticipant(
            challengeId = challenge.challengeId,
            userId = createdByUserId,
            joinedAt = now,
            currentProgress = 0
        )
        return challenge
    }

    /**
     * Joins a challenge as a participant.
     *
     * @throws IllegalStateException if the challenge is not ACTIVE or UPCOMING.
     */
    fun joinChallenge(challengeId: String, userId: String, clock: Clock = Clock.System): ChallengeParticipant {
        val challenge = getChallenge(challengeId)
        check(challenge.status == ChallengeStatus.ACTIVE || challenge.status == ChallengeStatus.UPCOMING) {
            "Cannot join a challenge that is ${challenge.status}."
        }
        // Idempotent — return existing if already joined
        getParticipant(challengeId, userId)?.let { return it }

        val participant = ChallengeParticipant(
            challengeId = challengeId,
            userId = userId,
            joinedAt = clock.now(),
            currentProgress = 0
        )
        participants += participant
        return participant
    }

    /**
     * Updates a participant's progress for a challenge.
     * Marks the participant as completed if [ChallengeParticipant.currentProgress] ≥ target.
     * For team challenges, also checks whether the group has collectively reached the goal.
     *
     * @param progressDelta The amount to add to the current progress (e.g. minutes, FP, count).
     * @return The updated [ChallengeParticipant].
     */
    fun recordProgress(
        challengeId: String,
        userId: String,
        progressDelta: Int,
        clock: Clock = Clock.System
    ): ChallengeParticipant {
        val challenge = getChallenge(challengeId)
        check(challenge.status == ChallengeStatus.ACTIVE) { "Challenge is not active." }

        val idx = participants.indexOfFirst { it.challengeId == challengeId && it.userId == userId }
        require(idx != -1) { "User $userId is not a participant in challenge $challengeId." }

        val p = participants[idx]
        val newProgress = p.currentProgress + progressDelta

        val isComplete = if (challenge.isTeamChallenge) {
            val totalGroupProgress = participants
                .filter { it.challengeId == challengeId }
                .sumOf { it.currentProgress } + progressDelta
            totalGroupProgress >= challenge.targetValue
        } else {
            newProgress >= challenge.targetValue
        }

        val updated = p.copy(
            currentProgress = newProgress,
            hasCompleted = isComplete,
            completedAt = if (isComplete) clock.now() else null
        )
        participants[idx] = updated
        return updated
    }

    /**
     * Finalizes a challenge (typically called at end of [Challenge.endDate]).
     * Determines winners and marks the challenge as COMPLETED.
     */
    fun finalizeChallenge(challengeId: String, clock: Clock = Clock.System): ChallengeResult {
        val challenge = getChallenge(challengeId)
        check(challenge.status == ChallengeStatus.ACTIVE) {
            "Challenge is not active (status: ${challenge.status})."
        }

        val challengeParticipants = participants.filter { it.challengeId == challengeId }
        val now = clock.now()

        val winners: List<String>
        val finalProgress: Map<String, Int>

        if (challenge.isTeamChallenge) {
            val totalProgress = challengeParticipants.sumOf { it.currentProgress }
            finalProgress = challengeParticipants.associate { it.userId to it.currentProgress }
            winners = if (totalProgress >= challenge.targetValue) {
                challengeParticipants.map { it.userId }
            } else emptyList()
        } else {
            finalProgress = challengeParticipants.associate { it.userId to it.currentProgress }
            winners = when (challenge.type) {
                // For competitive challenges: winner has the best absolute progress
                ChallengeType.REDUCE_EMPTY_CALORIES -> {
                    // Lower is better
                    val minProgress = challengeParticipants.minOfOrNull { it.currentProgress }
                    challengeParticipants
                        .filter { it.currentProgress == minProgress }
                        .map { it.userId }
                }
                else -> {
                    // Higher is better — all who reached the target are winners
                    challengeParticipants
                        .filter { it.currentProgress >= challenge.targetValue }
                        .map { it.userId }
                }
            }
        }

        challenges[challengeId] = challenge.copy(status = ChallengeStatus.COMPLETED)

        return ChallengeResult(
            challengeId = challengeId,
            winners = winners,
            participantCount = challengeParticipants.size,
            finalProgress = finalProgress,
            completedAt = now
        )
    }

    /**
     * Cancels a challenge.
     */
    fun cancelChallenge(challengeId: String, requestingUserId: String): Boolean {
        val challenge = getChallenge(challengeId)
        require(challenge.createdByUserId == requestingUserId) {
            "Only the challenge creator can cancel it."
        }
        if (challenge.status == ChallengeStatus.COMPLETED) return false
        challenges[challengeId] = challenge.copy(status = ChallengeStatus.CANCELLED)
        return true
    }

    // -------------------------------------------------------------------------
    // Query
    // -------------------------------------------------------------------------

    /**
     * Returns all challenges for a given scope (circle or buddy pair).
     */
    fun getChallengesForScope(scopeId: String): List<Challenge> =
        challenges.values.filter { it.scopeId == scopeId }

    /**
     * Returns active challenges for a scope.
     */
    fun getActiveChallengesForScope(scopeId: String): List<Challenge> =
        getChallengesForScope(scopeId).filter { it.status == ChallengeStatus.ACTIVE }

    /**
     * Returns all challenges a user has joined.
     */
    fun getChallengesForUser(userId: String): List<Challenge> {
        val challengeIds = participants
            .filter { it.userId == userId }
            .map { it.challengeId }
            .toSet()
        return challenges.values.filter { it.challengeId in challengeIds }
    }

    /**
     * Returns participants for a challenge, sorted by progress (descending).
     */
    fun getLeaderboard(challengeId: String): List<ChallengeParticipant> {
        val challenge = getChallenge(challengeId)
        val list = participants.filter { it.challengeId == challengeId }
        return if (challenge.type == ChallengeType.REDUCE_EMPTY_CALORIES) {
            list.sortedBy { it.currentProgress }  // lower is better
        } else {
            list.sortedByDescending { it.currentProgress }
        }
    }

    /**
     * Returns the progress percentage (0–100) for a participant.
     */
    fun progressPercent(challengeId: String, userId: String): Int {
        val challenge = getChallenge(challengeId)
        val participant = getParticipant(challengeId, userId) ?: return 0
        return ((participant.currentProgress.toFloat() / challenge.targetValue) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun getChallenge(challengeId: String): Challenge =
        challenges[challengeId] ?: throw IllegalArgumentException("Challenge $challengeId not found.")

    private fun getParticipant(challengeId: String, userId: String): ChallengeParticipant? =
        participants.find { it.challengeId == challengeId && it.userId == userId }

    private var idCounter = 0L
    private fun generateId(): String = "ch_${++idCounter}_${Clock.System.now().epochSeconds}"
}
