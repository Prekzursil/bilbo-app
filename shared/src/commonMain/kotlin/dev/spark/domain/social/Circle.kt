package dev.spark.domain.social

import kotlinx.datetime.Instant

/**
 * A named group of users who share wellness progress with each other.
 * Circles can be public (discoverable) or invite-only.
 */
data class Circle(
    val id: Long = 0,
    val remoteId: String,
    val name: String,
    val description: String? = null,
    val avatarUrl: String? = null,
    val ownerId: String,
    val sharingLevel: SharingLevel,
    val isPublic: Boolean = false,
    val inviteCode: String? = null,
    val createdAt: Instant,
    val memberCount: Int = 0,
    val members: List<CircleMember> = emptyList()
)

/**
 * A single user's membership record within a Circle.
 */
data class CircleMember(
    val userId: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val role: CircleRole,
    val joinedAt: Instant,
    val sharingLevel: SharingLevel,
    val isActive: Boolean = true,
    val currentStreakDays: Int = 0,
    val weeklyFpEarned: Int = 0,
    val weeklyFpBalance: Int = 0
)

/**
 * Administrative role within a Circle.
 */
enum class CircleRole {
    /** Read-only participant; can view data but cannot manage the circle. */
    MEMBER,

    /** Can invite/remove members and edit circle settings. */
    MODERATOR,

    /** Full control including deletion and ownership transfer. */
    OWNER
}
