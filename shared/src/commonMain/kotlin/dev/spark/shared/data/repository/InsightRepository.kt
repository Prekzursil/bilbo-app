package dev.spark.shared.data.repository

import dev.spark.shared.data.remote.SparkApiService
import dev.spark.shared.domain.model.DailyInsight
import dev.spark.shared.util.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mediates between the data layer (remote API, local DB) and the domain layer.
 *
 * Implements a simple cache-then-network strategy: emit cached data immediately,
 * then refresh from the network and emit updated data.
 */
class InsightRepository(
    private val apiService: SparkApiService,
) {

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
}
