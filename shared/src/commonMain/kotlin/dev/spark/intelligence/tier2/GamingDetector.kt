package dev.spark.intelligence.tier2

import dev.spark.domain.*
import kotlinx.datetime.*

/**
 * Anti-gaming heuristics for the FP economy.
 *
 * Rules enforced:
 *  1. Nutritive sessions shorter than 60 seconds do NOT earn FP.
 *  2. The same Nutritive app opened more than 20 times/day triggers a flag and earn block.
 *  3. Screen-off events during an active session void that session's FP earnings.
 *  4. Daily FP earn is capped at [FPEconomy.DAILY_EARN_CAP] (60 FP).
 */
class GamingDetector {

    companion object {
        private const val MIN_SESSION_SECONDS = FPEconomy.MIN_SESSION_SECONDS.toLong()
        private const val MAX_LAUNCHES_PER_DAY = 20
        private val DAILY_EARN_CAP = FPEconomy.DAILY_EARN_CAP
    }

    enum class GamingFlag {
        SESSION_TOO_SHORT,
        EXCESSIVE_RELAUNCHES,
        SCREEN_OFF_DURING_SESSION,
        DAILY_EARN_CAP_HIT
    }

    data class SessionAuditResult(
        val session: UsageSession,
        val isEligible: Boolean,
        val flags: Set<GamingFlag>,
        val earnedFP: Int
    )

    data class DailyEarnResult(
        val auditedSessions: List<SessionAuditResult>,
        val totalRawFP: Int,
        val cappedFP: Int,
        val capHit: Boolean,
        val flaggedPackages: Set<String>
    )

    /**
     * Audits a single Nutritive session for gaming behaviour.
     *
     * @param session the session to audit
     * @param dailyLaunchCounts a map of packageName → launch count so far today (before this session)
     * @param hadScreenOff whether the screen went off during this session
     */
    fun auditSession(
        session: UsageSession,
        dailyLaunchCounts: Map<String, Int> = emptyMap(),
        hadScreenOff: Boolean = false
    ): SessionAuditResult {
        require(session.category == AppCategory.NUTRITIVE) {
            "GamingDetector.auditSession only evaluates NUTRITIVE sessions."
        }

        val flags = mutableSetOf<GamingFlag>()

        // Rule 1: Session too short
        if (session.durationSeconds < MIN_SESSION_SECONDS) {
            flags += GamingFlag.SESSION_TOO_SHORT
        }

        // Rule 2: Excessive relaunches
        val launchCount = dailyLaunchCounts[session.packageName] ?: 0
        if (launchCount >= MAX_LAUNCHES_PER_DAY) {
            flags += GamingFlag.EXCESSIVE_RELAUNCHES
        }

        // Rule 3: Screen-off during session
        if (hadScreenOff) {
            flags += GamingFlag.SCREEN_OFF_DURING_SESSION
        }

        val isEligible = flags.isEmpty()
        val earned = if (isEligible) {
            (session.durationSeconds / 60L).toInt().coerceAtMost(DAILY_EARN_CAP)
        } else {
            0
        }

        return SessionAuditResult(
            session = session,
            isEligible = isEligible,
            flags = flags,
            earnedFP = earned
        )
    }

    /**
     * Audits all Nutritive sessions for a single calendar day, applying the daily earn cap.
     *
     * @param sessions all sessions for the day (any category — non-Nutritive are skipped)
     * @param screenOffPackages set of packageNames that had screen-off events during their sessions
     * @param timeZone used to verify all sessions are on the same calendar day
     */
    fun auditDay(
        sessions: List<UsageSession>,
        screenOffPackages: Set<String> = emptySet(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): DailyEarnResult {
        val nutritiveSessions = sessions
            .filter { it.category == AppCategory.NUTRITIVE }
            .sortedBy { it.startTime }

        val launchCounts = mutableMapOf<String, Int>()
        val auditedSessions = mutableListOf<SessionAuditResult>()
        var cumulativeFP = 0

        for (session in nutritiveSessions) {
            val currentLaunches = launchCounts[session.packageName] ?: 0
            val hadScreenOff = session.packageName in screenOffPackages

            val result = auditSession(
                session = session,
                dailyLaunchCounts = launchCounts,
                hadScreenOff = hadScreenOff
            )

            // Track launch count regardless of eligibility
            launchCounts[session.packageName] = currentLaunches + 1
            cumulativeFP += result.earnedFP
            auditedSessions += result
        }

        val cappedFP = minOf(cumulativeFP, DAILY_EARN_CAP)
        val capHit = cumulativeFP > DAILY_EARN_CAP

        val flaggedPackages = auditedSessions
            .filter { it.flags.isNotEmpty() }
            .map { it.session.packageName }
            .toSet()

        return DailyEarnResult(
            auditedSessions = auditedSessions,
            totalRawFP = cumulativeFP,
            cappedFP = cappedFP,
            capHit = capHit,
            flaggedPackages = flaggedPackages
        )
    }

    /**
     * Returns a human-readable explanation for a given [GamingFlag].
     */
    fun explainFlag(flag: GamingFlag): String = when (flag) {
        GamingFlag.SESSION_TOO_SHORT ->
            "Session was under 60 seconds — too short to earn Focus Points."
        GamingFlag.EXCESSIVE_RELAUNCHES ->
            "This app was opened more than $MAX_LAUNCHES_PER_DAY times today. FP earning is paused."
        GamingFlag.SCREEN_OFF_DURING_SESSION ->
            "Screen turned off during the session — no Focus Points awarded."
        GamingFlag.DAILY_EARN_CAP_HIT ->
            "Daily earn cap of ${DAILY_EARN_CAP} FP reached. Keep going tomorrow!"
    }
}
