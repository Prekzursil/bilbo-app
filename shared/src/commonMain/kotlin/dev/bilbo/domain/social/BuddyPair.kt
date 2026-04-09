package dev.bilbo.domain.social

import kotlinx.datetime.Instant

/**
 * Represents a one-to-one accountability pairing between two users.
 * Buddies share a configurable level of wellness data with each other.
 */
data class BuddyPair(
    val id: Long = 0,
    val localUserId: String,
    val buddyUserId: String,
    val buddyDisplayName: String,
    val buddyAvatarUrl: String? = null,
    val sharingLevel: SharingLevel,
    val createdAt: Instant,
    val lastSyncedAt: Instant? = null,
    val isActive: Boolean = true,
    val streakDays: Int = 0,
    val lastEncouragementSentAt: Instant? = null,
    val lastEncouragementReceivedAt: Instant? = null
)

/**
 * Granularity of data shared between users. Higher levels include all data
 * from lower levels plus additional details.
 */
enum class SharingLevel {
    /** Share nothing — buddy relationship exists but no data is exchanged. */
    PRIVATE,

    /** Share only aggregate scores: daily FP balance and streak length. */
    SCORES_ONLY,

    /** Share category-level breakdown (nutritive/empty-calorie/neutral minutes). */
    CATEGORY_BREAKDOWN,

    /** Share app-level usage totals (app names + durations, no session timestamps). */
    APP_TOTALS,

    /** Full transparency: all session data including timing and intent accuracy. */
    FULL
}
