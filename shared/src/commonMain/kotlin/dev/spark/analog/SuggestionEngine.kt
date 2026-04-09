package dev.spark.analog

import dev.spark.data.BudgetRepository
import dev.spark.data.SuggestionRepository
import dev.spark.domain.AnalogSuggestion
import dev.spark.domain.FPEconomy
import dev.spark.domain.SuggestionCategory
import dev.spark.domain.TimeOfDay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Selects, scores, and records interactions with [AnalogSuggestion] items.
 *
 * Selection algorithm:
 *  1. Fetch all suggestions from [suggestionRepo].
 *  2. Keep only those whose [AnalogSuggestion.timeOfDay] matches [currentTimeOfDay]
 *     or is null (any time).
 *  3. Further filter to suggestions whose [AnalogSuggestion.category] is in [userInterests].
 *     If that leaves fewer than [count] items, fall back to all time-filtered suggestions.
 *  4. Sort by a composite score: least recently shown first, then highest acceptance rate.
 *  5. Return the top [count] suggestions.
 *
 * @param suggestionRepo   Persistence layer for [AnalogSuggestion] records.
 * @param budgetRepository Used to award +5 FP when the user accepts a suggestion.
 * @param userInterests    Lambda returning the current set of preferred [SuggestionCategory] items.
 * @param currentTimeOfDay Lambda returning the current [TimeOfDay] (injected for testability).
 * @param clock            Clock used to determine today's date for FP awarding.
 */
class SuggestionEngine(
    private val suggestionRepo: SuggestionRepository,
    private val budgetRepository: BudgetRepository,
    private val userInterests: () -> Set<SuggestionCategory>,
    private val currentTimeOfDay: () -> TimeOfDay = { resolveTimeOfDay() },
    private val clock: Clock = Clock.System,
) {

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Returns up to [count] personalised [AnalogSuggestion] items for the current moment.
     */
    suspend fun getSuggestions(count: Int = 3): List<AnalogSuggestion> {
        val allSuggestions = suggestionRepo.getAll()
        val tod = currentTimeOfDay()
        val interests = userInterests()

        // Step 2 – filter by time of day (include null = any time)
        val timeFiltered = allSuggestions.filter { s ->
            s.timeOfDay == null || s.timeOfDay == tod
        }

        // Step 3 – filter by user interests; fall back if too few results
        val interestFiltered = timeFiltered.filter { s -> s.category in interests }
        val pool = if (interestFiltered.size >= count) interestFiltered else timeFiltered

        // Step 4 – sort: least shown first, then highest acceptance rate
        val sorted = pool.sortedWith(
            compareBy<AnalogSuggestion> { it.timesShown }
                .thenByDescending { it.acceptanceRate }
        )

        // Step 5 – top N
        return sorted.take(count)
    }

    /**
     * Marks a suggestion as accepted: increments [AnalogSuggestion.timesAccepted]
     * and awards [FPEconomy.BONUS_ANALOG_ACCEPTED] (+5 FP) for today.
     */
    suspend fun acceptSuggestion(id: Long) {
        suggestionRepo.recordAccepted(id)

        val today = clock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        budgetRepository.incrementFpBonus(today, FPEconomy.BONUS_ANALOG_ACCEPTED)
    }

    /**
     * Records that this suggestion was shown again (user tapped "Show another").
     * Only increments [AnalogSuggestion.timesShown] — no FP change.
     */
    suspend fun showAnother(id: Long) {
        suggestionRepo.recordShown(id)
    }

    /**
     * Saves a user-created custom suggestion and returns its generated id.
     */
    suspend fun addCustomSuggestion(suggestion: AnalogSuggestion): Long {
        require(suggestion.text.isNotBlank()) { "Suggestion text must not be blank" }
        return suggestionRepo.insert(suggestion.copy(isCustom = true))
    }

    /**
     * Removes a user-created custom suggestion.  Built-in suggestions cannot be deleted.
     */
    suspend fun deleteCustomSuggestion(id: Long) {
        val suggestion = suggestionRepo.getById(id)
        checkNotNull(suggestion) { "No suggestion with id=$id" }
        check(suggestion.isCustom) { "Only custom suggestions can be deleted (id=$id)" }
        suggestionRepo.deleteById(id)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    companion object {
        /**
         * Resolves the current [TimeOfDay] from the device clock.
         *
         *   - 05:00–11:59 → MORNING
         *   - 12:00–16:59 → AFTERNOON
         *   - 17:00–20:59 → EVENING
         *   - 21:00–04:59 → NIGHT
         */
        fun resolveTimeOfDay(clock: Clock = Clock.System): TimeOfDay {
            val hour = clock.now()
                .toLocalDateTime(TimeZone.currentSystemDefault())
                .hour
            return when (hour) {
                in 5..11  -> TimeOfDay.MORNING
                in 12..16 -> TimeOfDay.AFTERNOON
                in 17..20 -> TimeOfDay.EVENING
                else      -> TimeOfDay.NIGHT
            }
        }
    }
}

// ── Extension ─────────────────────────────────────────────────────────────────

/** Acceptance rate in [0, 1]; avoids divide-by-zero. */
private val AnalogSuggestion.acceptanceRate: Float
    get() = if (timesShown == 0) 0f else timesAccepted.toFloat() / timesShown.toFloat()
