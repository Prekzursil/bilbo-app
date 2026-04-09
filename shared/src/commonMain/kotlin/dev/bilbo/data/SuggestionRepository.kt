package dev.bilbo.data

import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay
import kotlinx.coroutines.flow.Flow

/**
 * Repository for persisting and querying [AnalogSuggestion] records.
 */
interface SuggestionRepository {

    /**
     * Observe all suggestions ordered by category then text.
     * Emits whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<AnalogSuggestion>>

    /**
     * Return all stored suggestions.
     */
    suspend fun getAll(): List<AnalogSuggestion>

    /**
     * Return a single suggestion by its primary key, or null if not found.
     */
    suspend fun getById(id: Long): AnalogSuggestion?

    /**
     * Return all suggestions in [category], ordered by acceptance count descending.
     */
    suspend fun getByCategory(category: SuggestionCategory): List<AnalogSuggestion>

    /**
     * Return all suggestions appropriate for [timeOfDay] (including those
     * without a time restriction), ordered by acceptance count descending.
     */
    suspend fun getByTimeOfDay(timeOfDay: TimeOfDay): List<AnalogSuggestion>

    /**
     * Return suggestions filtered by both [category] and [timeOfDay].
     */
    suspend fun getByCategoryAndTimeOfDay(
        category: SuggestionCategory,
        timeOfDay: TimeOfDay
    ): List<AnalogSuggestion>

    /**
     * Return user-created suggestions.
     */
    suspend fun getCustom(): List<AnalogSuggestion>

    /**
     * Return the [limit] most-accepted suggestions across all categories.
     */
    suspend fun getTopAccepted(limit: Long): List<AnalogSuggestion>

    /**
     * Persist a new suggestion.
     * Returns the auto-generated row id.
     */
    suspend fun insert(suggestion: AnalogSuggestion): Long

    /**
     * Update the mutable content fields (text, category, tags, timeOfDay) of
     * an existing suggestion.
     */
    suspend fun update(suggestion: AnalogSuggestion)

    /**
     * Atomically increment the [AnalogSuggestion.timesShown] counter for [id].
     */
    suspend fun recordShown(id: Long)

    /**
     * Atomically increment the [AnalogSuggestion.timesAccepted] counter for [id].
     */
    suspend fun recordAccepted(id: Long)

    /**
     * Remove the suggestion with [id].
     */
    suspend fun deleteById(id: Long)
}
