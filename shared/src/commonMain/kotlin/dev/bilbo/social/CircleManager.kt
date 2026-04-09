package dev.bilbo.social

import kotlin.time.Clock
import kotlinx.datetime.*

/**
 * Manages focus circles — small accountability groups (2–12 members).
 *
 * Features:
 *  - Create, join, and leave circles.
 *  - Role-based membership (Admin / Member).
 *  - Invite by code or direct user ID.
 *  - Circles can be public (discoverable) or private (invite-only).
 */
class CircleManager {

    companion object {
        const val MIN_CIRCLE_SIZE = 2
        const val MAX_CIRCLE_SIZE = 12
        const val INVITE_CODE_LENGTH = 8
        const val INVITE_EXPIRY_HOURS = 72L
    }

    enum class CircleRole { ADMIN, MEMBER }
    enum class CircleVisibility { PUBLIC, PRIVATE }

    data class Circle(
        val circleId: String,
        val name: String,
        val description: String,
        val visibility: CircleVisibility,
        val createdAt: Instant,
        val createdByUserId: String,
        val inviteCode: String,
        val isActive: Boolean = true
    )

    data class CircleMembership(
        val circleId: String,
        val userId: String,
        val role: CircleRole,
        val joinedAt: Instant,
        val isActive: Boolean = true
    )

    data class CircleInvite(
        val inviteId: String,
        val circleId: String,
        val invitedByUserId: String,
        val invitedUserId: String?,      // null if invite-code-only
        val inviteCode: String,
        val createdAt: Instant,
        val expiresAt: Instant,
        val isUsed: Boolean = false
    )

    // In-memory state
    private val circles = mutableMapOf<String, Circle>()
    private val memberships = mutableListOf<CircleMembership>()
    private val invites = mutableListOf<CircleInvite>()

    // -------------------------------------------------------------------------
    // Circle creation
    // -------------------------------------------------------------------------

    /**
     * Creates a new circle and automatically adds [creatorUserId] as the Admin.
     */
    fun createCircle(
        name: String,
        description: String = "",
        visibility: CircleVisibility = CircleVisibility.PRIVATE,
        creatorUserId: String,
        clock: Clock = Clock.System
    ): Circle {
        require(name.isNotBlank()) { "Circle name must not be blank." }

        val now = clock.now()
        val circle = Circle(
            circleId = generateId(),
            name = name.trim(),
            description = description.trim(),
            visibility = visibility,
            createdAt = now,
            createdByUserId = creatorUserId,
            inviteCode = generateInviteCode()
        )
        circles[circle.circleId] = circle

        // Creator becomes admin
        memberships += CircleMembership(
            circleId = circle.circleId,
            userId = creatorUserId,
            role = CircleRole.ADMIN,
            joinedAt = now
        )
        return circle
    }

    // -------------------------------------------------------------------------
    // Join / Leave
    // -------------------------------------------------------------------------

    /**
     * Joins a circle by invite code.
     *
     * @throws IllegalArgumentException if the code is invalid or expired.
     * @throws IllegalStateException if the circle is full.
     */
    fun joinByInviteCode(
        inviteCode: String,
        joiningUserId: String,
        clock: Clock = Clock.System
    ): CircleMembership {
        val now = clock.now()

        // Look up circle by invite code
        val circle = circles.values.firstOrNull {
            it.inviteCode == inviteCode && it.isActive
        } ?: throw IllegalArgumentException("Invalid invite code.")

        return joinCircle(circle.circleId, joiningUserId, clock)
    }

    /**
     * Joins a circle directly by [circleId] (e.g. for public circles or direct invites).
     */
    fun joinCircle(
        circleId: String,
        joiningUserId: String,
        clock: Clock = Clock.System
    ): CircleMembership {
        val now = clock.now()
        val circle = circles[circleId] ?: throw IllegalArgumentException("Circle not found.")
        require(circle.isActive) { "Circle is no longer active." }

        val existing = getActiveMembership(circleId, joiningUserId)
        if (existing != null) return existing  // idempotent

        val memberCount = getActiveMemberships(circleId).size
        check(memberCount < MAX_CIRCLE_SIZE) {
            "Circle is full ($MAX_CIRCLE_SIZE members maximum)."
        }

        val membership = CircleMembership(
            circleId = circleId,
            userId = joiningUserId,
            role = CircleRole.MEMBER,
            joinedAt = now
        )
        memberships += membership
        return membership
    }

    /**
     * Leaves a circle. If the leaving user is the only admin, the next-longest member
     * is promoted to admin before the user departs.
     */
    fun leaveCircle(circleId: String, userId: String): Boolean {
        val membership = getActiveMembership(circleId, userId) ?: return false

        // If leaving admin is the last admin, promote another member
        if (membership.role == CircleRole.ADMIN) {
            val admins = getActiveMemberships(circleId).filter { it.role == CircleRole.ADMIN }
            if (admins.size == 1) {
                // Find oldest non-admin member to promote
                val nextAdmin = getActiveMemberships(circleId)
                    .filter { it.userId != userId }
                    .minByOrNull { it.joinedAt }
                if (nextAdmin != null) {
                    val idx = memberships.indexOf(nextAdmin)
                    memberships[idx] = nextAdmin.copy(role = CircleRole.ADMIN)
                } else {
                    // No other members — deactivate the circle
                    circles[circleId] = circles[circleId]!!.copy(isActive = false)
                }
            }
        }

        val idx = memberships.indexOf(membership)
        memberships[idx] = membership.copy(isActive = false)
        return true
    }

    /**
     * Removes a member from the circle (admin only).
     */
    fun removeMember(circleId: String, requestingUserId: String, targetUserId: String): Boolean {
        val requester = getActiveMembership(circleId, requestingUserId)
            ?: return false
        require(requester.role == CircleRole.ADMIN) { "Only admins can remove members." }
        require(requestingUserId != targetUserId) { "Admins cannot remove themselves — use leaveCircle." }

        val target = getActiveMembership(circleId, targetUserId) ?: return false
        val idx = memberships.indexOf(target)
        memberships[idx] = target.copy(isActive = false)
        return true
    }

    // -------------------------------------------------------------------------
    // Member management
    // -------------------------------------------------------------------------

    /**
     * Promotes a [targetUserId] to Admin within [circleId].
     */
    fun promoteToAdmin(circleId: String, requestingUserId: String, targetUserId: String) {
        val requester = getActiveMembership(circleId, requestingUserId)
        require(requester?.role == CircleRole.ADMIN) { "Only admins can promote members." }
        val target = getActiveMembership(circleId, targetUserId)
            ?: throw IllegalArgumentException("User is not a member of this circle.")
        val idx = memberships.indexOf(target)
        memberships[idx] = target.copy(role = CircleRole.ADMIN)
    }

    /**
     * Returns active memberships for a circle.
     */
    fun getActiveMemberships(circleId: String): List<CircleMembership> =
        memberships.filter { it.circleId == circleId && it.isActive }

    /**
     * Returns all circles a user is an active member of.
     */
    fun getCirclesForUser(userId: String): List<Circle> =
        memberships
            .filter { it.userId == userId && it.isActive }
            .mapNotNull { circles[it.circleId] }
            .filter { it.isActive }

    /**
     * Returns the circle for [circleId], or null.
     */
    fun getCircle(circleId: String): Circle? = circles[circleId]?.takeIf { it.isActive }

    /**
     * Returns all public circles (for discovery).
     */
    fun getPublicCircles(): List<Circle> =
        circles.values.filter { it.isActive && it.visibility == CircleVisibility.PUBLIC }

    /**
     * Returns the member count for [circleId].
     */
    fun getMemberCount(circleId: String): Int = getActiveMemberships(circleId).size

    // -------------------------------------------------------------------------
    // Circle settings
    // -------------------------------------------------------------------------

    /**
     * Updates a circle's name, description, or visibility (admin only).
     */
    fun updateCircle(
        circleId: String,
        requestingUserId: String,
        name: String? = null,
        description: String? = null,
        visibility: CircleVisibility? = null
    ): Circle {
        requireAdmin(circleId, requestingUserId)
        val circle = circles[circleId] ?: throw IllegalArgumentException("Circle not found.")
        val updated = circle.copy(
            name = name?.trim() ?: circle.name,
            description = description?.trim() ?: circle.description,
            visibility = visibility ?: circle.visibility
        )
        circles[circleId] = updated
        return updated
    }

    /**
     * Regenerates the circle invite code (admin only).
     */
    fun regenerateInviteCode(circleId: String, requestingUserId: String): String {
        requireAdmin(circleId, requestingUserId)
        val circle = circles[circleId] ?: throw IllegalArgumentException("Circle not found.")
        val newCode = generateInviteCode()
        circles[circleId] = circle.copy(inviteCode = newCode)
        return newCode
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private fun getActiveMembership(circleId: String, userId: String): CircleMembership? =
        memberships.find { it.circleId == circleId && it.userId == userId && it.isActive }

    private fun requireAdmin(circleId: String, userId: String) {
        val m = getActiveMembership(circleId, userId)
        require(m?.role == CircleRole.ADMIN) { "Only circle admins can perform this action." }
    }

    private var idCounter = 0L
    private fun generateId(): String = "circle_${++idCounter}_${Clock.System.now().epochSeconds}"

    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private fun generateInviteCode(): String =
        (1..INVITE_CODE_LENGTH).map { codeChars.random() }.joinToString("")
}
