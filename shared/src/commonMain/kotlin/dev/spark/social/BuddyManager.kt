package dev.spark.social

import kotlinx.datetime.*

/**
 * Manages accountability buddy pairs.
 *
 * Rules:
 *  - Maximum 3 active buddy pairs per user.
 *  - Each pair goes through an invite → accepted lifecycle.
 *  - Each pair has a configurable [SharingLevel].
 */
class BuddyManager {

    companion object {
        const val MAX_BUDDY_PAIRS = 3
    }

    enum class SharingLevel {
        /** Share nothing — presence only (both users know they're buddies). */
        MINIMAL,
        /** Share FP balance and streak only. */
        BASIC,
        /** Share daily FP summary: balance, earned, spent, streak. */
        STANDARD,
        /** Share full daily summary including app category breakdown. */
        DETAILED
    }

    enum class InviteStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        CANCELLED,
        EXPIRED
    }

    data class BuddyInvite(
        val inviteId: String,
        val fromUserId: String,
        val toUserId: String?,           // null until the invite link is claimed
        val inviteCode: String,          // short alphanumeric code the recipient enters
        val sharingLevel: SharingLevel,
        val createdAt: Instant,
        val expiresAt: Instant,
        val status: InviteStatus
    )

    data class BuddyPair(
        val pairId: String,
        val userAId: String,
        val userBId: String,
        val sharingLevel: SharingLevel,
        val createdAt: Instant,
        val isActive: Boolean = true
    )

    data class BuddySnapshot(
        val buddyUserId: String,
        val sharingLevel: SharingLevel,
        val fpBalance: Int?,        // null if not shared
        val streakDays: Int?,       // null if not shared
        val fpEarned: Int?,
        val fpSpent: Int?,
        val nutritiveMinutes: Int?,
        val emptyCalorieMinutes: Int?
    )

    // In-memory state (backed by repository in production)
    private val pairs = mutableListOf<BuddyPair>()
    private val pendingInvites = mutableListOf<BuddyInvite>()

    // -------------------------------------------------------------------------
    // Invite lifecycle
    // -------------------------------------------------------------------------

    /**
     * Creates a new buddy invite from [fromUserId].
     *
     * @throws IllegalStateException if [fromUserId] already has [MAX_BUDDY_PAIRS] active pairs.
     */
    fun createInvite(
        fromUserId: String,
        sharingLevel: SharingLevel = SharingLevel.STANDARD,
        clock: Clock = Clock.System
    ): BuddyInvite {
        val activePairs = getActivePairsForUser(fromUserId)
        check(activePairs.size < MAX_BUDDY_PAIRS) {
            "Maximum of $MAX_BUDDY_PAIRS buddy pairs reached."
        }

        val now = clock.now()
        val invite = BuddyInvite(
            inviteId = generateId(),
            fromUserId = fromUserId,
            toUserId = null,
            inviteCode = generateInviteCode(),
            sharingLevel = sharingLevel,
            createdAt = now,
            expiresAt = now.plus(48 * 3600, DateTimeUnit.SECOND),
            status = InviteStatus.PENDING
        )
        pendingInvites += invite
        return invite
    }

    /**
     * Accepts a pending invite identified by [inviteCode]. Creates the [BuddyPair].
     *
     * @throws IllegalArgumentException if the invite code is invalid or expired.
     * @throws IllegalStateException if the accepting user already has [MAX_BUDDY_PAIRS] pairs.
     */
    fun acceptInvite(
        inviteCode: String,
        acceptingUserId: String,
        clock: Clock = Clock.System
    ): BuddyPair {
        val now = clock.now()
        val invite = pendingInvites.find {
            it.inviteCode == inviteCode && it.status == InviteStatus.PENDING
        } ?: throw IllegalArgumentException("Invalid or already-used invite code.")

        require(invite.expiresAt > now) { "Invite code has expired." }
        require(invite.fromUserId != acceptingUserId) { "Cannot accept your own invite." }

        val acceptorPairs = getActivePairsForUser(acceptingUserId)
        check(acceptorPairs.size < MAX_BUDDY_PAIRS) {
            "You already have $MAX_BUDDY_PAIRS active buddy pairs."
        }

        // Expire the invite
        val index = pendingInvites.indexOf(invite)
        pendingInvites[index] = invite.copy(
            toUserId = acceptingUserId,
            status = InviteStatus.ACCEPTED
        )

        val pair = BuddyPair(
            pairId = generateId(),
            userAId = invite.fromUserId,
            userBId = acceptingUserId,
            sharingLevel = invite.sharingLevel,
            createdAt = now
        )
        pairs += pair
        return pair
    }

    /**
     * Declines a pending invite.
     */
    fun declineInvite(inviteCode: String): Boolean {
        val idx = pendingInvites.indexOfFirst {
            it.inviteCode == inviteCode && it.status == InviteStatus.PENDING
        }
        if (idx == -1) return false
        pendingInvites[idx] = pendingInvites[idx].copy(status = InviteStatus.DECLINED)
        return true
    }

    /**
     * Cancels an outgoing invite (only the sender can cancel).
     */
    fun cancelInvite(inviteId: String, requestingUserId: String): Boolean {
        val idx = pendingInvites.indexOfFirst {
            it.inviteId == inviteId && it.fromUserId == requestingUserId && it.status == InviteStatus.PENDING
        }
        if (idx == -1) return false
        pendingInvites[idx] = pendingInvites[idx].copy(status = InviteStatus.CANCELLED)
        return true
    }

    // -------------------------------------------------------------------------
    // Pair management
    // -------------------------------------------------------------------------

    /** Returns all active buddy pairs for [userId]. */
    fun getActivePairsForUser(userId: String): List<BuddyPair> =
        pairs.filter { it.isActive && (it.userAId == userId || it.userBId == userId) }

    /** Returns the buddy user ID within a pair for [userId]. */
    fun getBuddyId(pair: BuddyPair, userId: String): String =
        if (pair.userAId == userId) pair.userBId else pair.userAId

    /**
     * Updates the sharing level for an existing pair.
     * Either user in the pair may update the level.
     */
    fun updateSharingLevel(pairId: String, requestingUserId: String, newLevel: SharingLevel): BuddyPair {
        val idx = pairs.indexOfFirst { it.pairId == pairId && it.isActive }
        require(idx != -1) { "Pair not found." }
        val pair = pairs[idx]
        require(pair.userAId == requestingUserId || pair.userBId == requestingUserId) {
            "Not a member of this pair."
        }
        val updated = pair.copy(sharingLevel = newLevel)
        pairs[idx] = updated
        return updated
    }

    /**
     * Removes a buddy pair (either user may remove).
     */
    fun removePair(pairId: String, requestingUserId: String): Boolean {
        val idx = pairs.indexOfFirst {
            it.pairId == pairId && it.isActive &&
            (it.userAId == requestingUserId || it.userBId == requestingUserId)
        }
        if (idx == -1) return false
        pairs[idx] = pairs[idx].copy(isActive = false)
        return true
    }

    /**
     * Builds a [BuddySnapshot] from raw data, filtered by [sharingLevel].
     */
    fun buildSnapshot(
        buddyUserId: String,
        sharingLevel: SharingLevel,
        fpBalance: Int,
        streakDays: Int,
        fpEarned: Int,
        fpSpent: Int,
        nutritiveMinutes: Int,
        emptyCalorieMinutes: Int
    ): BuddySnapshot = BuddySnapshot(
        buddyUserId = buddyUserId,
        sharingLevel = sharingLevel,
        fpBalance = if (sharingLevel >= SharingLevel.BASIC) fpBalance else null,
        streakDays = if (sharingLevel >= SharingLevel.BASIC) streakDays else null,
        fpEarned = if (sharingLevel >= SharingLevel.STANDARD) fpEarned else null,
        fpSpent = if (sharingLevel >= SharingLevel.STANDARD) fpSpent else null,
        nutritiveMinutes = if (sharingLevel >= SharingLevel.DETAILED) nutritiveMinutes else null,
        emptyCalorieMinutes = if (sharingLevel >= SharingLevel.DETAILED) emptyCalorieMinutes else null
    )

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private var idCounter = 0L
    private fun generateId(): String = "id_${++idCounter}_${Clock.System.now().epochSeconds}"

    private val codeChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private fun generateInviteCode(): String =
        (1..6).map { codeChars.random() }.joinToString("")
}

private operator fun BuddyManager.SharingLevel.compareTo(other: BuddyManager.SharingLevel): Int =
    this.ordinal.compareTo(other.ordinal)
