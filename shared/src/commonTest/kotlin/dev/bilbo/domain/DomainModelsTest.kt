package dev.bilbo.domain

import dev.bilbo.domain.social.*
import dev.bilbo.shared.domain.model.*
import dev.bilbo.shared.domain.model.AppCategory as SharedAppCategory
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class DomainModelsTest {

    // ── AnalogSuggestion ─────────────────────────────────────────────────

    @Test fun analogSuggestionDefaults() {
        val s = AnalogSuggestion(text = "Walk", category = SuggestionCategory.EXERCISE, tags = listOf("outdoor"))
        assertEquals(0L, s.id)
        assertNull(s.timeOfDay)
        assertEquals(0, s.timesShown)
        assertEquals(0, s.timesAccepted)
        assertFalse(s.isCustom)
    }

    @Test fun analogSuggestionCopy() {
        val s = AnalogSuggestion(id = 1, text = "Run", category = SuggestionCategory.EXERCISE, tags = emptyList())
        val c = s.copy(text = "Sprint", isCustom = true)
        assertEquals("Sprint", c.text)
        assertTrue(c.isCustom)
        assertEquals(1L, c.id)
    }

    @Test fun suggestionCategoryEntries() {
        assertEquals(10, SuggestionCategory.entries.size)
        assertNotNull(SuggestionCategory.valueOf("EXERCISE"))
        assertNotNull(SuggestionCategory.valueOf("READING"))
    }

    @Test fun timeOfDayEntries() {
        assertEquals(4, TimeOfDay.entries.size)
        assertEquals(TimeOfDay.MORNING, TimeOfDay.valueOf("MORNING"))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.valueOf("NIGHT"))
    }

    // ── AppProfile / AppCategory / EnforcementMode ────────────────────────

    @Test fun appProfileDefaults() {
        val p = AppProfile("com.test", "Test", AppCategory.NEUTRAL, EnforcementMode.NUDGE)
        assertFalse(p.coolingOffEnabled)
        assertFalse(p.isBypassed)
        assertFalse(p.isCustomClassification)
    }

    @Test fun appCategoryEntries() {
        assertEquals(3, AppCategory.entries.size)
        assertNotNull(AppCategory.valueOf("NUTRITIVE"))
        assertNotNull(AppCategory.valueOf("NEUTRAL"))
        assertNotNull(AppCategory.valueOf("EMPTY_CALORIES"))
    }

    @Test fun enforcementModeEntries() {
        assertEquals(2, EnforcementMode.entries.size)
        assertEquals(EnforcementMode.NUDGE, EnforcementMode.valueOf("NUDGE"))
        assertEquals(EnforcementMode.HARD_LOCK, EnforcementMode.valueOf("HARD_LOCK"))
    }

    // ── DopamineBudget ────────────────────────────────────────────────────

    @Test fun currentBalanceBasic() {
        val b = DopamineBudget(
            date = LocalDate(2025, 1, 1), fpEarned = 10, fpSpent = 5, fpBonus = 3,
            fpRolloverIn = 2, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertEquals(25, b.currentBalance())
    }

    @Test fun currentBalanceNegative() {
        val b = DopamineBudget(
            date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 20, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertEquals(-5, b.currentBalance())
    }

    @Test fun currentBalanceZero() {
        val b = DopamineBudget(
            date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertEquals(FPEconomy.DAILY_BASELINE, b.currentBalance())
    }

    // ── FPEconomy ─────────────────────────────────────────────────────────

    @Test fun fpEconomyConstants() {
        assertEquals(1, FPEconomy.EARN_PER_NUTRITIVE_MINUTE)
        assertEquals(1, FPEconomy.COST_PER_EMPTY_CALORIE_MINUTE)
        assertEquals(3, FPEconomy.BONUS_BREATHING_EXERCISE)
        assertEquals(5, FPEconomy.BONUS_ANALOG_ACCEPTED)
        assertEquals(2, FPEconomy.BONUS_ACCURATE_INTENT)
        assertEquals(10, FPEconomy.PENALTY_HARD_LOCK_OVERRIDE)
        assertEquals(3, FPEconomy.PENALTY_NUDGE_IGNORE)
        assertEquals(60, FPEconomy.DAILY_EARN_CAP)
        assertEquals(15, FPEconomy.DAILY_BASELINE)
        assertEquals(0.5, FPEconomy.ROLLOVER_PERCENTAGE)
        assertEquals(60, FPEconomy.MIN_SESSION_SECONDS)
        assertEquals(20, FPEconomy.STREAK_BONUS_7_DAY)
    }

    // ── Emotion / EmotionalCheckIn ────────────────────────────────────────

    @Test fun emotionEntries() {
        assertEquals(7, Emotion.entries.size)
        assertNotNull(Emotion.valueOf("HAPPY"))
        assertNotNull(Emotion.valueOf("LONELY"))
    }

    @Test fun emotionalCheckInDefaults() {
        val c = EmotionalCheckIn(timestamp = Instant.fromEpochSeconds(100), preSessionEmotion = Emotion.HAPPY)
        assertEquals(0L, c.id)
        assertNull(c.postSessionMood)
        assertNull(c.linkedIntentId)
    }

    // ── IntentDeclaration ─────────────────────────────────────────────────

    @Test fun intentDeclarationDefaults() {
        val i = IntentDeclaration(timestamp = Instant.fromEpochSeconds(100), declaredApp = "com.app", declaredDurationMinutes = 30)
        assertEquals(0L, i.id)
        assertNull(i.actualDurationMinutes)
        assertFalse(i.wasEnforced)
        assertNull(i.enforcementType)
        assertFalse(i.wasOverridden)
        assertNull(i.emotionalCheckInId)
    }

    // ── UsageSession ──────────────────────────────────────────────────────

    @Test fun usageSessionDefaults() {
        val s = UsageSession(
            packageName = "com.app", appLabel = "App", category = AppCategory.NEUTRAL,
            startTime = Instant.fromEpochSeconds(100), endTime = null, durationSeconds = 120
        )
        assertEquals(0L, s.id)
        assertTrue(s.wasTracked)
        assertNull(s.endTime)
    }

    // ── WeeklyInsight / HeuristicInsight / InsightType ────────────────────

    @Test fun insightTypeEntries() {
        assertEquals(4, InsightType.entries.size)
        assertNotNull(InsightType.valueOf("CORRELATION"))
        assertNotNull(InsightType.valueOf("ACHIEVEMENT"))
    }

    @Test fun weeklyInsightDefaults() {
        val w = WeeklyInsight(
            weekStart = LocalDate(2025, 1, 6),
            tier2Insights = listOf(HeuristicInsight(InsightType.TREND, "test", 0.8f)),
            totalScreenTimeMinutes = 100, nutritiveMinutes = 40, emptyCalorieMinutes = 30,
            fpEarned = 50, fpSpent = 20, intentAccuracyPercent = 0.85f, streakDays = 5
        )
        assertEquals(0L, w.id)
        assertNull(w.tier3Narrative)
        assertEquals(1, w.tier2Insights.size)
    }

    @Test fun heuristicInsightCreation() {
        val h = HeuristicInsight(InsightType.ANOMALY, "spike day", 0.9f)
        assertEquals(InsightType.ANOMALY, h.type)
        assertEquals("spike day", h.message)
        assertEquals(0.9f, h.confidence)
    }

    // ── Social: SharingLevel ──────────────────────────────────────────────

    @Test fun sharingLevelEntries() {
        assertEquals(5, SharingLevel.entries.size)
        assertEquals(SharingLevel.PRIVATE, SharingLevel.valueOf("PRIVATE"))
        assertEquals(SharingLevel.FULL, SharingLevel.valueOf("FULL"))
    }

    // ── Social: BuddyPair ─────────────────────────────────────────────────

    @Test fun buddyPairDefaults() {
        val bp = BuddyPair(
            localUserId = "a", buddyUserId = "b", buddyDisplayName = "Buddy",
            sharingLevel = SharingLevel.SCORES_ONLY, createdAt = Instant.fromEpochSeconds(0)
        )
        assertEquals(0L, bp.id)
        assertNull(bp.buddyAvatarUrl)
        assertNull(bp.lastSyncedAt)
        assertTrue(bp.isActive)
        assertEquals(0, bp.streakDays)
        assertNull(bp.lastEncouragementSentAt)
        assertNull(bp.lastEncouragementReceivedAt)
    }

    // ── Social: Challenge ─────────────────────────────────────────────────

    @Test fun challengeTypeEntries() {
        assertEquals(7, ChallengeType.entries.size)
    }

    @Test fun challengeModeEntries() {
        assertEquals(2, ChallengeMode.entries.size)
        assertNotNull(ChallengeMode.valueOf("COMPETITIVE"))
        assertNotNull(ChallengeMode.valueOf("COOPERATIVE"))
    }

    @Test fun challengeStatusEntries() {
        assertEquals(4, ChallengeStatus.entries.size)
    }

    @Test fun challengeGoalDefaults() {
        val g = ChallengeGoal(targetValue = 100, unit = "FP")
        assertTrue(g.perParticipant)
    }

    @Test fun challengeCreation() {
        val c = Challenge(
            remoteId = "r1", title = "Test", type = ChallengeType.MOST_FP_EARNED,
            mode = ChallengeMode.COMPETITIVE, status = ChallengeStatus.ACTIVE,
            goal = ChallengeGoal(100, "FP"), creatorId = "u1",
            participantIds = listOf("u1", "u2"),
            startDate = LocalDate(2025, 1, 1), endDate = LocalDate(2025, 1, 7),
            createdAt = Instant.fromEpochSeconds(0)
        )
        assertEquals(0L, c.id)
        assertNull(c.description)
        assertNull(c.completedAt)
        assertNull(c.winnerId)
        assertEquals(0f, c.localUserProgress)
        assertTrue(c.leaderboard.isEmpty())
    }

    @Test fun challengeParticipantProgressCreation() {
        val p = ChallengeParticipantProgress(
            userId = "u1", displayName = "User", currentValue = 50f,
            rank = 1, lastUpdatedAt = Instant.fromEpochSeconds(0)
        )
        assertEquals("u1", p.userId)
        assertNull(p.avatarUrl)
    }

    // ── Social: Circle ────────────────────────────────────────────────────

    @Test fun circleRoleEntries() {
        assertEquals(3, CircleRole.entries.size)
        assertNotNull(CircleRole.valueOf("MEMBER"))
        assertNotNull(CircleRole.valueOf("OWNER"))
    }

    @Test fun circleDefaults() {
        val c = Circle(
            remoteId = "r1", name = "Test", ownerId = "u1",
            sharingLevel = SharingLevel.CATEGORY_BREAKDOWN,
            createdAt = Instant.fromEpochSeconds(0)
        )
        assertEquals(0L, c.id)
        assertNull(c.description)
        assertNull(c.avatarUrl)
        assertFalse(c.isPublic)
        assertNull(c.inviteCode)
        assertEquals(0, c.memberCount)
        assertTrue(c.members.isEmpty())
    }

    @Test fun circleMemberDefaults() {
        val m = CircleMember(
            userId = "u1", displayName = "User", role = CircleRole.MODERATOR,
            joinedAt = Instant.fromEpochSeconds(0), sharingLevel = SharingLevel.SCORES_ONLY
        )
        assertNull(m.avatarUrl)
        assertTrue(m.isActive)
        assertEquals(0, m.currentStreakDays)
        assertEquals(0, m.weeklyFpEarned)
        assertEquals(0, m.weeklyFpBalance)
    }

    // ── Shared models: AppUsageSession ────────────────────────────────────

    private val json = Json { ignoreUnknownKeys = true }

    @Test fun appUsageSessionSerialization() {
        val s = AppUsageSession(
            id = "s1", userId = "u1", packageName = "com.test", appName = "Test",
            startTime = Instant.fromEpochSeconds(1000), endTime = Instant.fromEpochSeconds(2000),
            durationMs = 1000000, category = SharedAppCategory.SOCIAL
        )
        val str = json.encodeToString(AppUsageSession.serializer(), s)
        val decoded = json.decodeFromString(AppUsageSession.serializer(), str)
        assertEquals(s, decoded)
    }

    @Test fun sharedAppCategoryEntries() {
        assertEquals(7, SharedAppCategory.entries.size)
    }

    @Test fun dailyInsightSerialization() {
        val d = DailyInsight(
            id = "i1", userId = "u1", date = "2025-01-15", summary = "Good",
            highlights = listOf("h1"), suggestions = listOf("s1"),
            totalScreenTimeMinutes = 120,
            topApps = listOf(AppUsageSummary("com.test", "Test", 60, SharedAppCategory.SOCIAL)),
            tier = 1, mood = MoodScore(8, "happy")
        )
        val str = json.encodeToString(DailyInsight.serializer(), d)
        val decoded = json.decodeFromString(DailyInsight.serializer(), str)
        assertEquals(d, decoded)
    }

    @Test fun dailyInsightNullMood() {
        val d = DailyInsight(
            id = "i2", userId = "u1", date = "2025-01-16", summary = "OK",
            highlights = emptyList(), suggestions = emptyList(),
            totalScreenTimeMinutes = 60, topApps = emptyList(), tier = 2, mood = null
        )
        val str = json.encodeToString(DailyInsight.serializer(), d)
        val decoded = json.decodeFromString(DailyInsight.serializer(), str)
        assertNull(decoded.mood)
    }

    @Test fun wellnessGoalSerialization() {
        val g = WellnessGoal(
            id = "g1", userId = "u1", name = "Reduce", description = "Less scrolling",
            type = GoalType.SCREEN_TIME_LIMIT, targetApps = listOf("com.test"),
            dailyLimitMinutes = 60, isActive = true, createdAt = "2025-01-01"
        )
        val str = json.encodeToString(WellnessGoal.serializer(), g)
        val decoded = json.decodeFromString(WellnessGoal.serializer(), str)
        assertEquals(g, decoded)
    }

    @Test fun goalTypeEntries() {
        assertEquals(4, GoalType.entries.size)
    }

    @Test fun moodScoreSerialization() {
        val m = MoodScore(8, "happy")
        val str = json.encodeToString(MoodScore.serializer(), m)
        val decoded = json.decodeFromString(MoodScore.serializer(), str)
        assertEquals(m, decoded)
    }

    @Test fun appUsageSummarySerialization() {
        val a = AppUsageSummary("com.test", "Test", 30, SharedAppCategory.ENTERTAINMENT)
        val str = json.encodeToString(AppUsageSummary.serializer(), a)
        val decoded = json.decodeFromString(AppUsageSummary.serializer(), str)
        assertEquals(a, decoded)
    }
}
