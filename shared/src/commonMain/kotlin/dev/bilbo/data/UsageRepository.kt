package dev.bilbo.data

import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository for persisting and querying [UsageSession] records.
 * All suspend functions are safe to call from any coroutine context.
 */
interface UsageRepository {

    /**
     * Observe all sessions, ordered by start time descending.
     * Emits a new list whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<UsageSession>>

    /**
     * Return all stored sessions.
     */
    suspend fun getAll(): List<UsageSession>

    /**
     * Return a single session by its primary key, or null if not found.
     */
    suspend fun getById(id: Long): UsageSession?

    /**
     * Return all sessions for the given [packageName], newest first.
     */
    suspend fun getByPackageName(packageName: String): List<UsageSession>

    /**
     * Return all sessions whose [UsageSession.startTime] falls within
     * [[from], [to]] inclusive, newest first.
     */
    suspend fun getByDateRange(from: Instant, to: Instant): List<UsageSession>

    /**
     * Return all sessions belonging to [category], newest first.
     */
    suspend fun getByCategory(category: AppCategory): List<UsageSession>

    /**
     * Return sessions filtered by both date range and category.
     */
    suspend fun getByDateRangeAndCategory(
        from: Instant,
        to: Instant,
        category: AppCategory
    ): List<UsageSession>

    /**
     * Persist a new session. If [session.id] is 0, the database assigns an id.
     * Returns the auto-generated row id.
     */
    suspend fun insert(session: UsageSession): Long

    /**
     * Update the [endTime] and [durationSeconds] of an existing session.
     */
    suspend fun updateEndTime(id: Long, endTime: Instant, durationSeconds: Long)

    /**
     * Remove the session with [id].
     */
    suspend fun deleteById(id: Long)

    /**
     * Remove all sessions whose [UsageSession.startTime] is before [before].
     * Useful for data retention / pruning old records.
     */
    suspend fun deleteOlderThan(before: Instant)

    /**
     * Return the total session count for [packageName].
     */
    suspend fun countByPackageName(packageName: String): Long

    /**
     * Return a map of category name to total duration in seconds for the given
     * date range.
     */
    suspend fun sumDurationByCategory(from: Instant, to: Instant): Map<AppCategory, Long>
}
