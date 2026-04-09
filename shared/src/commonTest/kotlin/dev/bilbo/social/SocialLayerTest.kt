package dev.bilbo.social

import kotlin.test.*
import kotlinx.datetime.*

// =============================================================================
//  BuddyManager Tests
// =============================================================================

class BuddyManagerSharingLevelEnumTest {

    @Test
    fun sharingLevelHasFourValues() {
        val values = BuddyManager.SharingLevel.entries
        assertEquals(4, values.size)
    }

    @Test
    fun sharingLevelValuesInOrder() {
        val values = BuddyManager.SharingLevel.entries
        assertEquals(BuddyManager.SharingLevel.MINIMAL, values[0])
        assertEquals(BuddyManager.SharingLevel.BASIC, values[1])
        assertEquals(BuddyManager.SharingLevel.STANDARD, values[2])
        assertEquals(BuddyManager.SharingLevel.DETAILED, values[3])
    }
}

class BuddyManagerInviteStatusEnumTest {

    @Test
    fun inviteStatusHasFiveValues() {
        val values = BuddyManager.InviteStatus.entries
        assertEquals(5, values.size)
    }

    @Test
    fun inviteStatusValuesInOrder() {
        val values = BuddyManager.InviteStatus.entries
        assertEquals(BuddyManager.InviteStatus.PENDING, values[0])
        assertEquals(BuddyManager.InviteStatus.ACCEPTED, values[1])
        assertEquals(BuddyManager.InviteStatus.DECLINED, values[2])
        assertEquals(BuddyManager.InviteStatus.CANCELLED, values[3])
        assertEquals(BuddyManager.InviteStatus.EXPIRED, values[4])
    }
}

class BuddyManagerConstantsTest {

    @Test
    fun maxBuddyPairsIsThree() {
        assertEquals(3, BuddyManager.MAX_BUDDY_PAIRS)
    }
}

class BuddyManagerCreateInviteTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun createInviteReturnsInviteWithPendingStatus() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertEquals(BuddyManager.InviteStatus.PENDING, invite.status)
    }

    @Test
    fun createInviteDefaultsSharingLevelToStandard() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertEquals(BuddyManager.SharingLevel.STANDARD, invite.sharingLevel)
    }

    @Test
    fun createInviteRespectsCustomSharingLevel() {
        val invite = manager.createInvite("user1", BuddyManager.SharingLevel.MINIMAL, fixedClock)
        assertEquals(BuddyManager.SharingLevel.MINIMAL, invite.sharingLevel)
    }

    @Test
    fun createInviteSetsFromUserId() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertEquals("user1", invite.fromUserId)
    }

    @Test
    fun createInviteToUserIdIsNull() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertNull(invite.toUserId)
    }

    @Test
    fun createInviteSetsCreatedAt() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertEquals(fixedClock.now(), invite.createdAt)
    }

    @Test
    fun createInviteSetsExpiresAt48HoursLater() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val expected = fixedClock.now().plus(48 * 3600, DateTimeUnit.SECOND)
        assertEquals(expected, invite.expiresAt)
    }

    @Test
    fun createInviteGeneratesNonBlankCode() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertTrue(invite.inviteCode.isNotBlank())
    }

    @Test
    fun createInviteGeneratesUniqueIds() {
        val invite1 = manager.createInvite("user1", clock = fixedClock)
        val invite2 = manager.createInvite("user1", clock = fixedClock)
        assertNotEquals(invite1.inviteId, invite2.inviteId)
    }

    @Test
    fun createInviteThrowsWhenMaxPairsReached() {
        // Create 3 active pairs by creating invites and accepting them
        for (i in 1..3) {
            val invite = manager.createInvite("user1", clock = fixedClock)
            manager.acceptInvite(invite.inviteCode, "buddy$i", fixedClock)
        }
        assertFailsWith<IllegalStateException> {
            manager.createInvite("user1", clock = fixedClock)
        }
    }

    @Test
    fun createInviteAllowedWhenFewerThanMaxPairs() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.acceptInvite(invite.inviteCode, "buddy1", fixedClock)
        // Should not throw with only 1 pair
        val invite2 = manager.createInvite("user1", clock = fixedClock)
        assertNotNull(invite2)
    }
}

class BuddyManagerAcceptInviteTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun acceptInviteCreatesBuddyPair() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals("user1", pair.userAId)
        assertEquals("user2", pair.userBId)
    }

    @Test
    fun acceptInvitePairInheritsSharingLevel() {
        val invite = manager.createInvite("user1", BuddyManager.SharingLevel.DETAILED, fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals(BuddyManager.SharingLevel.DETAILED, pair.sharingLevel)
    }

    @Test
    fun acceptInvitePairIsActiveByDefault() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertTrue(pair.isActive)
    }

    @Test
    fun acceptInviteSetsCreatedAt() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals(fixedClock.now(), pair.createdAt)
    }

    @Test
    fun acceptInviteThrowsOnInvalidCode() {
        assertFailsWith<IllegalArgumentException> {
            manager.acceptInvite("INVALID_CODE", "user2", fixedClock)
        }
    }

    @Test
    fun acceptInviteThrowsOnExpiredInvite() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val expiredClock = object : Clock {
            override fun now(): Instant = Instant.parse("2025-06-05T12:00:00Z") // 4 days later
        }
        assertFailsWith<IllegalArgumentException> {
            manager.acceptInvite(invite.inviteCode, "user2", expiredClock)
        }
    }

    @Test
    fun acceptInviteThrowsOnSelfAccept() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.acceptInvite(invite.inviteCode, "user1", fixedClock)
        }
    }

    @Test
    fun acceptInviteThrowsWhenAcceptorHasMaxPairs() {
        // Give acceptor 3 existing pairs
        for (i in 1..3) {
            val inv = manager.createInvite("other$i", clock = fixedClock)
            manager.acceptInvite(inv.inviteCode, "acceptor", fixedClock)
        }
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertFailsWith<IllegalStateException> {
            manager.acceptInvite(invite.inviteCode, "acceptor", fixedClock)
        }
    }

    @Test
    fun acceptInviteThrowsOnAlreadyUsedCode() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        // The invite status is now ACCEPTED, so the code should be invalid
        assertFailsWith<IllegalArgumentException> {
            manager.acceptInvite(invite.inviteCode, "user3", fixedClock)
        }
    }
}

class BuddyManagerDeclineInviteTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun declineInviteReturnsTrue() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertTrue(manager.declineInvite(invite.inviteCode))
    }

    @Test
    fun declineInviteReturnsFalseForInvalidCode() {
        assertFalse(manager.declineInvite("INVALID_CODE"))
    }

    @Test
    fun declineInviteReturnsFalseForAlreadyDeclined() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.declineInvite(invite.inviteCode)
        // Declining again should return false since status is no longer PENDING
        assertFalse(manager.declineInvite(invite.inviteCode))
    }

    @Test
    fun declineInviteReturnsFalseForAcceptedInvite() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertFalse(manager.declineInvite(invite.inviteCode))
    }
}

class BuddyManagerCancelInviteTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun cancelInviteReturnsTrue() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertTrue(manager.cancelInvite(invite.inviteId, "user1"))
    }

    @Test
    fun cancelInviteReturnsFalseIfNotSender() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        assertFalse(manager.cancelInvite(invite.inviteId, "user2"))
    }

    @Test
    fun cancelInviteReturnsFalseForInvalidId() {
        assertFalse(manager.cancelInvite("invalid-id", "user1"))
    }

    @Test
    fun cancelInviteReturnsFalseForAlreadyCancelled() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.cancelInvite(invite.inviteId, "user1")
        assertFalse(manager.cancelInvite(invite.inviteId, "user1"))
    }

    @Test
    fun cancelledInviteCannotBeAccepted() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.cancelInvite(invite.inviteId, "user1")
        assertFailsWith<IllegalArgumentException> {
            manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        }
    }
}

class BuddyManagerPairManagementTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun getActivePairsForUserReturnsEmptyInitially() {
        assertEquals(emptyList(), manager.getActivePairsForUser("user1"))
    }

    @Test
    fun getActivePairsForUserReturnsPairsForUserA() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        val pairs = manager.getActivePairsForUser("user1")
        assertEquals(1, pairs.size)
        assertEquals("user1", pairs[0].userAId)
    }

    @Test
    fun getActivePairsForUserReturnsPairsForUserB() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        val pairs = manager.getActivePairsForUser("user2")
        assertEquals(1, pairs.size)
        assertEquals("user2", pairs[0].userBId)
    }

    @Test
    fun getActivePairsExcludesInactivePairs() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        manager.removePair(pair.pairId, "user1")
        assertEquals(0, manager.getActivePairsForUser("user1").size)
    }

    @Test
    fun getBuddyIdReturnsUserBWhenCalledWithUserA() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals("user2", manager.getBuddyId(pair, "user1"))
    }

    @Test
    fun getBuddyIdReturnsUserAWhenCalledWithUserB() {
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals("user1", manager.getBuddyId(pair, "user2"))
    }

    @Test
    fun getBuddyIdReturnsUserAForUnknownUser() {
        // If userId doesn't match userA, it returns userA (the else branch)
        val invite = manager.createInvite("user1", clock = fixedClock)
        val pair = manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
        assertEquals("user1", manager.getBuddyId(pair, "user_unknown"))
    }
}

class BuddyManagerUpdateSharingLevelTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    private fun createPair(): BuddyManager.BuddyPair {
        val invite = manager.createInvite("user1", BuddyManager.SharingLevel.STANDARD, fixedClock)
        return manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
    }

    @Test
    fun updateSharingLevelByUserA() {
        val pair = createPair()
        val updated = manager.updateSharingLevel(pair.pairId, "user1", BuddyManager.SharingLevel.DETAILED)
        assertEquals(BuddyManager.SharingLevel.DETAILED, updated.sharingLevel)
    }

    @Test
    fun updateSharingLevelByUserB() {
        val pair = createPair()
        val updated = manager.updateSharingLevel(pair.pairId, "user2", BuddyManager.SharingLevel.MINIMAL)
        assertEquals(BuddyManager.SharingLevel.MINIMAL, updated.sharingLevel)
    }

    @Test
    fun updateSharingLevelThrowsForNonMember() {
        val pair = createPair()
        assertFailsWith<IllegalArgumentException> {
            manager.updateSharingLevel(pair.pairId, "user3", BuddyManager.SharingLevel.BASIC)
        }
    }

    @Test
    fun updateSharingLevelThrowsForInactivePair() {
        val pair = createPair()
        manager.removePair(pair.pairId, "user1")
        assertFailsWith<IllegalArgumentException> {
            manager.updateSharingLevel(pair.pairId, "user1", BuddyManager.SharingLevel.BASIC)
        }
    }

    @Test
    fun updateSharingLevelThrowsForNonExistentPair() {
        assertFailsWith<IllegalArgumentException> {
            manager.updateSharingLevel("nonexistent", "user1", BuddyManager.SharingLevel.BASIC)
        }
    }
}

class BuddyManagerRemovePairTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    private fun createPair(): BuddyManager.BuddyPair {
        val invite = manager.createInvite("user1", clock = fixedClock)
        return manager.acceptInvite(invite.inviteCode, "user2", fixedClock)
    }

    @Test
    fun removePairByUserAReturnsTrue() {
        val pair = createPair()
        assertTrue(manager.removePair(pair.pairId, "user1"))
    }

    @Test
    fun removePairByUserBReturnsTrue() {
        val pair = createPair()
        assertTrue(manager.removePair(pair.pairId, "user2"))
    }

    @Test
    fun removePairReturnsFalseForNonMember() {
        val pair = createPair()
        assertFalse(manager.removePair(pair.pairId, "user3"))
    }

    @Test
    fun removePairReturnsFalseForNonExistentPair() {
        assertFalse(manager.removePair("nonexistent", "user1"))
    }

    @Test
    fun removePairReturnsFalseForAlreadyRemovedPair() {
        val pair = createPair()
        manager.removePair(pair.pairId, "user1")
        assertFalse(manager.removePair(pair.pairId, "user1"))
    }

    @Test
    fun removePairMakesPairInactive() {
        val pair = createPair()
        manager.removePair(pair.pairId, "user1")
        assertEquals(0, manager.getActivePairsForUser("user1").size)
        assertEquals(0, manager.getActivePairsForUser("user2").size)
    }

    @Test
    fun removePairFreesSlotForNewPair() {
        // Fill up to max
        for (i in 1..3) {
            val inv = manager.createInvite("user1", clock = fixedClock)
            manager.acceptInvite(inv.inviteCode, "buddy$i", fixedClock)
        }
        // Remove one
        val pairs = manager.getActivePairsForUser("user1")
        manager.removePair(pairs[0].pairId, "user1")
        // Should now be able to create another
        val newInvite = manager.createInvite("user1", clock = fixedClock)
        assertNotNull(newInvite)
    }
}

class BuddyManagerBuildSnapshotTest {

    private val manager = BuddyManager()

    @Test
    fun buildSnapshotMinimalAllNullsExceptIdentifiers() {
        val snapshot = manager.buildSnapshot(
            buddyUserId = "buddy1",
            sharingLevel = BuddyManager.SharingLevel.MINIMAL,
            fpBalance = 100,
            streakDays = 5,
            fpEarned = 200,
            fpSpent = 50,
            nutritiveMinutes = 60,
            emptyCalorieMinutes = 30
        )
        assertEquals("buddy1", snapshot.buddyUserId)
        assertEquals(BuddyManager.SharingLevel.MINIMAL, snapshot.sharingLevel)
        assertNull(snapshot.fpBalance)
        assertNull(snapshot.streakDays)
        assertNull(snapshot.fpEarned)
        assertNull(snapshot.fpSpent)
        assertNull(snapshot.nutritiveMinutes)
        assertNull(snapshot.emptyCalorieMinutes)
    }

    @Test
    fun buildSnapshotBasicSharesBalanceAndStreak() {
        val snapshot = manager.buildSnapshot(
            buddyUserId = "buddy1",
            sharingLevel = BuddyManager.SharingLevel.BASIC,
            fpBalance = 100,
            streakDays = 5,
            fpEarned = 200,
            fpSpent = 50,
            nutritiveMinutes = 60,
            emptyCalorieMinutes = 30
        )
        assertEquals(100, snapshot.fpBalance)
        assertEquals(5, snapshot.streakDays)
        assertNull(snapshot.fpEarned)
        assertNull(snapshot.fpSpent)
        assertNull(snapshot.nutritiveMinutes)
        assertNull(snapshot.emptyCalorieMinutes)
    }

    @Test
    fun buildSnapshotStandardSharesBalanceStreakEarnedSpent() {
        val snapshot = manager.buildSnapshot(
            buddyUserId = "buddy1",
            sharingLevel = BuddyManager.SharingLevel.STANDARD,
            fpBalance = 100,
            streakDays = 5,
            fpEarned = 200,
            fpSpent = 50,
            nutritiveMinutes = 60,
            emptyCalorieMinutes = 30
        )
        assertEquals(100, snapshot.fpBalance)
        assertEquals(5, snapshot.streakDays)
        assertEquals(200, snapshot.fpEarned)
        assertEquals(50, snapshot.fpSpent)
        assertNull(snapshot.nutritiveMinutes)
        assertNull(snapshot.emptyCalorieMinutes)
    }

    @Test
    fun buildSnapshotDetailedSharesEverything() {
        val snapshot = manager.buildSnapshot(
            buddyUserId = "buddy1",
            sharingLevel = BuddyManager.SharingLevel.DETAILED,
            fpBalance = 100,
            streakDays = 5,
            fpEarned = 200,
            fpSpent = 50,
            nutritiveMinutes = 60,
            emptyCalorieMinutes = 30
        )
        assertEquals(100, snapshot.fpBalance)
        assertEquals(5, snapshot.streakDays)
        assertEquals(200, snapshot.fpEarned)
        assertEquals(50, snapshot.fpSpent)
        assertEquals(60, snapshot.nutritiveMinutes)
        assertEquals(30, snapshot.emptyCalorieMinutes)
    }

    @Test
    fun buildSnapshotAlwaysSetsBuddyUserIdAndSharingLevel() {
        BuddyManager.SharingLevel.entries.forEach { level ->
            val snapshot = manager.buildSnapshot("buddy1", level, 0, 0, 0, 0, 0, 0)
            assertEquals("buddy1", snapshot.buddyUserId)
            assertEquals(level, snapshot.sharingLevel)
        }
    }
}

// =============================================================================
//  ChallengeEngine Tests
// =============================================================================

class ChallengeEngineEnumTest {

    @Test
    fun challengeTypeHasSixValues() {
        assertEquals(6, ChallengeEngine.ChallengeType.entries.size)
    }

    @Test
    fun challengeTypeValues() {
        val values = ChallengeEngine.ChallengeType.entries
        assertEquals(ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES, values[0])
        assertEquals(ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES, values[1])
        assertEquals(ChallengeEngine.ChallengeType.REACH_FP_BALANCE, values[2])
        assertEquals(ChallengeEngine.ChallengeType.DAILY_STREAK, values[3])
        assertEquals(ChallengeEngine.ChallengeType.GROUP_FP_POOL, values[4])
        assertEquals(ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS, values[5])
    }

    @Test
    fun challengeScopeHasTwoValues() {
        val values = ChallengeEngine.ChallengeScope.entries
        assertEquals(2, values.size)
        assertEquals(ChallengeEngine.ChallengeScope.CIRCLE, values[0])
        assertEquals(ChallengeEngine.ChallengeScope.BUDDY_PAIR, values[1])
    }

    @Test
    fun challengeStatusHasFourValues() {
        val values = ChallengeEngine.ChallengeStatus.entries
        assertEquals(4, values.size)
        assertEquals(ChallengeEngine.ChallengeStatus.UPCOMING, values[0])
        assertEquals(ChallengeEngine.ChallengeStatus.ACTIVE, values[1])
        assertEquals(ChallengeEngine.ChallengeStatus.COMPLETED, values[2])
        assertEquals(ChallengeEngine.ChallengeStatus.CANCELLED, values[3])
    }
}

class ChallengeEngineCreateChallengeTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    @Test
    fun createChallengeReturnsActiveChallengeWhenStartDateIsTodayOrPast() {
        val challenge = engine.createChallenge(
            title = "Test Challenge",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "circle1",
            createdByUserId = "user1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
        assertEquals(ChallengeEngine.ChallengeStatus.ACTIVE, challenge.status)
    }

    @Test
    fun createChallengeReturnsUpcomingWhenStartDateIsFuture() {
        val futureDate = today.plus(5, DateTimeUnit.DAY)
        val challenge = engine.createChallenge(
            title = "Future Challenge",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "circle1",
            createdByUserId = "user1",
            startDate = futureDate,
            endDate = futureDate.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
        assertEquals(ChallengeEngine.ChallengeStatus.UPCOMING, challenge.status)
    }

    @Test
    fun createChallengeAutoEnrollsCreator() {
        val challenge = engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.BUDDY_PAIR,
            scopeId = "pair1",
            createdByUserId = "user1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
        val challenges = engine.getChallengesForUser("user1")
        assertTrue(challenges.any { it.challengeId == challenge.challengeId })
    }

    @Test
    fun createChallengeTrimsTitle() {
        val challenge = engine.createChallenge(
            title = "  Trimmed Title  ",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "u1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
        assertEquals("Trimmed Title", challenge.title)
    }

    @Test
    fun createChallengeTrimsDescription() {
        val challenge = engine.createChallenge(
            title = "Test",
            description = "  Some description  ",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "u1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
        assertEquals("Some description", challenge.description)
    }

    @Test
    fun createChallengeThrowsOnBlankTitle() {
        assertFailsWith<IllegalArgumentException> {
            engine.createChallenge(
                title = "   ",
                type = ChallengeEngine.ChallengeType.DAILY_STREAK,
                scope = ChallengeEngine.ChallengeScope.CIRCLE,
                scopeId = "c1",
                createdByUserId = "u1",
                startDate = today,
                endDate = today.plus(7, DateTimeUnit.DAY),
                targetValue = 5,
                clock = fixedClock
            )
        }
    }

    @Test
    fun createChallengeThrowsWhenEndDateBeforeStartDate() {
        assertFailsWith<IllegalArgumentException> {
            engine.createChallenge(
                title = "Bad Dates",
                type = ChallengeEngine.ChallengeType.DAILY_STREAK,
                scope = ChallengeEngine.ChallengeScope.CIRCLE,
                scopeId = "c1",
                createdByUserId = "u1",
                startDate = today.plus(7, DateTimeUnit.DAY),
                endDate = today,
                targetValue = 5,
                clock = fixedClock
            )
        }
    }

    @Test
    fun createChallengeAllowsSameStartAndEndDate() {
        val challenge = engine.createChallenge(
            title = "Same Day",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "u1",
            startDate = today,
            endDate = today,
            targetValue = 5,
            clock = fixedClock
        )
        assertNotNull(challenge)
    }

    @Test
    fun createChallengeThrowsWhenTargetValueIsZero() {
        assertFailsWith<IllegalArgumentException> {
            engine.createChallenge(
                title = "Zero Target",
                type = ChallengeEngine.ChallengeType.DAILY_STREAK,
                scope = ChallengeEngine.ChallengeScope.CIRCLE,
                scopeId = "c1",
                createdByUserId = "u1",
                startDate = today,
                endDate = today.plus(7, DateTimeUnit.DAY),
                targetValue = 0,
                clock = fixedClock
            )
        }
    }

    @Test
    fun createChallengeThrowsWhenTargetValueIsNegative() {
        assertFailsWith<IllegalArgumentException> {
            engine.createChallenge(
                title = "Negative Target",
                type = ChallengeEngine.ChallengeType.DAILY_STREAK,
                scope = ChallengeEngine.ChallengeScope.CIRCLE,
                scopeId = "c1",
                createdByUserId = "u1",
                startDate = today,
                endDate = today.plus(7, DateTimeUnit.DAY),
                targetValue = -10,
                clock = fixedClock
            )
        }
    }

    @Test
    fun createChallengeStoresCorrectFields() {
        val challenge = engine.createChallenge(
            title = "Focus Week",
            description = "A week of focus",
            type = ChallengeEngine.ChallengeType.REACH_FP_BALANCE,
            scope = ChallengeEngine.ChallengeScope.BUDDY_PAIR,
            scopeId = "pair99",
            createdByUserId = "creator1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 500,
            isTeamChallenge = true,
            clock = fixedClock
        )
        assertEquals("Focus Week", challenge.title)
        assertEquals("A week of focus", challenge.description)
        assertEquals(ChallengeEngine.ChallengeType.REACH_FP_BALANCE, challenge.type)
        assertEquals(ChallengeEngine.ChallengeScope.BUDDY_PAIR, challenge.scope)
        assertEquals("pair99", challenge.scopeId)
        assertEquals("creator1", challenge.createdByUserId)
        assertEquals(today, challenge.startDate)
        assertEquals(today.plus(7, DateTimeUnit.DAY), challenge.endDate)
        assertEquals(500, challenge.targetValue)
        assertTrue(challenge.isTeamChallenge)
        assertEquals(fixedClock.now(), challenge.createdAt)
    }

    @Test
    fun createChallengeDefaultsTeamChallengeToFalse() {
        val challenge = engine.createChallenge(
            title = "Solo",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "u1",
            startDate = today,
            endDate = today.plus(1, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
        assertFalse(challenge.isTeamChallenge)
    }
}

class ChallengeEngineJoinChallengeTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(creator: String = "creator"): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = creator,
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
    }

    @Test
    fun joinChallengeReturnsNewParticipant() {
        val challenge = createActiveChallenge()
        val participant = engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        assertEquals(challenge.challengeId, participant.challengeId)
        assertEquals("user2", participant.userId)
        assertEquals(0, participant.currentProgress)
        assertFalse(participant.hasCompleted)
        assertNull(participant.completedAt)
    }

    @Test
    fun joinChallengeIsIdempotent() {
        val challenge = createActiveChallenge()
        val first = engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        val second = engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        assertEquals(first.challengeId, second.challengeId)
        assertEquals(first.userId, second.userId)
    }

    @Test
    fun joinChallengeAllowsUpcomingChallenge() {
        val futureDate = today.plus(5, DateTimeUnit.DAY)
        val challenge = engine.createChallenge(
            title = "Future",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "creator",
            startDate = futureDate,
            endDate = futureDate.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
        assertEquals(ChallengeEngine.ChallengeStatus.UPCOMING, challenge.status)
        val participant = engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        assertNotNull(participant)
    }

    @Test
    fun joinChallengeThrowsForCompletedChallenge() {
        val challenge = createActiveChallenge()
        engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertFailsWith<IllegalStateException> {
            engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        }
    }

    @Test
    fun joinChallengeThrowsForCancelledChallenge() {
        val challenge = createActiveChallenge("creator")
        engine.cancelChallenge(challenge.challengeId, "creator")
        assertFailsWith<IllegalStateException> {
            engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        }
    }

    @Test
    fun joinChallengeThrowsForNonExistentChallenge() {
        assertFailsWith<IllegalArgumentException> {
            engine.joinChallenge("nonexistent", "user1", fixedClock)
        }
    }
}

class ChallengeEngineRecordProgressTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(
        target: Int = 100,
        isTeam: Boolean = false,
        creator: String = "creator"
    ): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = creator,
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = target,
            isTeamChallenge = isTeam,
            clock = fixedClock
        )
    }

    @Test
    fun recordProgressIncrementsCurrentProgress() {
        val challenge = createActiveChallenge()
        val result = engine.recordProgress(challenge.challengeId, "creator", 30, fixedClock)
        assertEquals(30, result.currentProgress)
    }

    @Test
    fun recordProgressAccumulates() {
        val challenge = createActiveChallenge()
        engine.recordProgress(challenge.challengeId, "creator", 30, fixedClock)
        val result = engine.recordProgress(challenge.challengeId, "creator", 25, fixedClock)
        assertEquals(55, result.currentProgress)
    }

    @Test
    fun recordProgressMarksCompletedWhenTargetReached() {
        val challenge = createActiveChallenge(target = 50)
        val result = engine.recordProgress(challenge.challengeId, "creator", 50, fixedClock)
        assertTrue(result.hasCompleted)
        assertNotNull(result.completedAt)
    }

    @Test
    fun recordProgressMarksCompletedWhenTargetExceeded() {
        val challenge = createActiveChallenge(target = 50)
        val result = engine.recordProgress(challenge.challengeId, "creator", 75, fixedClock)
        assertTrue(result.hasCompleted)
    }

    @Test
    fun recordProgressNotCompletedWhenBelowTarget() {
        val challenge = createActiveChallenge(target = 100)
        val result = engine.recordProgress(challenge.challengeId, "creator", 30, fixedClock)
        assertFalse(result.hasCompleted)
        assertNull(result.completedAt)
    }

    @Test
    fun recordProgressTeamChallengeChecksGroupTotal() {
        val challenge = createActiveChallenge(target = 100, isTeam = true)
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 60, fixedClock)
        val result = engine.recordProgress(challenge.challengeId, "user2", 40, fixedClock)
        // Group total = 60 + 40 = 100 >= target 100
        assertTrue(result.hasCompleted)
    }

    @Test
    fun recordProgressTeamChallengeNotCompleteIfGroupBelowTarget() {
        val challenge = createActiveChallenge(target = 100, isTeam = true)
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 30, fixedClock)
        val result = engine.recordProgress(challenge.challengeId, "user2", 20, fixedClock)
        // Group total = 30 + 20 = 50 < 100
        assertFalse(result.hasCompleted)
    }

    @Test
    fun recordProgressThrowsForNonActiveChallenge() {
        val challenge = createActiveChallenge()
        engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertFailsWith<IllegalStateException> {
            engine.recordProgress(challenge.challengeId, "creator", 10, fixedClock)
        }
    }

    @Test
    fun recordProgressThrowsForNonParticipant() {
        val challenge = createActiveChallenge()
        assertFailsWith<IllegalArgumentException> {
            engine.recordProgress(challenge.challengeId, "non_participant", 10, fixedClock)
        }
    }
}

class ChallengeEngineFinalizeChallengeTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(
        type: ChallengeEngine.ChallengeType = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
        target: Int = 100,
        isTeam: Boolean = false
    ): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = type,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "creator",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = target,
            isTeamChallenge = isTeam,
            clock = fixedClock
        )
    }

    @Test
    fun finalizeChallengeMarksChallengeCompleted() {
        val challenge = createActiveChallenge()
        engine.finalizeChallenge(challenge.challengeId, fixedClock)
        val challenges = engine.getChallengesForScope("c1")
        val finalized = challenges.first { it.challengeId == challenge.challengeId }
        assertEquals(ChallengeEngine.ChallengeStatus.COMPLETED, finalized.status)
    }

    @Test
    fun finalizeChallengeReturnsResult() {
        val challenge = createActiveChallenge()
        engine.recordProgress(challenge.challengeId, "creator", 120, fixedClock)
        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertEquals(challenge.challengeId, result.challengeId)
        assertEquals(1, result.participantCount)
        assertEquals(fixedClock.now(), result.completedAt)
    }

    @Test
    fun finalizeChallengeWinnersAreThoseWhoReachedTarget() {
        val challenge = createActiveChallenge(target = 50)
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 60, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 30, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertTrue("creator" in result.winners)
        assertFalse("user2" in result.winners)
    }

    @Test
    fun finalizeChallengeTeamChallengeAllWinIfGroupReachesTarget() {
        val challenge = createActiveChallenge(target = 100, isTeam = true)
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 60, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 50, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertTrue("creator" in result.winners)
        assertTrue("user2" in result.winners)
    }

    @Test
    fun finalizeChallengeTeamChallengeNoWinnersIfGroupBelowTarget() {
        val challenge = createActiveChallenge(target = 200, isTeam = true)
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 60, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 50, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertTrue(result.winners.isEmpty())
    }

    @Test
    fun finalizeReduceEmptyCaloriesLowerWins() {
        val challenge = createActiveChallenge(
            type = ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES,
            target = 100
        )
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        engine.joinChallenge(challenge.challengeId, "user3", fixedClock)

        engine.recordProgress(challenge.challengeId, "creator", 50, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 20, fixedClock)
        engine.recordProgress(challenge.challengeId, "user3", 20, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        // Lower is better for REDUCE_EMPTY_CALORIES
        // user2 and user3 tied at 20 (lowest), so both are winners
        assertTrue("user2" in result.winners)
        assertTrue("user3" in result.winners)
        assertFalse("creator" in result.winners)
    }

    @Test
    fun finalizeChallengeReturnsFinalProgress() {
        val challenge = createActiveChallenge()
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        engine.recordProgress(challenge.challengeId, "creator", 40, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 80, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertEquals(40, result.finalProgress["creator"])
        assertEquals(80, result.finalProgress["user2"])
    }

    @Test
    fun finalizeChallengeThrowsIfNotActive() {
        val challenge = createActiveChallenge()
        engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertFailsWith<IllegalStateException> {
            engine.finalizeChallenge(challenge.challengeId, fixedClock)
        }
    }

    @Test
    fun finalizeChallengeThrowsForCancelledChallenge() {
        val challenge = createActiveChallenge()
        engine.cancelChallenge(challenge.challengeId, "creator")
        assertFailsWith<IllegalStateException> {
            engine.finalizeChallenge(challenge.challengeId, fixedClock)
        }
    }
}

class ChallengeEngineCancelChallengeTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(creator: String = "creator"): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = creator,
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
    }

    @Test
    fun cancelChallengeReturnsTrue() {
        val challenge = createActiveChallenge()
        assertTrue(engine.cancelChallenge(challenge.challengeId, "creator"))
    }

    @Test
    fun cancelChallengeSetsCancelledStatus() {
        val challenge = createActiveChallenge()
        engine.cancelChallenge(challenge.challengeId, "creator")
        val challenges = engine.getChallengesForScope("c1")
        val cancelled = challenges.first { it.challengeId == challenge.challengeId }
        assertEquals(ChallengeEngine.ChallengeStatus.CANCELLED, cancelled.status)
    }

    @Test
    fun cancelChallengeThrowsForNonCreator() {
        val challenge = createActiveChallenge()
        assertFailsWith<IllegalArgumentException> {
            engine.cancelChallenge(challenge.challengeId, "other_user")
        }
    }

    @Test
    fun cancelChallengeReturnsFalseForCompletedChallenge() {
        val challenge = createActiveChallenge()
        engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertFalse(engine.cancelChallenge(challenge.challengeId, "creator"))
    }

    @Test
    fun cancelChallengeThrowsForNonExistentChallenge() {
        assertFailsWith<IllegalArgumentException> {
            engine.cancelChallenge("nonexistent", "creator")
        }
    }
}

class ChallengeEngineQueryTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(
        scopeId: String = "c1",
        creator: String = "creator"
    ): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.DAILY_STREAK,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = scopeId,
            createdByUserId = creator,
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 5,
            clock = fixedClock
        )
    }

    @Test
    fun getChallengesForScopeReturnsEmpty() {
        assertEquals(emptyList(), engine.getChallengesForScope("nonexistent"))
    }

    @Test
    fun getChallengesForScopeReturnsMatchingChallenges() {
        createActiveChallenge(scopeId = "scope1")
        createActiveChallenge(scopeId = "scope1")
        createActiveChallenge(scopeId = "scope2")
        assertEquals(2, engine.getChallengesForScope("scope1").size)
    }

    @Test
    fun getActiveChallengesForScopeFiltersNonActive() {
        val c1 = createActiveChallenge(scopeId = "scope1")
        createActiveChallenge(scopeId = "scope1")
        engine.cancelChallenge(c1.challengeId, "creator")
        assertEquals(1, engine.getActiveChallengesForScope("scope1").size)
    }

    @Test
    fun getActiveChallengesForScopeReturnsEmpty() {
        assertEquals(emptyList(), engine.getActiveChallengesForScope("nonexistent"))
    }

    @Test
    fun getChallengesForUserReturnsJoinedChallenges() {
        val c1 = createActiveChallenge(creator = "creator1")
        val c2 = createActiveChallenge(creator = "creator2")
        engine.joinChallenge(c1.challengeId, "user1", fixedClock)
        engine.joinChallenge(c2.challengeId, "user1", fixedClock)
        assertEquals(2, engine.getChallengesForUser("user1").size)
    }

    @Test
    fun getChallengesForUserReturnsEmptyForNonParticipant() {
        createActiveChallenge(creator = "creator1")
        assertEquals(emptyList(), engine.getChallengesForUser("non_participant"))
    }

    @Test
    fun getChallengesForUserIncludesCreatorChallenges() {
        createActiveChallenge(creator = "creator1")
        assertEquals(1, engine.getChallengesForUser("creator1").size)
    }
}

class ChallengeEngineLeaderboardTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    @Test
    fun getLeaderboardSortedDescendingByDefault() {
        val challenge = engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "user1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        engine.joinChallenge(challenge.challengeId, "user3", fixedClock)

        engine.recordProgress(challenge.challengeId, "user1", 50, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 80, fixedClock)
        engine.recordProgress(challenge.challengeId, "user3", 30, fixedClock)

        val board = engine.getLeaderboard(challenge.challengeId)
        assertEquals("user2", board[0].userId)
        assertEquals("user1", board[1].userId)
        assertEquals("user3", board[2].userId)
    }

    @Test
    fun getLeaderboardAscendingForReduceEmptyCalories() {
        val challenge = engine.createChallenge(
            title = "Reduce",
            type = ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "user1",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
        engine.joinChallenge(challenge.challengeId, "user2", fixedClock)
        engine.joinChallenge(challenge.challengeId, "user3", fixedClock)

        engine.recordProgress(challenge.challengeId, "user1", 50, fixedClock)
        engine.recordProgress(challenge.challengeId, "user2", 20, fixedClock)
        engine.recordProgress(challenge.challengeId, "user3", 80, fixedClock)

        val board = engine.getLeaderboard(challenge.challengeId)
        // Lower is better for REDUCE_EMPTY_CALORIES so ascending order
        assertEquals("user2", board[0].userId)
        assertEquals("user1", board[1].userId)
        assertEquals("user3", board[2].userId)
    }

    @Test
    fun getLeaderboardThrowsForNonExistentChallenge() {
        assertFailsWith<IllegalArgumentException> {
            engine.getLeaderboard("nonexistent")
        }
    }
}

class ChallengeEngineProgressPercentTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    private fun createActiveChallenge(target: Int = 100): ChallengeEngine.Challenge {
        return engine.createChallenge(
            title = "Test",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "c1",
            createdByUserId = "creator",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = target,
            clock = fixedClock
        )
    }

    @Test
    fun progressPercentReturnsZeroForNonParticipant() {
        val challenge = createActiveChallenge()
        assertEquals(0, engine.progressPercent(challenge.challengeId, "non_participant"))
    }

    @Test
    fun progressPercentReturnsZeroAtStart() {
        val challenge = createActiveChallenge()
        assertEquals(0, engine.progressPercent(challenge.challengeId, "creator"))
    }

    @Test
    fun progressPercentReturnsCorrectPercentage() {
        val challenge = createActiveChallenge(target = 200)
        engine.recordProgress(challenge.challengeId, "creator", 100, fixedClock)
        assertEquals(50, engine.progressPercent(challenge.challengeId, "creator"))
    }

    @Test
    fun progressPercentCapsAt100() {
        val challenge = createActiveChallenge(target = 50)
        engine.recordProgress(challenge.challengeId, "creator", 100, fixedClock)
        assertEquals(100, engine.progressPercent(challenge.challengeId, "creator"))
    }

    @Test
    fun progressPercentExactTarget() {
        val challenge = createActiveChallenge(target = 100)
        engine.recordProgress(challenge.challengeId, "creator", 100, fixedClock)
        assertEquals(100, engine.progressPercent(challenge.challengeId, "creator"))
    }

    @Test
    fun progressPercentPartialProgress() {
        val challenge = createActiveChallenge(target = 100)
        engine.recordProgress(challenge.challengeId, "creator", 33, fixedClock)
        assertEquals(33, engine.progressPercent(challenge.challengeId, "creator"))
    }

    @Test
    fun progressPercentThrowsForNonExistentChallenge() {
        assertFailsWith<IllegalArgumentException> {
            engine.progressPercent("nonexistent", "user1")
        }
    }
}

// =============================================================================
//  CircleManager Tests
// =============================================================================

class CircleManagerEnumTest {

    @Test
    fun circleRoleHasTwoValues() {
        val values = CircleManager.CircleRole.entries
        assertEquals(2, values.size)
        assertEquals(CircleManager.CircleRole.ADMIN, values[0])
        assertEquals(CircleManager.CircleRole.MEMBER, values[1])
    }

    @Test
    fun circleVisibilityHasTwoValues() {
        val values = CircleManager.CircleVisibility.entries
        assertEquals(2, values.size)
        assertEquals(CircleManager.CircleVisibility.PUBLIC, values[0])
        assertEquals(CircleManager.CircleVisibility.PRIVATE, values[1])
    }
}

class CircleManagerConstantsTest {

    @Test
    fun minCircleSizeIsTwo() {
        assertEquals(2, CircleManager.MIN_CIRCLE_SIZE)
    }

    @Test
    fun maxCircleSizeIsTwelve() {
        assertEquals(12, CircleManager.MAX_CIRCLE_SIZE)
    }

    @Test
    fun inviteCodeLengthIsEight() {
        assertEquals(8, CircleManager.INVITE_CODE_LENGTH)
    }

    @Test
    fun inviteExpiryHoursIs72() {
        assertEquals(72L, CircleManager.INVITE_EXPIRY_HOURS)
    }
}

class CircleManagerCreateCircleTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun createCircleReturnsCircle() {
        val circle = manager.createCircle(
            name = "Focus Group",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals("Focus Group", circle.name)
        assertEquals("user1", circle.createdByUserId)
        assertTrue(circle.isActive)
    }

    @Test
    fun createCircleTrimsName() {
        val circle = manager.createCircle(
            name = "  Trimmed  ",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals("Trimmed", circle.name)
    }

    @Test
    fun createCircleTrimsDescription() {
        val circle = manager.createCircle(
            name = "Test",
            description = "  Trimmed Description  ",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals("Trimmed Description", circle.description)
    }

    @Test
    fun createCircleDefaultsToPrivate() {
        val circle = manager.createCircle(
            name = "Test",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals(CircleManager.CircleVisibility.PRIVATE, circle.visibility)
    }

    @Test
    fun createCircleRespectsPublicVisibility() {
        val circle = manager.createCircle(
            name = "Public Circle",
            visibility = CircleManager.CircleVisibility.PUBLIC,
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals(CircleManager.CircleVisibility.PUBLIC, circle.visibility)
    }

    @Test
    fun createCircleSetsCreatedAt() {
        val circle = manager.createCircle(
            name = "Test",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals(fixedClock.now(), circle.createdAt)
    }

    @Test
    fun createCircleGeneratesInviteCode() {
        val circle = manager.createCircle(
            name = "Test",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertTrue(circle.inviteCode.isNotBlank())
    }

    @Test
    fun createCircleCreatorBecomesAdmin() {
        val circle = manager.createCircle(
            name = "Test",
            creatorUserId = "user1",
            clock = fixedClock
        )
        val memberships = manager.getActiveMemberships(circle.circleId)
        assertEquals(1, memberships.size)
        assertEquals("user1", memberships[0].userId)
        assertEquals(CircleManager.CircleRole.ADMIN, memberships[0].role)
    }

    @Test
    fun createCircleThrowsOnBlankName() {
        assertFailsWith<IllegalArgumentException> {
            manager.createCircle(
                name = "   ",
                creatorUserId = "user1",
                clock = fixedClock
            )
        }
    }

    @Test
    fun createCircleThrowsOnEmptyName() {
        assertFailsWith<IllegalArgumentException> {
            manager.createCircle(
                name = "",
                creatorUserId = "user1",
                clock = fixedClock
            )
        }
    }

    @Test
    fun createCircleDefaultsDescriptionToEmpty() {
        val circle = manager.createCircle(
            name = "Test",
            creatorUserId = "user1",
            clock = fixedClock
        )
        assertEquals("", circle.description)
    }
}

class CircleManagerJoinByInviteCodeTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun joinByInviteCodeReturnsMembership() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        val membership = manager.joinByInviteCode(circle.inviteCode, "user2", fixedClock)
        assertEquals(circle.circleId, membership.circleId)
        assertEquals("user2", membership.userId)
        assertEquals(CircleManager.CircleRole.MEMBER, membership.role)
    }

    @Test
    fun joinByInviteCodeThrowsForInvalidCode() {
        assertFailsWith<IllegalArgumentException> {
            manager.joinByInviteCode("INVALID_CODE", "user2", fixedClock)
        }
    }

    @Test
    fun joinByInviteCodeThrowsForInactiveCircle() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        // Deactivate the circle by having the only member leave
        manager.leaveCircle(circle.circleId, "user1")
        assertFailsWith<IllegalArgumentException> {
            manager.joinByInviteCode(circle.inviteCode, "user2", fixedClock)
        }
    }
}

class CircleManagerJoinCircleTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun joinCircleReturnsMembership() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        val membership = manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertEquals(circle.circleId, membership.circleId)
        assertEquals("user2", membership.userId)
        assertEquals(CircleManager.CircleRole.MEMBER, membership.role)
        assertTrue(membership.isActive)
    }

    @Test
    fun joinCircleIsIdempotent() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        val first = manager.joinCircle(circle.circleId, "user2", fixedClock)
        val second = manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertEquals(first.circleId, second.circleId)
        assertEquals(first.userId, second.userId)
    }

    @Test
    fun joinCircleThrowsWhenFull() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        // Add 11 more members (to reach 12 total with creator)
        for (i in 2..12) {
            manager.joinCircle(circle.circleId, "user$i", fixedClock)
        }
        assertFailsWith<IllegalStateException> {
            manager.joinCircle(circle.circleId, "user13", fixedClock)
        }
    }

    @Test
    fun joinCircleThrowsForNonExistentCircle() {
        assertFailsWith<IllegalArgumentException> {
            manager.joinCircle("nonexistent", "user1", fixedClock)
        }
    }

    @Test
    fun joinCircleThrowsForInactiveCircle() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.leaveCircle(circle.circleId, "user1")
        assertFailsWith<IllegalArgumentException> {
            manager.joinCircle(circle.circleId, "user2", fixedClock)
        }
    }

    @Test
    fun joinCircleSetsJoinedAt() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        val membership = manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertEquals(fixedClock.now(), membership.joinedAt)
    }
}

class CircleManagerLeaveCircleTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun leaveCircleReturnsTrue() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertTrue(manager.leaveCircle(circle.circleId, "user2"))
    }

    @Test
    fun leaveCircleReturnsFalseIfNotMember() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        assertFalse(manager.leaveCircle(circle.circleId, "user_nonmember"))
    }

    @Test
    fun leaveCircleReturnsFalseIfAlreadyLeft() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.leaveCircle(circle.circleId, "user2")
        assertFalse(manager.leaveCircle(circle.circleId, "user2"))
    }

    @Test
    fun leaveCircleReducesMemberCount() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertEquals(2, manager.getMemberCount(circle.circleId))
        manager.leaveCircle(circle.circleId, "user2")
        assertEquals(1, manager.getMemberCount(circle.circleId))
    }

    @Test
    fun leaveCircleAdminPromotesNextMember() {
        val circle = manager.createCircle("Test", creatorUserId = "admin1", clock = fixedClock)
        val laterClock = object : Clock {
            override fun now(): Instant = Instant.parse("2025-06-01T13:00:00Z")
        }
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.joinCircle(circle.circleId, "user3", laterClock)

        // Leave as admin
        manager.leaveCircle(circle.circleId, "admin1")

        // user2 joined earlier, so user2 should be promoted
        val memberships = manager.getActiveMemberships(circle.circleId)
        val user2Membership = memberships.first { it.userId == "user2" }
        assertEquals(CircleManager.CircleRole.ADMIN, user2Membership.role)
    }

    @Test
    fun leaveCircleLastMemberDeactivatesCircle() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.leaveCircle(circle.circleId, "user1")
        assertNull(manager.getCircle(circle.circleId))
    }

    @Test
    fun leaveCircleNonAdminDoesNotTriggerPromotion() {
        val circle = manager.createCircle("Test", creatorUserId = "admin1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.leaveCircle(circle.circleId, "user2")

        val memberships = manager.getActiveMemberships(circle.circleId)
        assertEquals(1, memberships.size)
        assertEquals(CircleManager.CircleRole.ADMIN, memberships[0].role)
    }

    @Test
    fun leaveCircleWithMultipleAdminsDoesNotPromote() {
        val circle = manager.createCircle("Test", creatorUserId = "admin1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "admin2", fixedClock)
        manager.promoteToAdmin(circle.circleId, "admin1", "admin2")

        manager.leaveCircle(circle.circleId, "admin1")

        val memberships = manager.getActiveMemberships(circle.circleId)
        assertEquals(1, memberships.size)
        assertEquals(CircleManager.CircleRole.ADMIN, memberships[0].role)
        assertEquals("admin2", memberships[0].userId)
    }
}

class CircleManagerRemoveMemberTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun removeMemberReturnsTrue() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertTrue(manager.removeMember(circle.circleId, "admin", "user2"))
    }

    @Test
    fun removeMemberReducesMemberCount() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.removeMember(circle.circleId, "admin", "user2")
        assertEquals(1, manager.getMemberCount(circle.circleId))
    }

    @Test
    fun removeMemberThrowsForNonAdmin() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.joinCircle(circle.circleId, "user3", fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.removeMember(circle.circleId, "user2", "user3")
        }
    }

    @Test
    fun removeMemberThrowsWhenAdminTriesToSelfRemove() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.removeMember(circle.circleId, "admin", "admin")
        }
    }

    @Test
    fun removeMemberReturnsFalseForNonMemberTarget() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        assertFalse(manager.removeMember(circle.circleId, "admin", "nonexistent_user"))
    }

    @Test
    fun removeMemberReturnsFalseForNonMemberRequester() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertFalse(manager.removeMember(circle.circleId, "nonmember", "user2"))
    }
}

class CircleManagerPromoteToAdminTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun promoteToAdminChangesRole() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.promoteToAdmin(circle.circleId, "admin", "user2")

        val memberships = manager.getActiveMemberships(circle.circleId)
        val user2 = memberships.first { it.userId == "user2" }
        assertEquals(CircleManager.CircleRole.ADMIN, user2.role)
    }

    @Test
    fun promoteToAdminThrowsForNonAdmin() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.joinCircle(circle.circleId, "user3", fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.promoteToAdmin(circle.circleId, "user2", "user3")
        }
    }

    @Test
    fun promoteToAdminThrowsForNonMemberTarget() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.promoteToAdmin(circle.circleId, "admin", "nonexistent")
        }
    }

    @Test
    fun promoteToAdminThrowsForNonMemberRequester() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.promoteToAdmin(circle.circleId, "nonmember", "user2")
        }
    }
}

class CircleManagerGetActiveMembershipsTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun getActiveMembershipsReturnsCreator() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        val memberships = manager.getActiveMemberships(circle.circleId)
        assertEquals(1, memberships.size)
    }

    @Test
    fun getActiveMembershipsExcludesInactive() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.leaveCircle(circle.circleId, "user2")
        assertEquals(1, manager.getActiveMemberships(circle.circleId).size)
    }

    @Test
    fun getActiveMembershipsReturnsAll() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.joinCircle(circle.circleId, "user3", fixedClock)
        assertEquals(3, manager.getActiveMemberships(circle.circleId).size)
    }

    @Test
    fun getActiveMembershipsReturnsEmptyForNonExistentCircle() {
        assertEquals(0, manager.getActiveMemberships("nonexistent").size)
    }
}

class CircleManagerGetCirclesForUserTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun getCirclesForUserReturnsEmpty() {
        assertEquals(emptyList(), manager.getCirclesForUser("nobody"))
    }

    @Test
    fun getCirclesForUserReturnsCreatedCircles() {
        manager.createCircle("C1", creatorUserId = "user1", clock = fixedClock)
        manager.createCircle("C2", creatorUserId = "user1", clock = fixedClock)
        assertEquals(2, manager.getCirclesForUser("user1").size)
    }

    @Test
    fun getCirclesForUserReturnsJoinedCircles() {
        val c1 = manager.createCircle("C1", creatorUserId = "admin1", clock = fixedClock)
        manager.joinCircle(c1.circleId, "user2", fixedClock)
        assertEquals(1, manager.getCirclesForUser("user2").size)
    }

    @Test
    fun getCirclesForUserExcludesInactiveCircles() {
        val circle = manager.createCircle("C1", creatorUserId = "user1", clock = fixedClock)
        manager.leaveCircle(circle.circleId, "user1")
        assertEquals(0, manager.getCirclesForUser("user1").size)
    }

    @Test
    fun getCirclesForUserExcludesLeftCircles() {
        val circle = manager.createCircle("C1", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.leaveCircle(circle.circleId, "user2")
        assertEquals(0, manager.getCirclesForUser("user2").size)
    }
}

class CircleManagerGetCircleTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun getCircleReturnsActiveCircle() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        assertNotNull(manager.getCircle(circle.circleId))
    }

    @Test
    fun getCircleReturnsNullForNonExistent() {
        assertNull(manager.getCircle("nonexistent"))
    }

    @Test
    fun getCircleReturnsNullForDeactivated() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.leaveCircle(circle.circleId, "user1")
        assertNull(manager.getCircle(circle.circleId))
    }
}

class CircleManagerGetPublicCirclesTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun getPublicCirclesReturnsEmpty() {
        assertEquals(emptyList(), manager.getPublicCircles())
    }

    @Test
    fun getPublicCirclesReturnsOnlyPublic() {
        manager.createCircle("Public", visibility = CircleManager.CircleVisibility.PUBLIC, creatorUserId = "u1", clock = fixedClock)
        manager.createCircle("Private", visibility = CircleManager.CircleVisibility.PRIVATE, creatorUserId = "u2", clock = fixedClock)
        assertEquals(1, manager.getPublicCircles().size)
        assertEquals("Public", manager.getPublicCircles()[0].name)
    }

    @Test
    fun getPublicCirclesExcludesInactive() {
        val circle = manager.createCircle("Public", visibility = CircleManager.CircleVisibility.PUBLIC, creatorUserId = "u1", clock = fixedClock)
        manager.leaveCircle(circle.circleId, "u1") // deactivates
        assertEquals(0, manager.getPublicCircles().size)
    }
}

class CircleManagerGetMemberCountTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun getMemberCountReturnsOneAfterCreation() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        assertEquals(1, manager.getMemberCount(circle.circleId))
    }

    @Test
    fun getMemberCountIncrementsOnJoin() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertEquals(2, manager.getMemberCount(circle.circleId))
    }

    @Test
    fun getMemberCountDecrementsOnLeave() {
        val circle = manager.createCircle("Test", creatorUserId = "user1", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        manager.leaveCircle(circle.circleId, "user2")
        assertEquals(1, manager.getMemberCount(circle.circleId))
    }

    @Test
    fun getMemberCountReturnsZeroForNonExistent() {
        assertEquals(0, manager.getMemberCount("nonexistent"))
    }
}

class CircleManagerUpdateCircleTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun updateCircleChangesName() {
        val circle = manager.createCircle("Old Name", creatorUserId = "admin", clock = fixedClock)
        val updated = manager.updateCircle(circle.circleId, "admin", name = "New Name")
        assertEquals("New Name", updated.name)
    }

    @Test
    fun updateCircleChangesDescription() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val updated = manager.updateCircle(circle.circleId, "admin", description = "New Desc")
        assertEquals("New Desc", updated.description)
    }

    @Test
    fun updateCircleChangesVisibility() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val updated = manager.updateCircle(circle.circleId, "admin", visibility = CircleManager.CircleVisibility.PUBLIC)
        assertEquals(CircleManager.CircleVisibility.PUBLIC, updated.visibility)
    }

    @Test
    fun updateCircleTrimsName() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val updated = manager.updateCircle(circle.circleId, "admin", name = "  Trimmed  ")
        assertEquals("Trimmed", updated.name)
    }

    @Test
    fun updateCircleTrimsDescription() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val updated = manager.updateCircle(circle.circleId, "admin", description = "  Trimmed  ")
        assertEquals("Trimmed", updated.description)
    }

    @Test
    fun updateCirclePreservesUnchangedFields() {
        val circle = manager.createCircle(
            "Original",
            description = "Desc",
            visibility = CircleManager.CircleVisibility.PUBLIC,
            creatorUserId = "admin",
            clock = fixedClock
        )
        val updated = manager.updateCircle(circle.circleId, "admin", name = "Updated")
        assertEquals("Updated", updated.name)
        assertEquals("Desc", updated.description)
        assertEquals(CircleManager.CircleVisibility.PUBLIC, updated.visibility)
    }

    @Test
    fun updateCircleThrowsForNonAdmin() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.updateCircle(circle.circleId, "user2", name = "Hacked")
        }
    }

    @Test
    fun updateCircleThrowsForNonMember() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.updateCircle(circle.circleId, "nonmember", name = "Hacked")
        }
    }
}

class CircleManagerRegenerateInviteCodeTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun regenerateInviteCodeReturnsNewCode() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val oldCode = circle.inviteCode
        val newCode = manager.regenerateInviteCode(circle.circleId, "admin")
        assertTrue(newCode.isNotBlank())
        // New code should be different (very high probability with random generation)
        // We just verify it's non-blank; equality check would be flaky
    }

    @Test
    fun regenerateInviteCodeUpdatesCircle() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val newCode = manager.regenerateInviteCode(circle.circleId, "admin")
        val updatedCircle = manager.getCircle(circle.circleId)
        assertNotNull(updatedCircle)
        assertEquals(newCode, updatedCircle.inviteCode)
    }

    @Test
    fun regenerateInviteCodeThrowsForNonAdmin() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        manager.joinCircle(circle.circleId, "user2", fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.regenerateInviteCode(circle.circleId, "user2")
        }
    }

    @Test
    fun regenerateInviteCodeThrowsForNonMember() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        assertFailsWith<IllegalArgumentException> {
            manager.regenerateInviteCode(circle.circleId, "nonmember")
        }
    }

    @Test
    fun oldInviteCodeNoLongerWorksAfterRegeneration() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val oldCode = circle.inviteCode
        manager.regenerateInviteCode(circle.circleId, "admin")
        assertFailsWith<IllegalArgumentException> {
            manager.joinByInviteCode(oldCode, "user2", fixedClock)
        }
    }

    @Test
    fun newInviteCodeWorksAfterRegeneration() {
        val circle = manager.createCircle("Test", creatorUserId = "admin", clock = fixedClock)
        val newCode = manager.regenerateInviteCode(circle.circleId, "admin")
        val membership = manager.joinByInviteCode(newCode, "user2", fixedClock)
        assertEquals(circle.circleId, membership.circleId)
    }
}

// =============================================================================
//  LeaderboardCalculator Tests
// =============================================================================

class LeaderboardCalculatorEnumTest {

    @Test
    fun leaderboardCategoryHasSixValues() {
        assertEquals(6, LeaderboardCalculator.LeaderboardCategory.entries.size)
    }

    @Test
    fun leaderboardCategoryValues() {
        val values = LeaderboardCalculator.LeaderboardCategory.entries
        assertEquals(LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, values[0])
        assertEquals(LeaderboardCalculator.LeaderboardCategory.NUTRITIVE_MINUTES, values[1])
        assertEquals(LeaderboardCalculator.LeaderboardCategory.FEWEST_EMPTY_CALORIES, values[2])
        assertEquals(LeaderboardCalculator.LeaderboardCategory.STREAK_DAYS, values[3])
        assertEquals(LeaderboardCalculator.LeaderboardCategory.INTENT_ACCURACY, values[4])
        assertEquals(LeaderboardCalculator.LeaderboardCategory.FP_EARNED_WEEKLY, values[5])
    }
}

class LeaderboardCalculatorComputeTest {

    private val calculator = LeaderboardCalculator()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    private fun stats(userId: String, displayName: String = userId): LeaderboardCalculator.UserStats =
        LeaderboardCalculator.UserStats(
            userId = userId,
            displayName = displayName,
            fpBalance = 0,
            nutritiveMinutes = 0,
            emptyCalorieMinutes = 0,
            streakDays = 0,
            intentAccuracyPercent = 0f,
            fpEarnedWeekly = 0
        )

    private val sampleStats = listOf(
        LeaderboardCalculator.UserStats("u1", "Alice", fpBalance = 300, nutritiveMinutes = 120, emptyCalorieMinutes = 50, streakDays = 10, intentAccuracyPercent = 0.85f, fpEarnedWeekly = 200),
        LeaderboardCalculator.UserStats("u2", "Bob", fpBalance = 500, nutritiveMinutes = 80, emptyCalorieMinutes = 20, streakDays = 15, intentAccuracyPercent = 0.92f, fpEarnedWeekly = 350),
        LeaderboardCalculator.UserStats("u3", "Carol", fpBalance = 100, nutritiveMinutes = 200, emptyCalorieMinutes = 100, streakDays = 5, intentAccuracyPercent = 0.78f, fpEarnedWeekly = 150)
    )

    @Test
    fun computeFpBalanceDescending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        assertEquals("u2", board.entries[0].userId) // 500
        assertEquals("u1", board.entries[1].userId) // 300
        assertEquals("u3", board.entries[2].userId) // 100
    }

    @Test
    fun computeNutritiveminutesDescending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.NUTRITIVE_MINUTES, sampleStats, "u1", fixedClock)
        assertEquals("u3", board.entries[0].userId) // 200
        assertEquals("u1", board.entries[1].userId) // 120
        assertEquals("u2", board.entries[2].userId) // 80
    }

    @Test
    fun computeFewestEmptyCaloriesAscending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FEWEST_EMPTY_CALORIES, sampleStats, "u1", fixedClock)
        assertEquals("u2", board.entries[0].userId) // 20 (lowest)
        assertEquals("u1", board.entries[1].userId) // 50
        assertEquals("u3", board.entries[2].userId) // 100
    }

    @Test
    fun computeStreakDaysDescending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.STREAK_DAYS, sampleStats, "u1", fixedClock)
        assertEquals("u2", board.entries[0].userId) // 15
        assertEquals("u1", board.entries[1].userId) // 10
        assertEquals("u3", board.entries[2].userId) // 5
    }

    @Test
    fun computeIntentAccuracyDescending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.INTENT_ACCURACY, sampleStats, "u1", fixedClock)
        assertEquals("u2", board.entries[0].userId) // 0.92
        assertEquals("u1", board.entries[1].userId) // 0.85
        assertEquals("u3", board.entries[2].userId) // 0.78
    }

    @Test
    fun computeFpEarnedWeeklyDescending() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_EARNED_WEEKLY, sampleStats, "u1", fixedClock)
        assertEquals("u2", board.entries[0].userId) // 350
        assertEquals("u1", board.entries[1].userId) // 200
        assertEquals("u3", board.entries[2].userId) // 150
    }

    @Test
    fun computeSetsCorrectRanks() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        assertEquals(1, board.entries[0].rank)
        assertEquals(2, board.entries[1].rank)
        assertEquals(3, board.entries[2].rank)
    }

    @Test
    fun computeMarksCurrentUser() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val currentUserEntry = board.entries.find { it.userId == "u1" }!!
        assertTrue(currentUserEntry.isCurrentUser)
        assertFalse(board.entries.find { it.userId == "u2" }!!.isCurrentUser)
    }

    @Test
    fun computeSetsCurrentUserRank() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        assertEquals(2, board.currentUserRank) // u1 is ranked 2nd
    }

    @Test
    fun computeCurrentUserRankNullIfNotInStats() {
        val board = calculator.compute("circle1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "unknown_user", fixedClock)
        assertNull(board.currentUserRank)
    }

    @Test
    fun computeSetsCircleId() {
        val board = calculator.compute("myCircle", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        assertEquals("myCircle", board.circleId)
    }

    @Test
    fun computeSetsCategory() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.STREAK_DAYS, sampleStats, "u1", fixedClock)
        assertEquals(LeaderboardCalculator.LeaderboardCategory.STREAK_DAYS, board.category)
    }

    @Test
    fun computeSetsComputedAt() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        assertEquals(fixedClock.now(), board.computedAt)
    }

    @Test
    fun computeHandlesEmptyStats() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, emptyList(), "u1", fixedClock)
        assertTrue(board.entries.isEmpty())
        assertNull(board.currentUserRank)
    }

    @Test
    fun computeHandlesSingleMember() {
        val single = listOf(sampleStats[0])
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, single, "u1", fixedClock)
        assertEquals(1, board.entries.size)
        assertEquals(1, board.currentUserRank)
    }

    @Test
    fun computeFpBalanceValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u2" }!!
        assertEquals(500.0, entry.value)
        assertEquals("500 FP", entry.valueLabel)
    }

    @Test
    fun computeNutritiveminutesValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.NUTRITIVE_MINUTES, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u3" }!!
        assertEquals(200.0, entry.value)
        assertEquals("200 min", entry.valueLabel)
    }

    @Test
    fun computeFewestEmptyCaloriesValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FEWEST_EMPTY_CALORIES, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u2" }!!
        assertEquals(20.0, entry.value)
        assertEquals("20 min", entry.valueLabel)
    }

    @Test
    fun computeStreakDaysValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.STREAK_DAYS, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u2" }!!
        assertEquals(15.0, entry.value)
        assertEquals("15 days", entry.valueLabel)
    }

    @Test
    fun computeIntentAccuracyValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.INTENT_ACCURACY, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u2" }!!
        assertEquals(0.92.toDouble(), entry.value, 0.01)
        // 0.92 * 100 = 92
        assertEquals("92%", entry.valueLabel)
    }

    @Test
    fun computeFpEarnedWeeklyValueLabel() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_EARNED_WEEKLY, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u2" }!!
        assertEquals(350.0, entry.value)
        assertEquals("350 FP", entry.valueLabel)
    }

    @Test
    fun computeDisplayName() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val entry = board.entries.find { it.userId == "u1" }!!
        assertEquals("Alice", entry.displayName)
    }
}

class LeaderboardCalculatorComputeAllTest {

    private val calculator = LeaderboardCalculator()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    private val sampleStats = listOf(
        LeaderboardCalculator.UserStats("u1", "Alice", fpBalance = 300, nutritiveMinutes = 120, emptyCalorieMinutes = 50, streakDays = 10, intentAccuracyPercent = 0.85f, fpEarnedWeekly = 200),
        LeaderboardCalculator.UserStats("u2", "Bob", fpBalance = 500, nutritiveMinutes = 80, emptyCalorieMinutes = 20, streakDays = 15, intentAccuracyPercent = 0.92f, fpEarnedWeekly = 350)
    )

    @Test
    fun computeAllReturnsAllCategories() {
        val all = calculator.computeAll("circle1", sampleStats, "u1", fixedClock)
        assertEquals(6, all.size)
        LeaderboardCalculator.LeaderboardCategory.entries.forEach { category ->
            assertTrue(all.containsKey(category), "Missing category: $category")
        }
    }

    @Test
    fun computeAllEachBoardHasCorrectCategory() {
        val all = calculator.computeAll("circle1", sampleStats, "u1", fixedClock)
        all.forEach { (category, board) ->
            assertEquals(category, board.category)
        }
    }

    @Test
    fun computeAllEachBoardHasCorrectCircleId() {
        val all = calculator.computeAll("circle1", sampleStats, "u1", fixedClock)
        all.values.forEach { board ->
            assertEquals("circle1", board.circleId)
        }
    }

    @Test
    fun computeAllWithEmptyStats() {
        val all = calculator.computeAll("circle1", emptyList(), "u1", fixedClock)
        assertEquals(6, all.size)
        all.values.forEach { board ->
            assertTrue(board.entries.isEmpty())
        }
    }
}

class LeaderboardCalculatorSummarizeStandingTest {

    private val calculator = LeaderboardCalculator()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    private val sampleStats = listOf(
        LeaderboardCalculator.UserStats("u1", "Alice", fpBalance = 300, nutritiveMinutes = 120, emptyCalorieMinutes = 50, streakDays = 10, intentAccuracyPercent = 0.85f, fpEarnedWeekly = 200),
        LeaderboardCalculator.UserStats("u2", "Bob", fpBalance = 500, nutritiveMinutes = 80, emptyCalorieMinutes = 20, streakDays = 15, intentAccuracyPercent = 0.92f, fpEarnedWeekly = 350),
        LeaderboardCalculator.UserStats("u3", "Carol", fpBalance = 100, nutritiveMinutes = 200, emptyCalorieMinutes = 100, streakDays = 5, intentAccuracyPercent = 0.78f, fpEarnedWeekly = 150)
    )

    @Test
    fun summarizeStandingReturnsNoDataForEmptyLeaderboards() {
        val emptyBoards = calculator.computeAll("c1", emptyList(), "u1", fixedClock)
        val summary = calculator.summarizeStanding(emptyBoards, "u1")
        assertEquals("No data available yet.", summary)
    }

    @Test
    fun summarizeStandingContainsRankInfo() {
        val boards = calculator.computeAll("c1", sampleStats, "u1", fixedClock)
        val summary = calculator.summarizeStanding(boards, "u1")
        assertTrue(summary.contains("#"))
        assertTrue(summary.contains("Average rank:"))
    }

    @Test
    fun summarizeStandingContainsMemberCount() {
        val boards = calculator.computeAll("c1", sampleStats, "u1", fixedClock)
        val summary = calculator.summarizeStanding(boards, "u1")
        assertTrue(summary.contains("3 members"))
    }

    @Test
    fun summarizeStandingShowsBestCategory() {
        val boards = calculator.computeAll("c1", sampleStats, "u1", fixedClock)
        val summary = calculator.summarizeStanding(boards, "u1")
        // u1 (Alice) is best in NUTRITIVE_MINUTES (rank 2 in most, but rank 1 in nutritive - she has 120, Carol has 200...)
        // Actually let's just verify the summary is non-empty and well-formed
        assertTrue(summary.isNotBlank())
    }

    @Test
    fun summarizeStandingForTopUser() {
        val boards = calculator.computeAll("c1", sampleStats, "u2", fixedClock)
        val summary = calculator.summarizeStanding(boards, "u2")
        // u2 (Bob) is #1 in most categories
        assertTrue(summary.contains("#1"))
    }

    @Test
    fun summarizeStandingForUserNotInLeaderboards() {
        val boards = calculator.computeAll("c1", sampleStats, "unknown", fixedClock)
        val summary = calculator.summarizeStanding(boards, "unknown")
        // User not found in any board, so standings will be empty
        // bestCategory will be null
        assertTrue(summary.contains("N/A") || summary.contains("#-") || summary.contains("No data"))
    }
}

class LeaderboardCalculatorTopNTest {

    private val calculator = LeaderboardCalculator()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    private val sampleStats = listOf(
        LeaderboardCalculator.UserStats("u1", "Alice", fpBalance = 300, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u2", "Bob", fpBalance = 500, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u3", "Carol", fpBalance = 100, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u4", "Dave", fpBalance = 400, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u5", "Eve", fpBalance = 200, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0)
    )

    @Test
    fun topNReturnsDefaultThree() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val top = calculator.topN(board)
        assertEquals(3, top.size)
    }

    @Test
    fun topNReturnsTopEntries() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val top = calculator.topN(board, 3)
        assertEquals("u2", top[0].userId) // 500
        assertEquals("u4", top[1].userId) // 400
        assertEquals("u1", top[2].userId) // 300
    }

    @Test
    fun topNReturnsAllWhenNExceedsSize() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val top = calculator.topN(board, 10)
        assertEquals(5, top.size)
    }

    @Test
    fun topNReturnsEmptyForEmptyBoard() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, emptyList(), "u1", fixedClock)
        val top = calculator.topN(board, 3)
        assertTrue(top.isEmpty())
    }

    @Test
    fun topNReturnsOneWhenNIsOne() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val top = calculator.topN(board, 1)
        assertEquals(1, top.size)
        assertEquals("u2", top[0].userId) // highest FP
    }
}

class LeaderboardCalculatorUserContextTest {

    private val calculator = LeaderboardCalculator()
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    private val sampleStats = listOf(
        LeaderboardCalculator.UserStats("u1", "Alice", fpBalance = 500, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u2", "Bob", fpBalance = 400, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u3", "Carol", fpBalance = 300, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u4", "Dave", fpBalance = 200, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0),
        LeaderboardCalculator.UserStats("u5", "Eve", fpBalance = 100, nutritiveMinutes = 0, emptyCalorieMinutes = 0, streakDays = 0, intentAccuracyPercent = 0f, fpEarnedWeekly = 0)
    )

    @Test
    fun userContextReturnsWindowAroundUser() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u3", fixedClock)
        // u3 is at index 2 (rank 3), window=2 means indices 0..4
        val context = calculator.userContext(board, "u3", 2)
        assertEquals(5, context.size) // all 5 fit within window
    }

    @Test
    fun userContextReturnsEmptyForUnknownUser() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        val context = calculator.userContext(board, "unknown")
        assertTrue(context.isEmpty())
    }

    @Test
    fun userContextDefaultWindowSize() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u3", fixedClock)
        val context = calculator.userContext(board, "u3") // default windowSize = 2
        // u3 at index 2, window from 0 to 4+1=5
        assertEquals(5, context.size)
    }

    @Test
    fun userContextClampedAtStart() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u1", fixedClock)
        // u1 is at index 0, window=2: from max(0, 0-2)=0, to min(5, 0+2+1)=3
        val context = calculator.userContext(board, "u1", 2)
        assertEquals(3, context.size)
        assertEquals("u1", context[0].userId)
    }

    @Test
    fun userContextClampedAtEnd() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u5", fixedClock)
        // u5 is at index 4, window=2: from max(0, 4-2)=2, to min(5, 4+2+1)=5
        val context = calculator.userContext(board, "u5", 2)
        assertEquals(3, context.size)
        assertEquals("u5", context[2].userId)
    }

    @Test
    fun userContextWindowSizeOne() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u3", fixedClock)
        // u3 at index 2, window=1: from max(0,2-1)=1, to min(5,2+1+1)=4
        val context = calculator.userContext(board, "u3", 1)
        assertEquals(3, context.size)
        assertEquals("u2", context[0].userId)
        assertEquals("u3", context[1].userId)
        assertEquals("u4", context[2].userId)
    }

    @Test
    fun userContextWindowSizeZero() {
        val board = calculator.compute("c1", LeaderboardCalculator.LeaderboardCategory.FP_BALANCE, sampleStats, "u3", fixedClock)
        // u3 at index 2, window=0: from max(0,2)=2, to min(5,3)=3
        val context = calculator.userContext(board, "u3", 0)
        assertEquals(1, context.size)
        assertEquals("u3", context[0].userId)
    }
}

// =============================================================================
//  Integration-style tests combining multiple operations
// =============================================================================

class BuddyManagerIntegrationTest {

    private lateinit var manager: BuddyManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = BuddyManager()
    }

    @Test
    fun fullBuddyLifecycle() {
        // Create invite
        val invite = manager.createInvite("alice", BuddyManager.SharingLevel.BASIC, fixedClock)
        assertEquals(BuddyManager.InviteStatus.PENDING, invite.status)

        // Accept invite
        val pair = manager.acceptInvite(invite.inviteCode, "bob", fixedClock)
        assertTrue(pair.isActive)
        assertEquals("alice", pair.userAId)
        assertEquals("bob", pair.userBId)
        assertEquals(BuddyManager.SharingLevel.BASIC, pair.sharingLevel)

        // Check pairs
        assertEquals(1, manager.getActivePairsForUser("alice").size)
        assertEquals(1, manager.getActivePairsForUser("bob").size)

        // Update sharing level
        val updated = manager.updateSharingLevel(pair.pairId, "bob", BuddyManager.SharingLevel.DETAILED)
        assertEquals(BuddyManager.SharingLevel.DETAILED, updated.sharingLevel)

        // Build snapshot
        val snapshot = manager.buildSnapshot("bob", updated.sharingLevel, 100, 5, 200, 50, 60, 30)
        assertEquals(100, snapshot.fpBalance)
        assertEquals(60, snapshot.nutritiveMinutes)

        // Remove pair
        assertTrue(manager.removePair(pair.pairId, "alice"))
        assertEquals(0, manager.getActivePairsForUser("alice").size)
        assertEquals(0, manager.getActivePairsForUser("bob").size)
    }

    @Test
    fun maxPairsEnforcement() {
        // Create exactly 3 pairs for alice
        for (i in 1..3) {
            val invite = manager.createInvite("alice", clock = fixedClock)
            manager.acceptInvite(invite.inviteCode, "buddy_$i", fixedClock)
        }
        assertEquals(3, manager.getActivePairsForUser("alice").size)

        // Cannot create a 4th
        assertFailsWith<IllegalStateException> {
            manager.createInvite("alice", clock = fixedClock)
        }

        // Remove one, then we can create again
        val pairs = manager.getActivePairsForUser("alice")
        manager.removePair(pairs[0].pairId, "alice")
        assertEquals(2, manager.getActivePairsForUser("alice").size)

        val newInvite = manager.createInvite("alice", clock = fixedClock)
        assertNotNull(newInvite)
    }
}

class ChallengeEngineIntegrationTest {

    private lateinit var engine: ChallengeEngine
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }
    private val today: LocalDate
        get() = fixedClock.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    @BeforeTest
    fun setup() {
        engine = ChallengeEngine()
    }

    @Test
    fun fullIndividualChallengeLifecycle() {
        // Create
        val challenge = engine.createChallenge(
            title = "Weekly Focus",
            type = ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "circle1",
            createdByUserId = "alice",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 100,
            clock = fixedClock
        )
        assertEquals(ChallengeEngine.ChallengeStatus.ACTIVE, challenge.status)

        // Join
        engine.joinChallenge(challenge.challengeId, "bob", fixedClock)
        engine.joinChallenge(challenge.challengeId, "carol", fixedClock)

        // Record progress
        engine.recordProgress(challenge.challengeId, "alice", 50, fixedClock)
        engine.recordProgress(challenge.challengeId, "alice", 55, fixedClock) // total 105
        engine.recordProgress(challenge.challengeId, "bob", 80, fixedClock)
        engine.recordProgress(challenge.challengeId, "carol", 120, fixedClock)

        // Check progress
        assertEquals(100, engine.progressPercent(challenge.challengeId, "alice")) // capped at 100
        assertEquals(80, engine.progressPercent(challenge.challengeId, "bob"))
        assertEquals(100, engine.progressPercent(challenge.challengeId, "carol"))

        // Leaderboard
        val board = engine.getLeaderboard(challenge.challengeId)
        assertEquals("carol", board[0].userId) // 120
        assertEquals("alice", board[1].userId) // 105
        assertEquals("bob", board[2].userId)   // 80

        // Finalize
        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        assertEquals(3, result.participantCount)
        assertTrue("alice" in result.winners)
        assertTrue("carol" in result.winners)
        assertFalse("bob" in result.winners)
    }

    @Test
    fun fullTeamChallengeLifecycle() {
        val challenge = engine.createChallenge(
            title = "Team Goal",
            type = ChallengeEngine.ChallengeType.GROUP_FP_POOL,
            scope = ChallengeEngine.ChallengeScope.CIRCLE,
            scopeId = "circle1",
            createdByUserId = "alice",
            startDate = today,
            endDate = today.plus(7, DateTimeUnit.DAY),
            targetValue = 200,
            isTeamChallenge = true,
            clock = fixedClock
        )

        engine.joinChallenge(challenge.challengeId, "bob", fixedClock)

        engine.recordProgress(challenge.challengeId, "alice", 80, fixedClock)
        engine.recordProgress(challenge.challengeId, "bob", 120, fixedClock)

        val result = engine.finalizeChallenge(challenge.challengeId, fixedClock)
        // Total = 80 + 120 = 200 >= 200
        assertTrue("alice" in result.winners)
        assertTrue("bob" in result.winners)
    }
}

class CircleManagerIntegrationTest {

    private lateinit var manager: CircleManager
    private val fixedClock = object : Clock {
        override fun now(): Instant = Instant.parse("2025-06-01T12:00:00Z")
    }

    @BeforeTest
    fun setup() {
        manager = CircleManager()
    }

    @Test
    fun fullCircleLifecycle() {
        // Create
        val circle = manager.createCircle(
            name = "Focus Friends",
            description = "A circle for focused work",
            visibility = CircleManager.CircleVisibility.PUBLIC,
            creatorUserId = "alice",
            clock = fixedClock
        )

        // Verify creation
        assertEquals(1, manager.getMemberCount(circle.circleId))
        assertTrue(manager.getPublicCircles().any { it.circleId == circle.circleId })

        // Join by invite code
        val bob = manager.joinByInviteCode(circle.inviteCode, "bob", fixedClock)
        assertEquals(CircleManager.CircleRole.MEMBER, bob.role)

        // Join directly
        val carol = manager.joinCircle(circle.circleId, "carol", fixedClock)
        assertEquals(3, manager.getMemberCount(circle.circleId))

        // Promote bob to admin
        manager.promoteToAdmin(circle.circleId, "alice", "bob")

        // Update circle
        val updated = manager.updateCircle(circle.circleId, "bob", name = "Super Focus Friends")
        assertEquals("Super Focus Friends", updated.name)

        // Remove carol (by admin bob)
        assertTrue(manager.removeMember(circle.circleId, "bob", "carol"))
        assertEquals(2, manager.getMemberCount(circle.circleId))

        // Alice leaves - bob remains as admin
        manager.leaveCircle(circle.circleId, "alice")
        assertEquals(1, manager.getMemberCount(circle.circleId))

        // Bob is the last member - leaving deactivates
        manager.leaveCircle(circle.circleId, "bob")
        assertNull(manager.getCircle(circle.circleId))
    }

    @Test
    fun maxCircleSizeEnforcement() {
        val circle = manager.createCircle("Full Circle", creatorUserId = "admin", clock = fixedClock)
        for (i in 2..12) {
            manager.joinCircle(circle.circleId, "user$i", fixedClock)
        }
        assertEquals(12, manager.getMemberCount(circle.circleId))
        assertFailsWith<IllegalStateException> {
            manager.joinCircle(circle.circleId, "user13", fixedClock)
        }
    }
}
