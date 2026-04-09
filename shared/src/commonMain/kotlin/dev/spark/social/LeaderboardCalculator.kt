package dev.spark.social

import dev.spark.domain.*
import kotlinx.datetime.*

/**
 * Computes circle-scoped leaderboards across multiple ranking categories.
 *
 * Leaderboards are computed on-demand from raw user stats — they are not
 * persisted, to ensure privacy (raw data never leaves the device; only
 * the user's own stats are uploaded to compare).
 */
class LeaderboardCalculator {

    // -------------------------------------------------------------------------
    // Domain models
    // -------------------------------------------------------------------------

    enum class LeaderboardCategory {
        /** Highest FP balance today. */
        FP_BALANCE,
        /** Most nutritive minutes this week. */
        NUTRITIVE_MINUTES,
        /** Fewest empty-calorie minutes this week (lower = better). */
        FEWEST_EMPTY_CALORIES,
        /** Longest current streak (consecutive days with nutritive activity). */
        STREAK_DAYS,
        /** Best intent accuracy (% of declared intents honored). */
        INTENT_ACCURACY,
        /** Most FP earned this week (before spending). */
        FP_EARNED_WEEKLY
    }

    data class UserStats(
        val userId: String,
        val displayName: String,       // anonymized or real name per user's privacy setting
        val fpBalance: Int,
        val nutritiveMinutes: Int,     // this week
        val emptyCalorieMinutes: Int,  // this week
        val streakDays: Int,
        val intentAccuracyPercent: Float,
        val fpEarnedWeekly: Int
    )

    data class LeaderboardEntry(
        val rank: Int,
        val userId: String,
        val displayName: String,
        val value: Double,             // the raw value for the ranked metric
        val valueLabel: String,        // human-readable, e.g. "312 FP" or "87%"
        val isCurrentUser: Boolean
    )

    data class Leaderboard(
        val category: LeaderboardCategory,
        val circleId: String,
        val entries: List<LeaderboardEntry>,
        val currentUserRank: Int?,
        val computedAt: Instant
    )

    // -------------------------------------------------------------------------
    // Computation
    // -------------------------------------------------------------------------

    /**
     * Computes a leaderboard for [circleId] in the given [category].
     *
     * @param memberStats Stats for all members in the circle.
     * @param currentUserId The local user's ID (used to flag [LeaderboardEntry.isCurrentUser]).
     * @param clock Used to timestamp the result.
     */
    fun compute(
        circleId: String,
        category: LeaderboardCategory,
        memberStats: List<UserStats>,
        currentUserId: String,
        clock: Clock = Clock.System
    ): Leaderboard {
        val sorted = when (category) {
            LeaderboardCategory.FP_BALANCE ->
                memberStats.sortedByDescending { it.fpBalance }
            LeaderboardCategory.NUTRITIVE_MINUTES ->
                memberStats.sortedByDescending { it.nutritiveMinutes }
            LeaderboardCategory.FEWEST_EMPTY_CALORIES ->
                memberStats.sortedBy { it.emptyCalorieMinutes }  // ascending — lower is better
            LeaderboardCategory.STREAK_DAYS ->
                memberStats.sortedByDescending { it.streakDays }
            LeaderboardCategory.INTENT_ACCURACY ->
                memberStats.sortedByDescending { it.intentAccuracyPercent }
            LeaderboardCategory.FP_EARNED_WEEKLY ->
                memberStats.sortedByDescending { it.fpEarnedWeekly }
        }

        val entries = sorted.mapIndexed { index, stats ->
            val (value, label) = extractValueAndLabel(stats, category)
            LeaderboardEntry(
                rank = index + 1,
                userId = stats.userId,
                displayName = stats.displayName,
                value = value,
                valueLabel = label,
                isCurrentUser = stats.userId == currentUserId
            )
        }

        val currentUserRank = entries.find { it.userId == currentUserId }?.rank

        return Leaderboard(
            category = category,
            circleId = circleId,
            entries = entries,
            currentUserRank = currentUserRank,
            computedAt = clock.now()
        )
    }

    /**
     * Computes leaderboards for all categories simultaneously.
     */
    fun computeAll(
        circleId: String,
        memberStats: List<UserStats>,
        currentUserId: String,
        clock: Clock = Clock.System
    ): Map<LeaderboardCategory, Leaderboard> {
        return LeaderboardCategory.entries.associateWith { category ->
            compute(circleId, category, memberStats, currentUserId, clock)
        }
    }

    // -------------------------------------------------------------------------
    // Leaderboard insights
    // -------------------------------------------------------------------------

    /**
     * Returns a human-readable summary of the current user's standing across all categories.
     */
    fun summarizeStanding(
        allLeaderboards: Map<LeaderboardCategory, Leaderboard>,
        currentUserId: String
    ): String {
        val totalMembers = allLeaderboards.values.firstOrNull()?.entries?.size ?: 0
        if (totalMembers == 0) return "No data available yet."

        val standings = allLeaderboards.mapNotNull { (category, board) ->
            val rank = board.entries.find { it.userId == currentUserId }?.rank ?: return@mapNotNull null
            category to rank
        }

        val bestCategory = standings.minByOrNull { it.second }
        val avgRank = if (standings.isNotEmpty()) standings.map { it.second }.average() else 0.0

        return buildString {
            append("You're ranked #${bestCategory?.second ?: "-"} in ${bestCategory?.first?.displayName() ?: "N/A"}")
            if (totalMembers > 1) append(" (out of $totalMembers members)")
            append(". Average rank: ${avgRank.toInt()}.")
        }
    }

    /**
     * Returns the top N entries from a leaderboard (e.g. for a podium widget).
     */
    fun topN(leaderboard: Leaderboard, n: Int = 3): List<LeaderboardEntry> =
        leaderboard.entries.take(n)

    /**
     * Returns the context window around the current user's rank (e.g. ±2 places).
     */
    fun userContext(
        leaderboard: Leaderboard,
        currentUserId: String,
        windowSize: Int = 2
    ): List<LeaderboardEntry> {
        val userRank = leaderboard.entries.indexOfFirst { it.userId == currentUserId }
        if (userRank == -1) return emptyList()

        val from = maxOf(0, userRank - windowSize)
        val to = minOf(leaderboard.entries.size, userRank + windowSize + 1)
        return leaderboard.entries.subList(from, to)
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun extractValueAndLabel(
        stats: UserStats,
        category: LeaderboardCategory
    ): Pair<Double, String> = when (category) {
        LeaderboardCategory.FP_BALANCE ->
            stats.fpBalance.toDouble() to "${stats.fpBalance} FP"
        LeaderboardCategory.NUTRITIVE_MINUTES ->
            stats.nutritiveMinutes.toDouble() to "${stats.nutritiveMinutes} min"
        LeaderboardCategory.FEWEST_EMPTY_CALORIES ->
            stats.emptyCalorieMinutes.toDouble() to "${stats.emptyCalorieMinutes} min"
        LeaderboardCategory.STREAK_DAYS ->
            stats.streakDays.toDouble() to "${stats.streakDays} days"
        LeaderboardCategory.INTENT_ACCURACY ->
            stats.intentAccuracyPercent.toDouble() to "${(stats.intentAccuracyPercent * 100).toInt()}%"
        LeaderboardCategory.FP_EARNED_WEEKLY ->
            stats.fpEarnedWeekly.toDouble() to "${stats.fpEarnedWeekly} FP"
    }

    private fun LeaderboardCategory.displayName(): String = when (this) {
        LeaderboardCategory.FP_BALANCE -> "FP Balance"
        LeaderboardCategory.NUTRITIVE_MINUTES -> "Nutritive Time"
        LeaderboardCategory.FEWEST_EMPTY_CALORIES -> "Least Scrolling"
        LeaderboardCategory.STREAK_DAYS -> "Streak"
        LeaderboardCategory.INTENT_ACCURACY -> "Intent Accuracy"
        LeaderboardCategory.FP_EARNED_WEEKLY -> "FP Earned This Week"
    }
}
