package dev.bilbo.shared.data.repository

import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.WeeklyInsight
import dev.bilbo.shared.data.remote.BilboApiService
import dev.bilbo.shared.domain.model.DailyInsight
import dev.bilbo.shared.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.LocalDate

/**
 * Mediates between the data layer (remote API, local DB) and the domain layer.
 *
 * Implements a simple cache-then-network strategy: emit cached data immediately,
 * then refresh from the network and emit updated data.
 *
 * Extended in Phase 8 to support storing heuristic insights and weekly narratives
 * produced by [NightlyAnalysisWorker] and [WeeklyInsightWorker].
 */
class InsightRepository(
    private val apiService: BilboApiService,
) {

    // ── In-memory caches (production would use SQLDelight / DataStore) ─────────

    /** Cache of weekly insights keyed by weekStart date string. */
    private val weeklyInsightCache = mutableMapOf<String, WeeklyInsight>()

    /** Cache of heuristic insights keyed by weekStart date string. */
    private val heuristicCache = mutableMapOf<String, List<HeuristicInsight>>()

    /** Cache of correlation data keyed by weekStart date string. */
    private val correlationCache = mutableMapOf<String, Map<String, Float>>()

    // ── Daily insights (existing) ─────────────────────────────────────────────

    /**
     * Returns a [Flow] that emits [Result] states for daily insights.
     *
     * Emits [Result.Loading] first, then either [Result.Success] or [Result.Error].
     */
    fun getDailyInsights(limit: Int = 30): Flow<Result<List<DailyInsight>>> = flow {
        emit(Result.Loading)
        try {
            val insights = apiService.getDailyInsights(limit)
            emit(Result.Success(insights))
        } catch (e: Exception) {
            emit(Result.Error(e))
        }
    }

    /**
     * Requests AI-generated insight for the specified date.
     */
    suspend fun generateInsight(date: String): Result<DailyInsight> = try {
        val insight = apiService.generateAiInsight(date)
        Result.Success(insight)
    } catch (e: Exception) {
        Result.Error(e)
    }

    // ── Heuristic insights (Phase 8) ──────────────────────────────────────────

    /**
     * Stores the list of [HeuristicInsight] entries produced by [HeuristicEngine.analyzeWeek]
     * for the given [weekStart] date.
     *
     * In production this would persist to SQLDelight. For now we use an in-memory cache.
     */
    suspend fun storeHeuristicInsights(
        weekStart: LocalDate,
        insights: List<HeuristicInsight>,
    ) {
        heuristicCache[weekStart.toString()] = insights
    }

    /**
     * Returns cached heuristic insights for [weekStart], or an empty list if none are stored.
     */
    suspend fun getHeuristicInsights(weekStart: LocalDate): List<HeuristicInsight> =
        heuristicCache[weekStart.toString()] ?: emptyList()

    /**
     * Updates the correlation data cache for the given [weekStart].
     * Values are a map of correlation label → strength (0–1).
     */
    suspend fun updateCorrelationCache(
        weekStart: LocalDate,
        correlations: Map<String, Float>,
    ) {
        correlationCache[weekStart.toString()] = correlations
    }

    /**
     * Returns cached correlation data for [weekStart], or an empty map.
     */
    suspend fun getCorrelationCache(weekStart: LocalDate): Map<String, Float> =
        correlationCache[weekStart.toString()] ?: emptyMap()

    // ── Weekly insights (Phase 8) ─────────────────────────────────────────────

    /**
     * Persists a [WeeklyInsight] (containing both Tier-2 heuristics and optional
     * Tier-3 cloud narrative) for the week starting on [weekStart].
     *
     * In production this would upsert into SQLDelight. For now we use an in-memory cache.
     */
    suspend fun storeWeeklyInsight(insight: WeeklyInsight) {
        weeklyInsightCache[insight.weekStart.toString()] = insight
    }

    /**
     * Returns the most recent [WeeklyInsight], or null if none has been stored yet.
     */
    suspend fun getLatestWeeklyInsight(): WeeklyInsight? =
        weeklyInsightCache.values.maxByOrNull { it.weekStart.toString() }

    /**
     * Returns the [WeeklyInsight] for [weekStart], or null if not yet generated.
     */
    suspend fun getWeeklyInsight(weekStart: LocalDate): WeeklyInsight? =
        weeklyInsightCache[weekStart.toString()]

    /**
     * Returns all stored weekly insights, sorted by weekStart descending (newest first).
     */
    suspend fun getAllWeeklyInsights(): List<WeeklyInsight> =
        weeklyInsightCache.values.sortedByDescending { it.weekStart.toString() }
}
