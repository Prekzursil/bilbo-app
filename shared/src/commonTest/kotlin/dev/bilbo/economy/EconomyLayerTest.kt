package dev.bilbo.economy

import dev.bilbo.domain.*
import kotlinx.datetime.*
import kotlin.test.*

// =============================================================================
//  FocusPointsEngine Tests
// =============================================================================

class FocusPointsEngineEarnPointsTest {
    private val engine = FocusPointsEngine()
    private val baseBudget = DopamineBudget(
        date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 0, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun earnPointsBasic() {
        val (updated, earned) = engine.earnPoints(baseBudget, 10)
        assertEquals(10, earned)
        assertEquals(10, updated.fpEarned)
    }

    @Test fun earnPointsRespectsCapAtZero() {
        val budget = baseBudget.copy(fpEarned = FPEconomy.DAILY_EARN_CAP)
        val (updated, earned) = engine.earnPoints(budget, 10)
        assertEquals(0, earned)
        assertEquals(FPEconomy.DAILY_EARN_CAP, updated.fpEarned)
    }

    @Test fun earnPointsPartialCap() {
        val budget = baseBudget.copy(fpEarned = FPEconomy.DAILY_EARN_CAP - 5)
        val (updated, earned) = engine.earnPoints(budget, 10)
        assertEquals(5, earned)
        assertEquals(FPEconomy.DAILY_EARN_CAP, updated.fpEarned)
    }

    @Test fun earnPointsZeroMinutes() {
        val (updated, earned) = engine.earnPoints(baseBudget, 0)
        assertEquals(0, earned)
        assertEquals(0, updated.fpEarned)
    }
}

class FocusPointsEngineSpendPointsTest {
    private val engine = FocusPointsEngine()
    private val baseBudget = DopamineBudget(
        date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 0, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun spendPointsBasic() {
        val (updated, spent) = engine.spendPoints(baseBudget, 5)
        assertEquals(5, spent)
        assertEquals(5, updated.fpSpent)
    }

    @Test fun spendPointsAccumulates() {
        val budget = baseBudget.copy(fpSpent = 10)
        val (updated, spent) = engine.spendPoints(budget, 5)
        assertEquals(5, spent)
        assertEquals(15, updated.fpSpent)
    }
}

class FocusPointsEngineBonusPenaltyTest {
    private val engine = FocusPointsEngine()
    private val baseBudget = DopamineBudget(
        date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 0, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun applyBonusAddsToFpBonus() {
        val updated = engine.applyBonus(baseBudget, 10, "test")
        assertEquals(10, updated.fpBonus)
    }

    @Test fun applyBonusAccumulates() {
        val budget = baseBudget.copy(fpBonus = 5)
        val updated = engine.applyBonus(budget, 3)
        assertEquals(8, updated.fpBonus)
    }

    @Test fun applyPenaltyAddsToFpSpent() {
        val updated = engine.applyPenalty(baseBudget, 7, "test")
        assertEquals(7, updated.fpSpent)
    }

    @Test fun applyPenaltyAccumulates() {
        val budget = baseBudget.copy(fpSpent = 3)
        val updated = engine.applyPenalty(budget, 5)
        assertEquals(8, updated.fpSpent)
    }

    @Test fun getBalanceMatchesBudgetCurrentBalance() {
        val budget = baseBudget.copy(fpEarned = 10, fpSpent = 3, fpBonus = 2, fpRolloverIn = 1)
        assertEquals(budget.currentBalance(), engine.getBalance(budget))
    }

    // Preset bonuses
    @Test fun awardBreathingBonus() {
        val updated = engine.awardBreathingBonus(baseBudget)
        assertEquals(FPEconomy.BONUS_BREATHING_EXERCISE, updated.fpBonus)
    }

    @Test fun awardAnalogBonus() {
        val updated = engine.awardAnalogBonus(baseBudget)
        assertEquals(FPEconomy.BONUS_ANALOG_ACCEPTED, updated.fpBonus)
    }

    @Test fun awardAccurateIntentBonus() {
        val updated = engine.awardAccurateIntentBonus(baseBudget)
        assertEquals(FPEconomy.BONUS_ACCURATE_INTENT, updated.fpBonus)
    }

    @Test fun awardStreakBonus() {
        val updated = engine.awardStreakBonus(baseBudget)
        assertEquals(FPEconomy.STREAK_BONUS_7_DAY, updated.fpBonus)
    }

    // Preset penalties
    @Test fun penalizeHardLockOverride() {
        val updated = engine.penalizeHardLockOverride(baseBudget)
        assertEquals(FPEconomy.PENALTY_HARD_LOCK_OVERRIDE, updated.fpSpent)
    }

    @Test fun penalizeNudgeIgnored() {
        val updated = engine.penalizeNudgeIgnored(baseBudget)
        assertEquals(FPEconomy.PENALTY_NUDGE_IGNORE, updated.fpSpent)
    }
}

class FocusPointsEngineRolloverTest {
    private val engine = FocusPointsEngine()
    private val baseBudget = DopamineBudget(
        date = LocalDate(2025, 1, 1), fpEarned = 0, fpSpent = 0, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun calculateRolloverPositiveBalance() {
        val budget = baseBudget.copy(fpEarned = 20) // balance = 15 + 20 = 35
        val rollover = engine.calculateRollover(budget)
        assertEquals((35 * 0.5).toInt(), rollover)
    }

    @Test fun calculateRolloverNegativeBalance() {
        val budget = baseBudget.copy(fpSpent = 30) // balance = 15 - 30 = -15
        val rollover = engine.calculateRollover(budget)
        assertEquals(0, rollover)
    }

    @Test fun calculateRolloverZeroBalance() {
        val budget = baseBudget.copy(fpSpent = 15) // balance = 15 - 15 = 0
        val rollover = engine.calculateRollover(budget)
        assertEquals(0, rollover)
    }

    @Test fun createDayBudgetWithPreviousDay() {
        val previous = baseBudget.copy(fpEarned = 10) // balance = 25
        val newBudget = engine.createDayBudget(LocalDate(2025, 1, 2), previous)
        assertEquals(LocalDate(2025, 1, 2), newBudget.date)
        assertEquals(0, newBudget.fpEarned)
        assertEquals(0, newBudget.fpSpent)
        val expectedRollover = engine.calculateRollover(previous)
        assertEquals(expectedRollover, newBudget.fpRolloverIn)
    }

    @Test fun createDayBudgetWithoutPreviousDay() {
        val newBudget = engine.createDayBudget(LocalDate(2025, 1, 1), null)
        assertEquals(0, newBudget.fpRolloverIn)
        assertEquals(0, newBudget.fpRolloverOut)
    }

    @Test fun describeBudgetContainsBalance() {
        val budget = baseBudget.copy(fpEarned = 10)
        val desc = engine.describeBudget(budget)
        assertTrue(desc.contains("FP available"))
        assertTrue(desc.contains("FP earned today"))
    }
}

// =============================================================================
//  AppClassifier Tests
// =============================================================================

class AppClassifierClassifyTest {
    private val defaults = listOf(
        AppClassifier.AppClassification("com.known.app", "Known", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
    )
    private val classifier = AppClassifier.fromDefaults(defaults)

    @Test fun classifyBuiltIn() {
        val result = classifier.classify("com.known.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun classifyUnknownReturnsNull() {
        assertNull(classifier.classify("com.completely.unknown"))
    }

    @Test fun classifyUserOverrideTakesPrecedence() {
        classifier.setUserOverride("com.known.app", "Known", AppCategory.EMPTY_CALORIES, EnforcementMode.HARD_LOCK)
        val result = classifier.classify("com.known.app")
        assertNotNull(result)
        assertEquals(AppCategory.EMPTY_CALORIES, result.category)
    }

    @Test fun classifySocialHeuristic() {
        val result = classifier.classify("com.facebook.orca")
        assertNotNull(result)
        assertEquals(AppCategory.EMPTY_CALORIES, result.category)
    }

    @Test fun classifyLearningHeuristic() {
        val result = classifier.classify("com.duolingo.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun classifyProductivityHeuristic() {
        val result = classifier.classify("com.microsoft.teams")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun classifyHeuristicCaseInsensitive() {
        // Package names are lowercased for prefix matching
        val result = classifier.classify("com.Facebook.SomeApp")
        assertNotNull(result)
    }

    @Test fun classifyHeuristicAppLabel() {
        val result = classifier.classify("com.tiktok.musically")
        assertNotNull(result)
        // appLabel derived from last segment with first char uppercase
        assertEquals("Musically", result.appLabel)
    }
}

class AppClassifierGetProfileTest {
    private val defaults = listOf(
        AppClassifier.AppClassification("com.known", "Known", AppCategory.NEUTRAL, EnforcementMode.NUDGE)
    )
    private val classifier = AppClassifier.fromDefaults(defaults)

    @Test fun getProfileExisting() {
        val profile = classifier.getProfile("com.known", "Known App")
        assertNotNull(profile)
        assertEquals("com.known", profile.packageName)
        assertEquals("Known App", profile.appLabel)
        assertFalse(profile.isCustomClassification)
    }

    @Test fun getProfileWithOverride() {
        classifier.setUserOverride("com.known", "Known", AppCategory.EMPTY_CALORIES, EnforcementMode.HARD_LOCK)
        val profile = classifier.getProfile("com.known", "Known App")
        assertNotNull(profile)
        assertTrue(profile.isCustomClassification)
    }

    @Test fun getProfileUnknown() {
        assertNull(classifier.getProfile("com.totally.unknown", "Unknown"))
    }
}

class AppClassifierOverridesTest {
    private val classifier = AppClassifier.fromDefaults(emptyList())

    @Test fun setAndRemoveOverride() {
        classifier.setUserOverride("com.test", "Test", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
        assertTrue(classifier.hasUserOverride("com.test"))
        classifier.removeUserOverride("com.test")
        assertFalse(classifier.hasUserOverride("com.test"))
    }

    @Test fun getUserOverrides() {
        classifier.setUserOverride("com.a", "A", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
        classifier.setUserOverride("com.b", "B", AppCategory.NEUTRAL, EnforcementMode.HARD_LOCK)
        assertEquals(2, classifier.getUserOverrides().size)
    }
}

class AppClassifierBulkTest {
    private val defaults = listOf(
        AppClassifier.AppClassification("com.a", "A", AppCategory.NUTRITIVE, EnforcementMode.NUDGE),
        AppClassifier.AppClassification("com.b", "B", AppCategory.NEUTRAL, EnforcementMode.NUDGE)
    )
    private val classifier = AppClassifier.fromDefaults(defaults)

    @Test fun getAllClassificationsIncludesOverrides() {
        classifier.setUserOverride("com.c", "C", AppCategory.EMPTY_CALORIES, EnforcementMode.HARD_LOCK)
        val all = classifier.getAllClassifications()
        assertEquals(3, all.size)
    }

    @Test fun getAppsInCategory() {
        val nutritive = classifier.getAppsInCategory(AppCategory.NUTRITIVE)
        assertEquals(1, nutritive.size)
        assertEquals("com.a", nutritive[0].packageName)
    }

    @Test fun getUnclassified() {
        val unclassified = classifier.getUnclassified(listOf("com.a", "com.unknown.thing"))
        assertEquals(1, unclassified.size)
        assertEquals("com.unknown.thing", unclassified[0])
    }
}

class AppClassifierHeuristicEdgeCaseTest {
    private val classifier = AppClassifier.fromDefaults(emptyList())

    @Test fun inferSocialPrefix_instagram() {
        val result = classifier.classify("com.instagram.app")
        assertNotNull(result)
        assertEquals(AppCategory.EMPTY_CALORIES, result.category)
    }

    @Test fun inferSocialPrefix_twitter() {
        val result = classifier.classify("com.twitter.app")
        assertNotNull(result)
        assertEquals(AppCategory.EMPTY_CALORIES, result.category)
    }

    @Test fun inferSocialPrefix_snapchat() {
        val result = classifier.classify("com.snapchat.android")
        assertNotNull(result)
    }

    @Test fun inferSocialPrefix_tiktok() {
        val result = classifier.classify("com.tiktok.lite")
        assertNotNull(result)
    }

    @Test fun inferSocialPrefix_reddit() {
        val result = classifier.classify("com.reddit.frontpage")
        assertNotNull(result)
    }

    @Test fun inferSocialPrefix_tumblr() {
        val result = classifier.classify("com.tumblr.app")
        assertNotNull(result)
    }

    @Test fun inferSocialPrefix_pinterest() {
        val result = classifier.classify("com.pinterest.app")
        assertNotNull(result)
    }

    @Test fun inferLearningPrefix_coursera() {
        val result = classifier.classify("com.coursera.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun inferLearningPrefix_khanacademy() {
        val result = classifier.classify("org.khanacademy.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun inferLearningPrefix_audible() {
        val result = classifier.classify("com.audible.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun inferLearningPrefix_kindle() {
        val result = classifier.classify("com.amazon.kindle.app")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test fun inferProductivityPrefix_google_docs() {
        val result = classifier.classify("com.google.android.apps.docs.editors")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun inferProductivityPrefix_slack() {
        val result = classifier.classify("com.slack.app")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun inferProductivityPrefix_notion() {
        val result = classifier.classify("com.notion.id")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun inferProductivityPrefix_todoist() {
        val result = classifier.classify("com.todoist.app")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun inferProductivityPrefix_evernote() {
        val result = classifier.classify("com.evernote.app")
        assertNotNull(result)
        assertEquals(AppCategory.NEUTRAL, result.category)
    }

    @Test fun inferSocial_enforcement() {
        // Social apps get NUDGE enforcement
        val result = classifier.classify("com.facebook.katana")
        assertNotNull(result)
        assertEquals(EnforcementMode.NUDGE, result.defaultEnforcementMode)
    }

    @Test fun inferLearning_enforcement() {
        // Learning apps also get NUDGE
        val result = classifier.classify("com.duolingo.app")
        assertNotNull(result)
        assertEquals(EnforcementMode.NUDGE, result.defaultEnforcementMode)
    }

    @Test fun inferProductivity_enforcement() {
        // Productivity also gets NUDGE
        val result = classifier.classify("com.microsoft.teams")
        assertNotNull(result)
        assertEquals(EnforcementMode.NUDGE, result.defaultEnforcementMode)
    }
}

class AppClassifierFactoryTest {
    @Test fun fromDefaultsWithOverrides() {
        val defaults = listOf(
            AppClassifier.AppClassification("com.a", "A", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
        )
        val overrides = listOf(
            AppClassifier.AppClassification("com.a", "A Override", AppCategory.EMPTY_CALORIES, EnforcementMode.HARD_LOCK)
        )
        val classifier = AppClassifier.fromDefaults(defaults, overrides)
        val result = classifier.classify("com.a")
        assertNotNull(result)
        assertEquals(AppCategory.EMPTY_CALORIES, result.category)
        assertTrue(classifier.hasUserOverride("com.a"))
    }

    @Test fun appClassificationDataClass() {
        val c1 = AppClassifier.AppClassification("com.a", "A", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
        val c2 = AppClassifier.AppClassification("com.a", "A", AppCategory.NUTRITIVE, EnforcementMode.NUDGE)
        assertEquals(c1, c2)
        assertEquals(c1.hashCode(), c2.hashCode())
        val c3 = c1.copy(appLabel = "B")
        assertEquals("B", c3.appLabel)
    }
}

// =============================================================================
//  BudgetEnforcer Tests
// =============================================================================

private class FakeClock(var instant: Instant) : Clock {
    override fun now(): Instant = instant
}

class BudgetEnforcerResetTest {
    private val fixedInstant = LocalDateTime(2025, 6, 15, 10, 0).toInstant(TimeZone.UTC)
    private val clock = FakeClock(fixedInstant)
    private val enforcer = BudgetEnforcer(clock = clock)
    private val tz = TimeZone.UTC

    @Test fun resetForNewDayWithPrevious() {
        val prev = DopamineBudget(
            date = LocalDate(2025, 6, 14), fpEarned = 20, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        val budget = enforcer.resetForNewDay(prev, tz)
        assertEquals(LocalDate(2025, 6, 15), budget.date)
        assertTrue(budget.fpRolloverIn > 0)
    }

    @Test fun resetForNewDayWithNull() {
        val budget = enforcer.resetForNewDay(null, tz)
        assertEquals(LocalDate(2025, 6, 15), budget.date)
        assertEquals(0, budget.fpRolloverIn)
    }

    @Test fun isTodayBudgetTrue() {
        val budget = DopamineBudget(
            date = LocalDate(2025, 6, 15), fpEarned = 0, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertTrue(enforcer.isTodayBudget(budget, tz))
    }

    @Test fun isTodayBudgetFalse() {
        val budget = DopamineBudget(
            date = LocalDate(2025, 6, 14), fpEarned = 0, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertFalse(enforcer.isTodayBudget(budget, tz))
    }

    @Test fun ensureTodayBudgetReturnsSameIfToday() {
        val budget = DopamineBudget(
            date = LocalDate(2025, 6, 15), fpEarned = 10, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        val result = enforcer.ensureTodayBudget(budget, tz)
        assertEquals(budget, result)
    }

    @Test fun ensureTodayBudgetCreatesNewIfOldDate() {
        val budget = DopamineBudget(
            date = LocalDate(2025, 6, 14), fpEarned = 10, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        val result = enforcer.ensureTodayBudget(budget, tz)
        assertEquals(LocalDate(2025, 6, 15), result.date)
    }

    @Test fun ensureTodayBudgetCreatesNewIfNull() {
        val result = enforcer.ensureTodayBudget(null, tz)
        assertEquals(LocalDate(2025, 6, 15), result.date)
    }
}

class BudgetEnforcerGateTest {
    private val fixedInstant = LocalDateTime(2025, 6, 15, 10, 0).toInstant(TimeZone.UTC)
    private val clock = FakeClock(fixedInstant)
    private val enforcer = BudgetEnforcer(clock = clock)
    private val tz = TimeZone.UTC

    private fun todayBudget(fpEarned: Int = 0, fpSpent: Int = 0) = DopamineBudget(
        date = LocalDate(2025, 6, 15), fpEarned = fpEarned, fpSpent = fpSpent, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun gatePermittedWhenPositiveBalance() {
        val budget = todayBudget(fpEarned = 10) // balance = 15+10=25 > 0
        val gate = enforcer.evaluateGate(budget, EnforcementMode.NUDGE, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.Permitted)
    }

    @Test fun gateLowBalanceWhenNegativeNudge() {
        val budget = todayBudget(fpSpent = 20) // balance = 15-20 = -5
        val gate = enforcer.evaluateGate(budget, EnforcementMode.NUDGE, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.LowBalance)
        assertEquals(-5, (gate as BudgetEnforcer.EnforcementGate.LowBalance).balance)
    }

    @Test fun gateHardBlockedWhenNegativeHardLock() {
        val budget = todayBudget(fpSpent = 20)
        val gate = enforcer.evaluateGate(budget, EnforcementMode.HARD_LOCK, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.HardBlocked)
    }

    @Test fun gateNoBudgetForTodayWhenOldDate() {
        val oldBudget = DopamineBudget(
            date = LocalDate(2025, 6, 14), fpEarned = 0, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        val gate = enforcer.evaluateGate(oldBudget, EnforcementMode.NUDGE, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.NoBudgetForToday)
    }

    @Test fun gateZeroBalanceHardLock() {
        val budget = todayBudget(fpSpent = 15) // balance = 15-15 = 0
        val gate = enforcer.evaluateGate(budget, EnforcementMode.HARD_LOCK, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.HardBlocked)
    }

    @Test fun gateZeroBalanceNudge() {
        val budget = todayBudget(fpSpent = 15)
        val gate = enforcer.evaluateGate(budget, EnforcementMode.NUDGE, tz)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.LowBalance)
    }
}

class BudgetEnforcerRolloverAndSummaryTest {
    private val fixedInstant = LocalDateTime(2025, 6, 15, 10, 0).toInstant(TimeZone.UTC)
    private val clock = FakeClock(fixedInstant)
    private val enforcer = BudgetEnforcer(clock = clock)

    private val baseBudget = DopamineBudget(
        date = LocalDate(2025, 6, 15), fpEarned = 20, fpSpent = 5, fpBonus = 3,
        fpRolloverIn = 2, fpRolloverOut = 0, nutritiveMinutes = 30,
        emptyCalorieMinutes = 10, neutralMinutes = 5
    )

    @Test fun computeRollover() {
        val rollover = enforcer.computeRollover(baseBudget)
        assertTrue(rollover > 0)
    }

    @Test fun finalizeDayBudget() {
        val finalized = enforcer.finalizeDayBudget(baseBudget)
        assertEquals(enforcer.computeRollover(baseBudget), finalized.fpRolloverOut)
    }

    @Test fun getDailySummaryBasic() {
        val summary = enforcer.getDailySummary(baseBudget)
        assertEquals(baseBudget.date, summary.date)
        assertEquals(20, summary.fpEarned)
        assertEquals(5, summary.fpSpent)
        assertEquals(3, summary.fpBonus)
        assertEquals(2, summary.fpRolloverIn)
        assertEquals(30, summary.nutritiveMinutes)
        assertEquals(10, summary.emptyCalorieMinutes)
    }

    @Test fun getDailySummaryEarnCapNotHit() {
        val summary = enforcer.getDailySummary(baseBudget)
        assertFalse(summary.isEarnCapHit)
        assertEquals(FPEconomy.DAILY_EARN_CAP - 20, summary.earnCapRemaining)
    }

    @Test fun getDailySummaryEarnCapHit() {
        val capped = baseBudget.copy(fpEarned = FPEconomy.DAILY_EARN_CAP)
        val summary = enforcer.getDailySummary(capped)
        assertTrue(summary.isEarnCapHit)
        assertEquals(0, summary.earnCapRemaining)
    }

    @Test fun dailySummaryEarnCapPercent() {
        val summary = enforcer.getDailySummary(baseBudget)
        val expected = ((20f / FPEconomy.DAILY_EARN_CAP) * 100).toInt()
        assertEquals(expected, summary.earnCapPercent)
    }

    @Test fun dailySummaryEarnCapPercentCapped() {
        val over = baseBudget.copy(fpEarned = FPEconomy.DAILY_EARN_CAP + 10)
        val summary = enforcer.getDailySummary(over)
        assertEquals(100, summary.earnCapPercent)
    }

    @Test fun dailySummaryEarnCapPercentZero() {
        val zero = baseBudget.copy(fpEarned = 0)
        val summary = enforcer.getDailySummary(zero)
        assertEquals(0, summary.earnCapPercent)
    }

    @Test fun dailySummaryDataClass() {
        val s1 = BudgetEnforcer.DailySummary(
            date = LocalDate(2025, 1, 1), balance = 10, fpEarned = 5,
            fpSpent = 2, fpBonus = 1, fpRolloverIn = 0, projectedRolloverOut = 5,
            earnCapRemaining = 55, isEarnCapHit = false, nutritiveMinutes = 10,
            emptyCalorieMinutes = 5
        )
        val s2 = s1.copy()
        assertEquals(s1, s2)
    }
}

class BudgetEnforcerDefaultTzTest {
    // Test methods with default timeZone parameter to cover synthetic $default wrappers
    @Test fun resetForNewDayDefaultTz() {
        val enforcer = BudgetEnforcer()
        val budget = enforcer.resetForNewDay(null)
        assertNotNull(budget)
    }

    @Test fun isTodayBudgetDefaultTz() {
        val enforcer = BudgetEnforcer()
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val budget = DopamineBudget(
            date = today, fpEarned = 0, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        assertTrue(enforcer.isTodayBudget(budget))
    }

    @Test fun ensureTodayBudgetDefaultTz() {
        val enforcer = BudgetEnforcer()
        val budget = enforcer.ensureTodayBudget(null)
        assertNotNull(budget)
    }

    @Test fun evaluateGateDefaultTz() {
        val enforcer = BudgetEnforcer()
        val today = kotlinx.datetime.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault()).date
        val budget = DopamineBudget(
            date = today, fpEarned = 10, fpSpent = 0, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
            emptyCalorieMinutes = 0, neutralMinutes = 0
        )
        val gate = enforcer.evaluateGate(budget, EnforcementMode.NUDGE)
        assertTrue(gate is BudgetEnforcer.EnforcementGate.Permitted)
    }
}

class BudgetEnforcerFillMissingDaysTest {
    private val fixedInstant = LocalDateTime(2025, 6, 15, 10, 0).toInstant(TimeZone.UTC)
    private val clock = FakeClock(fixedInstant)
    private val enforcer = BudgetEnforcer(clock = clock)

    private fun budget(day: Int) = DopamineBudget(
        date = LocalDate(2025, 1, day), fpEarned = 10, fpSpent = 0, fpBonus = 0,
        fpRolloverIn = 0, fpRolloverOut = 0, nutritiveMinutes = 0,
        emptyCalorieMinutes = 0, neutralMinutes = 0
    )

    @Test fun fillMissingDaysBasic() {
        val budgets = listOf(budget(1), budget(3))
        val result = enforcer.fillMissingDays(budgets, LocalDate(2025, 1, 1), LocalDate(2025, 1, 3))
        assertEquals(3, result.size)
        assertEquals(10, result[0].fpEarned) // Day 1 (existing)
        assertEquals(0, result[1].fpEarned)  // Day 2 (filled)
        assertEquals(10, result[2].fpEarned) // Day 3 (existing)
    }

    @Test fun fillMissingDaysEmpty() {
        val result = enforcer.fillMissingDays(emptyList(), LocalDate(2025, 1, 1), LocalDate(2025, 1, 3))
        assertTrue(result.isEmpty())
    }

    @Test fun fillMissingDaysNoGaps() {
        val budgets = listOf(budget(1), budget(2), budget(3))
        val result = enforcer.fillMissingDays(budgets, LocalDate(2025, 1, 1), LocalDate(2025, 1, 3))
        assertEquals(3, result.size)
        assertTrue(result.all { it.fpEarned == 10 })
    }

    @Test fun enforcementGatePermitted() {
        val gate = BudgetEnforcer.EnforcementGate.Permitted
        assertTrue(gate is BudgetEnforcer.EnforcementGate.Permitted)
    }

    @Test fun enforcementGateLowBalance() {
        val gate = BudgetEnforcer.EnforcementGate.LowBalance(-5)
        assertEquals(-5, gate.balance)
    }

    @Test fun enforcementGateHardBlocked() {
        val gate = BudgetEnforcer.EnforcementGate.HardBlocked(-10)
        assertEquals(-10, gate.balance)
    }

    @Test fun enforcementGateNoBudgetForToday() {
        val gate = BudgetEnforcer.EnforcementGate.NoBudgetForToday
        assertTrue(gate is BudgetEnforcer.EnforcementGate.NoBudgetForToday)
    }
}
