package dev.bilbo.data

import dev.bilbo.domain.Emotion
import dev.bilbo.domain.EmotionalCheckIn
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant

/**
 * Repository for persisting and querying [EmotionalCheckIn] records.
 */
interface EmotionRepository {

    /**
     * Observe all check-ins, ordered by timestamp descending.
     * Emits whenever the underlying data changes.
     */
    fun observeAll(): Flow<List<EmotionalCheckIn>>

    /**
     * Return all stored check-ins.
     */
    suspend fun getAll(): List<EmotionalCheckIn>

    /**
     * Return a single check-in by its primary key, or null if not found.
     */
    suspend fun getById(id: Long): EmotionalCheckIn?

    /**
     * Return all check-ins whose timestamp falls within [[from], [to]].
     */
    suspend fun getByDateRange(from: Instant, to: Instant): List<EmotionalCheckIn>

    /**
     * Return the check-in linked to [intentId], or null if none exists.
     */
    suspend fun getByIntentId(intentId: Long): EmotionalCheckIn?

    /**
     * Persist a new check-in.
     * Returns the auto-generated row id.
     */
    suspend fun insert(checkIn: EmotionalCheckIn): Long

    /**
     * Record the post-session mood on an existing check-in.
     */
    suspend fun updatePostMood(id: Long, postMood: Emotion)

    /**
     * Remove the check-in with [id].
     */
    suspend fun deleteById(id: Long)
}
