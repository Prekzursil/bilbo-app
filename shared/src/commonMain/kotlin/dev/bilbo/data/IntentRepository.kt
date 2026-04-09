package dev.bilbo.data

import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.IntentDeclaration
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository for persisting and querying [IntentDeclaration] records.
 */
interface IntentRepository {

    /**
     * Observe all declarations, newest first.
     * Emits whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<IntentDeclaration>>

    /**
     * Return all stored declarations.
     */
    suspend fun getAll(): List<IntentDeclaration>

    /**
     * Return a single declaration by its primary key, or null if not found.
     */
    suspend fun getById(id: Long): IntentDeclaration?

    /**
     * Return all declarations for [packageName], newest first.
     */
    suspend fun getByApp(packageName: String): List<IntentDeclaration>

    /**
     * Return all declarations whose timestamp falls within [[from], [to]].
     */
    suspend fun getByDateRange(from: Instant, to: Instant): List<IntentDeclaration>

    /**
     * Return all declarations where the user overrode an enforcement action.
     */
    suspend fun getOverridden(): List<IntentDeclaration>

    /**
     * Persist a new declaration.
     * Returns the auto-generated row id.
     */
    suspend fun insert(declaration: IntentDeclaration): Long

    /**
     * Record the actual usage duration once a session completes.
     */
    suspend fun updateActualDuration(id: Long, actualDurationMinutes: Int)

    /**
     * Record enforcement outcome for a declaration.
     */
    suspend fun updateEnforcement(
        id: Long,
        wasEnforced: Boolean,
        enforcementType: EnforcementMode?,
        wasOverridden: Boolean
    )

    /**
     * Remove the declaration with [id].
     */
    suspend fun deleteById(id: Long)

    /**
     * Return the number of "accurate" intents (actual within ±2 min of declared)
     * in [[from], [to]].
     */
    suspend fun countAccurate(from: Instant, to: Instant): Long

    /**
     * Return the total number of intents recorded in [[from], [to]].
     */
    suspend fun countTotal(from: Instant, to: Instant): Long

    /**
     * Return the intent accuracy as a percentage (0–100) for [[from], [to]].
     * Returns 0 if there are no intents in the range.
     */
    suspend fun accuracyPercent(from: Instant, to: Instant): Float {
        val total = countTotal(from, to)
        if (total == 0L) return 0f
        return (countAccurate(from, to).toFloat() / total.toFloat()) * 100f
    }
}
