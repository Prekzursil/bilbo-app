package dev.bilbo.shared.domain.usecase

import dev.bilbo.shared.data.repository.InsightRepository
import dev.bilbo.shared.domain.model.DailyInsight
import dev.bilbo.shared.util.Result
import kotlinx.coroutines.flow.Flow

/**
 * Use case for fetching the user's daily wellness insights.
 *
 * Sits between the presentation layer and the [InsightRepository],
 * applying any domain-level business logic (sorting, filtering, etc.)
 * before delivering data to the UI.
 */
class GetDailyInsightsUseCase(
    private val repository: InsightRepository,
) {

    /**
     * Execute the use case.
     *
     * @param limit Maximum number of insight records to return (default 30).
     * @return A [Flow] emitting [Result] states wrapping a list of [DailyInsight].
     */
    operator fun invoke(limit: Int = 30): Flow<Result<List<DailyInsight>>> =
        repository.getDailyInsights(limit)
}
