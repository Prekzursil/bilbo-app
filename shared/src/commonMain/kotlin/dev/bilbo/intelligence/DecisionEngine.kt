package dev.bilbo.intelligence

import dev.bilbo.domain.*
import dev.bilbo.intelligence.tier1.LaunchDecision
import dev.bilbo.intelligence.tier1.EnforcementAction
import dev.bilbo.intelligence.tier1.RuleEngine
import dev.bilbo.intelligence.tier2.CorrelationAnalyzer
import dev.bilbo.intelligence.tier2.GamingDetector
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.intelligence.tier2.TrendDetector
import dev.bilbo.intelligence.tier3.CloudInsightClient
import dev.bilbo.intelligence.tier3.InsightPromptBuilder
import kotlinx.datetime.*

/**
 * Top-level orchestrator for all three intelligence tiers.
 *
 *  - Tier 1 ([RuleEngine]): real-time launch / enforcement decisions.
 *  - Tier 2 ([HeuristicEngine]): batch weekly analysis (on-device).
 *  - Tier 3 ([CloudInsightClient]): weekly narrative from cloud AI (rate-limited).
 */
class DecisionEngine(
    // Tier 1 dependencies
    private val appProfileProvider: (String) -> AppProfile?,
    private val budgetProvider: () -> DopamineBudget,
    private val cooldownChecker: (String) -> Int?,

    // Tier 3 dependencies
    private val cloudInsightClient: CloudInsightClient,
    private val anonymousUserId: String,

    // Optional overrides for testing / dependency injection
    private val ruleEngine: RuleEngine = RuleEngine(appProfileProvider, budgetProvider, cooldownChecker),
    private val heuristicEngine: HeuristicEngine = HeuristicEngine(
        correlationAnalyzer = CorrelationAnalyzer(),
        trendDetector = TrendDetector(),
        gamingDetector = GamingDetector()
    ),
    private val promptBuilder: InsightPromptBuilder = InsightPromptBuilder()
) {

    // -------------------------------------------------------------------------
    // Tier 1 — Real-time decisions
    // -------------------------------------------------------------------------

    /**
     * Evaluates whether [packageName] should be allowed to launch.
     * Called synchronously on the UI thread when the user opens an app.
     */
    fun evaluateAppLaunch(packageName: String): LaunchDecision =
        ruleEngine.evaluateAppLaunch(packageName)

    /**
     * Evaluates what enforcement action to take when a session timer expires.
     */
    fun evaluateTimerExpiry(profile: AppProfile): EnforcementAction =
        ruleEngine.evaluateTimerExpiry(profile)

    // -------------------------------------------------------------------------
    // Tier 2 — Batch weekly analysis (run off main thread)
    // -------------------------------------------------------------------------

    /**
     * Runs Tier-2 heuristic analysis for the given week.
     * Should be called from a background coroutine (e.g. WorkManager / background scope).
     *
     * @return A [WeeklyInsight] populated with Tier-2 heuristic insights (no Tier-3 narrative yet).
     */
    fun runWeeklyAnalysis(
        weekStart: LocalDate,
        sessions: List<UsageSession>,
        checkIns: List<EmotionalCheckIn>,
        intents: List<IntentDeclaration>,
        budget: DopamineBudget,
        priorWeekSessions: List<UsageSession> = emptyList(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): WeeklyInsight {
        val heuristicInsights = heuristicEngine.analyzeWeek(
            weekStart = weekStart,
            sessions = sessions,
            checkIns = checkIns,
            intents = intents,
            priorWeekSessions = priorWeekSessions,
            timeZone = timeZone
        )

        val nutritiveSessions = sessions.filter { it.category == AppCategory.NUTRITIVE }
        val emptySessions = sessions.filter { it.category == AppCategory.EMPTY_CALORIES }

        val intentAccuracy = computeIntentAccuracy(intents)
        val streakDays = computeStreakDays(sessions, timeZone)

        return WeeklyInsight(
            weekStart = weekStart,
            tier2Insights = heuristicInsights,
            tier3Narrative = null,  // filled in by Tier-3 after cloud call
            totalScreenTimeMinutes = (sessions.sumOf { it.durationSeconds } / 60L).toInt(),
            nutritiveMinutes = (nutritiveSessions.sumOf { it.durationSeconds } / 60L).toInt(),
            emptyCalorieMinutes = (emptySessions.sumOf { it.durationSeconds } / 60L).toInt(),
            fpEarned = budget.fpEarned,
            fpSpent = budget.fpSpent,
            intentAccuracyPercent = intentAccuracy,
            streakDays = streakDays
        )
    }

    // -------------------------------------------------------------------------
    // Tier 3 — Cloud narrative (suspend, network call)
    // -------------------------------------------------------------------------

    /**
     * Fetches the Tier-3 narrative from the cloud AI and merges it into [weeklyInsight].
     * Returns the updated [WeeklyInsight] with [WeeklyInsight.tier3Narrative] populated,
     * or the original insight if the request was rate-limited / failed.
     *
     * Must be called from a coroutine (suspend).
     */
    suspend fun enrichWithCloudNarrative(
        weeklyInsight: WeeklyInsight,
        budget: DopamineBudget,
        checkIns: List<EmotionalCheckIn>,
        sessions: List<UsageSession>,
        weekOverWeekChange: Double? = null
    ): WeeklyInsight {
        if (!cloudInsightClient.canRequest()) return weeklyInsight

        val payload = promptBuilder.buildPayload(
            weekStart = weeklyInsight.weekStart,
            budget = budget,
            insight = weeklyInsight,
            checkIns = checkIns,
            sessions = sessions,
            weekOverWeekChange = weekOverWeekChange
        )

        return when (val result = cloudInsightClient.fetchNarrativeForWeek(payload, anonymousUserId)) {
            is CloudInsightClient.InsightResult.Success ->
                weeklyInsight.copy(tier3Narrative = result.narrative)

            is CloudInsightClient.InsightResult.RateLimited ->
                weeklyInsight  // silently skip — already rate-limited server-side

            is CloudInsightClient.InsightResult.NetworkError ->
                weeklyInsight  // offline or transient error — return as-is

            is CloudInsightClient.InsightResult.ServerError ->
                weeklyInsight  // server-side issue — return as-is
        }
    }

    /**
     * Full weekly pipeline: Tier-2 analysis followed by optional Tier-3 cloud enrichment.
     *
     * @param attemptCloudNarrative Set false to skip the cloud call (e.g. offline mode).
     */
    suspend fun runFullWeeklyPipeline(
        weekStart: LocalDate,
        sessions: List<UsageSession>,
        checkIns: List<EmotionalCheckIn>,
        intents: List<IntentDeclaration>,
        budget: DopamineBudget,
        priorWeekSessions: List<UsageSession> = emptyList(),
        weekOverWeekChange: Double? = null,
        attemptCloudNarrative: Boolean = true,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): WeeklyInsight {
        val tier2Insight = runWeeklyAnalysis(
            weekStart = weekStart,
            sessions = sessions,
            checkIns = checkIns,
            intents = intents,
            budget = budget,
            priorWeekSessions = priorWeekSessions,
            timeZone = timeZone
        )

        return if (attemptCloudNarrative) {
            enrichWithCloudNarrative(
                weeklyInsight = tier2Insight,
                budget = budget,
                checkIns = checkIns,
                sessions = sessions,
                weekOverWeekChange = weekOverWeekChange
            )
        } else {
            tier2Insight
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private fun computeIntentAccuracy(intents: List<IntentDeclaration>): Float {
        val completed = intents.filter { it.actualDurationMinutes != null }
        if (completed.isEmpty()) return 0f

        val accurate = completed.count { intent ->
            val declared = intent.declaredDurationMinutes
            val actual = intent.actualDurationMinutes ?: return@count false
            val delta = kotlin.math.abs(actual - declared).toDouble() / declared
            delta <= 0.20
        }
        return accurate.toFloat() / completed.size.toFloat()
    }

    private fun computeStreakDays(
        sessions: List<UsageSession>,
        timeZone: TimeZone
    ): Int {
        if (sessions.isEmpty()) return 0

        // Count consecutive days (ending today) with at least one nutritive session
        val nutritiveDates = sessions
            .filter { it.category == AppCategory.NUTRITIVE }
            .map { it.startTime.toLocalDateTime(timeZone).date }
            .toSet()

        val today = Clock.System.now().toLocalDateTime(timeZone).date
        var streak = 0
        var current = today
        while (current in nutritiveDates) {
            streak++
            current = current.minus(1, DateTimeUnit.DAY)
        }
        return streak
    }
}
