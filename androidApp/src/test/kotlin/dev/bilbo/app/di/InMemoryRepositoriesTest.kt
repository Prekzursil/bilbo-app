package dev.bilbo.app.di

import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.SuggestionRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AnalogSuggestion
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.AppProfile
import dev.bilbo.domain.DopamineBudget
import dev.bilbo.domain.EmotionalCheckIn
import dev.bilbo.domain.Emotion
import dev.bilbo.domain.EnforcementMode
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.domain.SuggestionCategory
import dev.bilbo.domain.TimeOfDay
import dev.bilbo.domain.UsageSession
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the in-memory repository stub implementations exposed through
 * [RepositoryModule].  Since the concrete classes are private we access them
 * via the public interfaces returned by the module providers.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class InMemoryRepositoriesTest {

    // ── Helpers ──────────────────────────────────────────────────────────────

    private val t0 = Instant.fromEpochSeconds(1_700_000_000L)
    private val t1 = Instant.fromEpochSeconds(1_700_001_000L)
    private val t2 = Instant.fromEpochSeconds(1_700_002_000L)

    private fun session(
        pkg: String = "com.app",
        cat: AppCategory = AppCategory.EMPTY_CALORIES,
        start: Instant = t0,
    ) = UsageSession(
        packageName = pkg,
        appLabel = pkg,
        category = cat,
        startTime = start,
        endTime = null,
        durationSeconds = 60,
    )

    private fun profile(pkg: String, cat: AppCategory = AppCategory.EMPTY_CALORIES) =
        AppProfile(packageName = pkg, appLabel = pkg, category = cat, enforcementMode = EnforcementMode.NUDGE)

    private fun declaration(pkg: String = "com.app", minutes: Int = 5, ts: Instant = t0) =
        IntentDeclaration(timestamp = ts, declaredApp = pkg, declaredDurationMinutes = minutes)

    private fun checkIn(ts: Instant = t0, linkedIntent: Long? = null) =
        EmotionalCheckIn(timestamp = ts, preSessionEmotion = Emotion.BORED, linkedIntentId = linkedIntent)

    private fun budget(date: LocalDate = LocalDate(2024, 1, 1)) =
        DopamineBudget(date, 10, 2, 0, 0, 0, 30, 10, 5)

    private fun suggestion(cat: SuggestionCategory = SuggestionCategory.EXERCISE, tod: TimeOfDay? = null) =
        AnalogSuggestion(text = "Go for a run", category = cat, tags = emptyList(), timeOfDay = tod)

    // ════════════════════════════════════════════════════════════════════════
    // UsageRepository
    // ════════════════════════════════════════════════════════════════════════

    private lateinit var usageRepo: UsageRepository

    @Before
    fun setUp() {
        usageRepo = RepositoryModule.provideUsageRepository()
    }

    @Test
    fun `UsageRepository - insert and getAll returns inserted session`() = runTest {
        usageRepo.insert(session())
        val all = usageRepo.getAll()
        assertEquals(1, all.size)
        assertEquals("com.app", all[0].packageName)
    }

    @Test
    fun `UsageRepository - insert assigns sequential ids`() = runTest {
        val id1 = usageRepo.insert(session("a"))
        val id2 = usageRepo.insert(session("b"))
        assertTrue(id2 > id1)
    }

    @Test
    fun `UsageRepository - getById returns correct session`() = runTest {
        val id = usageRepo.insert(session("com.test"))
        val result = usageRepo.getById(id)
        assertNotNull(result)
        assertEquals("com.test", result.packageName)
    }

    @Test
    fun `UsageRepository - getById returns null for missing id`() = runTest {
        assertNull(usageRepo.getById(999L))
    }

    @Test
    fun `UsageRepository - getByPackageName filters correctly`() = runTest {
        usageRepo.insert(session("com.a"))
        usageRepo.insert(session("com.b"))
        val result = usageRepo.getByPackageName("com.a")
        assertEquals(1, result.size)
        assertEquals("com.a", result[0].packageName)
    }

    @Test
    fun `UsageRepository - getByDateRange returns sessions in range`() = runTest {
        usageRepo.insert(session(start = t0))
        usageRepo.insert(session(start = t2))
        val result = usageRepo.getByDateRange(t0, t1)
        assertEquals(1, result.size)
    }

    @Test
    fun `UsageRepository - getByCategory filters by category`() = runTest {
        usageRepo.insert(session(cat = AppCategory.NUTRITIVE))
        usageRepo.insert(session(cat = AppCategory.EMPTY_CALORIES))
        val result = usageRepo.getByCategory(AppCategory.NUTRITIVE)
        assertEquals(1, result.size)
    }

    @Test
    fun `UsageRepository - getByDateRangeAndCategory filters both`() = runTest {
        usageRepo.insert(session(cat = AppCategory.NUTRITIVE, start = t0))
        usageRepo.insert(session(cat = AppCategory.EMPTY_CALORIES, start = t0))
        usageRepo.insert(session(cat = AppCategory.NUTRITIVE, start = t2))
        val result = usageRepo.getByDateRangeAndCategory(t0, t1, AppCategory.NUTRITIVE)
        assertEquals(1, result.size)
    }

    @Test
    fun `UsageRepository - updateEndTime updates matching session`() = runTest {
        val id = usageRepo.insert(session())
        usageRepo.updateEndTime(id, t1, 120L)
        val updated = usageRepo.getById(id)
        assertEquals(120L, updated?.durationSeconds)
        assertEquals(t1, updated?.endTime)
    }

    @Test
    fun `UsageRepository - updateEndTime with non-existent id does not crash`() = runTest {
        usageRepo.updateEndTime(999L, t1, 100L) // should not throw
    }

    @Test
    fun `UsageRepository - deleteById removes session`() = runTest {
        val id = usageRepo.insert(session())
        usageRepo.deleteById(id)
        assertNull(usageRepo.getById(id))
    }

    @Test
    fun `UsageRepository - deleteOlderThan removes old sessions`() = runTest {
        usageRepo.insert(session(start = t0))
        usageRepo.insert(session(start = t2))
        usageRepo.deleteOlderThan(t1)
        val all = usageRepo.getAll()
        assertEquals(1, all.size)
        assertEquals(t2, all[0].startTime)
    }

    @Test
    fun `UsageRepository - countByPackageName returns correct count`() = runTest {
        usageRepo.insert(session("com.a"))
        usageRepo.insert(session("com.a"))
        usageRepo.insert(session("com.b"))
        assertEquals(2L, usageRepo.countByPackageName("com.a"))
    }

    @Test
    fun `UsageRepository - sumDurationByCategory sums correctly`() = runTest {
        usageRepo.insert(session(cat = AppCategory.NUTRITIVE).copy(durationSeconds = 100))
        usageRepo.insert(session(cat = AppCategory.NUTRITIVE).copy(durationSeconds = 50))
        usageRepo.insert(session(cat = AppCategory.EMPTY_CALORIES).copy(durationSeconds = 200))
        val result = usageRepo.sumDurationByCategory(t0, t2)
        assertEquals(150L, result[AppCategory.NUTRITIVE])
        assertEquals(200L, result[AppCategory.EMPTY_CALORIES])
    }

    @Test
    fun `UsageRepository - observeAll flow emits after insert`() = runTest {
        val before = usageRepo.observeAll().first()
        assertEquals(0, before.size)
        usageRepo.insert(session())
        val after = usageRepo.observeAll().first()
        assertEquals(1, after.size)
    }

    // ════════════════════════════════════════════════════════════════════════
    // AppProfileRepository
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `AppProfileRepository - insert and getAll`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app1"))
        val all = repo.getAll()
        assertEquals(1, all.size)
    }

    @Test
    fun `AppProfileRepository - getByPackageName returns correct profile`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.x", AppCategory.NUTRITIVE))
        val result = repo.getByPackageName("com.x")
        assertNotNull(result)
        assertEquals(AppCategory.NUTRITIVE, result.category)
    }

    @Test
    fun `AppProfileRepository - getByPackageName returns null for unknown`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        assertNull(repo.getByPackageName("com.unknown"))
    }

    @Test
    fun `AppProfileRepository - getByCategory filters correctly`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.a", AppCategory.NUTRITIVE))
        repo.insert(profile("com.b", AppCategory.EMPTY_CALORIES))
        val result = repo.getByCategory(AppCategory.NUTRITIVE)
        assertEquals(1, result.size)
    }

    @Test
    fun `AppProfileRepository - getByEnforcementMode filters correctly`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.nudge").copy(enforcementMode = EnforcementMode.NUDGE))
        repo.insert(profile("com.hard").copy(enforcementMode = EnforcementMode.HARD_LOCK))
        val result = repo.getByEnforcementMode(EnforcementMode.HARD_LOCK)
        assertEquals(1, result.size)
    }

    @Test
    fun `AppProfileRepository - getBypassed returns only bypassed profiles`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.a").copy(isBypassed = false))
        repo.insert(profile("com.b").copy(isBypassed = true))
        val result = repo.getBypassed()
        assertEquals(1, result.size)
        assertEquals("com.b", result[0].packageName)
    }

    @Test
    fun `AppProfileRepository - getCustomClassified returns only custom ones`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.a").copy(isCustomClassification = false))
        repo.insert(profile("com.b").copy(isCustomClassification = true))
        val result = repo.getCustomClassified()
        assertEquals(1, result.size)
    }

    @Test
    fun `AppProfileRepository - update replaces profile`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app", AppCategory.EMPTY_CALORIES))
        repo.update(profile("com.app", AppCategory.NUTRITIVE))
        val result = repo.getByPackageName("com.app")
        assertEquals(AppCategory.NUTRITIVE, result?.category)
    }

    @Test
    fun `AppProfileRepository - upsert inserts new and updates existing`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.upsert(profile("com.app", AppCategory.EMPTY_CALORIES))
        assertEquals(1, repo.getAll().size)
        repo.upsert(profile("com.app", AppCategory.NUTRITIVE))
        assertEquals(1, repo.getAll().size)
        assertEquals(AppCategory.NUTRITIVE, repo.getByPackageName("com.app")?.category)
    }

    @Test
    fun `AppProfileRepository - updateCategory sets custom flag`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app", AppCategory.EMPTY_CALORIES))
        repo.updateCategory("com.app", AppCategory.NUTRITIVE)
        val updated = repo.getByPackageName("com.app")
        assertEquals(AppCategory.NUTRITIVE, updated?.category)
        assertTrue(updated?.isCustomClassification == true)
    }

    @Test
    fun `AppProfileRepository - updateCategory on unknown package does not crash`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.updateCategory("com.unknown", AppCategory.NUTRITIVE) // should not throw
    }

    @Test
    fun `AppProfileRepository - updateBypass toggles isBypassed`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app"))
        repo.updateBypass("com.app", true)
        assertTrue(repo.getByPackageName("com.app")?.isBypassed == true)
        repo.updateBypass("com.app", false)
        assertFalse(repo.getByPackageName("com.app")?.isBypassed == true)
    }

    @Test
    fun `AppProfileRepository - updateBypass on unknown package does not crash`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.updateBypass("com.unknown", true) // should not throw
    }

    @Test
    fun `AppProfileRepository - deleteByPackageName removes profile`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app"))
        repo.deleteByPackageName("com.app")
        assertNull(repo.getByPackageName("com.app"))
    }

    @Test
    fun `AppProfileRepository - observeAll and observeByPackageName emit`() = runTest {
        val repo: AppProfileRepository = RepositoryModule.provideAppProfileRepository()
        repo.insert(profile("com.app"))
        assertEquals(1, repo.observeAll().first().size)
        assertNotNull(repo.observeByPackageName("com.app").first())
    }

    // ════════════════════════════════════════════════════════════════════════
    // IntentRepository
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `IntentRepository - insert and getAll`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration())
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `IntentRepository - getById returns correct record`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration("com.app", 10))
        val result = repo.getById(id)
        assertNotNull(result)
        assertEquals(10, result.declaredDurationMinutes)
    }

    @Test
    fun `IntentRepository - getById returns null for missing`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        assertNull(repo.getById(999L))
    }

    @Test
    fun `IntentRepository - getByApp filters by package`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration("com.a"))
        repo.insert(declaration("com.b"))
        assertEquals(1, repo.getByApp("com.a").size)
    }

    @Test
    fun `IntentRepository - getByDateRange filters by timestamp`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration(ts = t0))
        repo.insert(declaration(ts = t2))
        val result = repo.getByDateRange(t0, t1)
        assertEquals(1, result.size)
    }

    @Test
    fun `IntentRepository - getOverridden returns only overridden`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration().copy(wasOverridden = false))
        val id2 = repo.insert(declaration().copy(wasOverridden = false))
        repo.updateEnforcement(id2, true, EnforcementMode.NUDGE, true)
        val result = repo.getOverridden()
        assertEquals(1, result.size)
    }

    @Test
    fun `IntentRepository - updateActualDuration updates correctly`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration(minutes = 5))
        repo.updateActualDuration(id, 7)
        assertEquals(7, repo.getById(id)?.actualDurationMinutes)
    }

    @Test
    fun `IntentRepository - updateActualDuration with missing id does not crash`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.updateActualDuration(999L, 5) // should not throw
    }

    @Test
    fun `IntentRepository - updateEnforcement updates fields`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration())
        repo.updateEnforcement(id, true, EnforcementMode.HARD_LOCK, false)
        val updated = repo.getById(id)
        assertTrue(updated?.wasEnforced == true)
        assertEquals(EnforcementMode.HARD_LOCK, updated?.enforcementType)
    }

    @Test
    fun `IntentRepository - updateEnforcement with missing id does not crash`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.updateEnforcement(999L, true, EnforcementMode.NUDGE, false) // should not throw
    }

    @Test
    fun `IntentRepository - deleteById removes record`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration())
        repo.deleteById(id)
        assertNull(repo.getById(id))
    }

    @Test
    fun `IntentRepository - countAccurate returns correct count`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration(minutes = 10, ts = t0))
        repo.updateActualDuration(id, 10) // exact match — accurate
        assertEquals(1L, repo.countAccurate(t0, t1))
    }

    @Test
    fun `IntentRepository - countAccurate excludes zero declared duration`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val id = repo.insert(declaration(minutes = 0, ts = t0))
        repo.updateActualDuration(id, 5)
        assertEquals(0L, repo.countAccurate(t0, t1))
    }

    @Test
    fun `IntentRepository - countAccurate excludes null actualDuration`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration(minutes = 10, ts = t0))
        // No updateActualDuration call — actual remains null
        assertEquals(0L, repo.countAccurate(t0, t1))
    }

    @Test
    fun `IntentRepository - countTotal returns total in range`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        repo.insert(declaration(ts = t0))
        repo.insert(declaration(ts = t0))
        repo.insert(declaration(ts = t2))
        assertEquals(2L, repo.countTotal(t0, t1))
    }

    @Test
    fun `IntentRepository - observeAll flow emits`() = runTest {
        val repo: IntentRepository = RepositoryModule.provideIntentRepository()
        val list = repo.observeAll().first()
        assertNotNull(list)
    }

    // ════════════════════════════════════════════════════════════════════════
    // EmotionRepository
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `EmotionRepository - insert and getAll`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        repo.insert(checkIn())
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `EmotionRepository - getById returns correct record`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        val id = repo.insert(checkIn())
        assertNotNull(repo.getById(id))
    }

    @Test
    fun `EmotionRepository - getById returns null for missing`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        assertNull(repo.getById(999L))
    }

    @Test
    fun `EmotionRepository - getByDateRange filters correctly`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        repo.insert(checkIn(ts = t0))
        repo.insert(checkIn(ts = t2))
        assertEquals(1, repo.getByDateRange(t0, t1).size)
    }

    @Test
    fun `EmotionRepository - getByIntentId returns check-in with matching intentId`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        repo.insert(checkIn(linkedIntent = null))
        val id = repo.insert(checkIn(linkedIntent = 42L))
        val result = repo.getByIntentId(42L)
        assertNotNull(result)
        assertEquals(42L, result.linkedIntentId)
    }

    @Test
    fun `EmotionRepository - getByIntentId returns null for no match`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        repo.insert(checkIn(linkedIntent = 1L))
        assertNull(repo.getByIntentId(999L))
    }

    @Test
    fun `EmotionRepository - updatePostMood updates field`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        val id = repo.insert(checkIn())
        repo.updatePostMood(id, Emotion.HAPPY)
        assertEquals(Emotion.HAPPY, repo.getById(id)?.postSessionMood)
    }

    @Test
    fun `EmotionRepository - updatePostMood with missing id does not crash`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        repo.updatePostMood(999L, Emotion.CALM) // should not throw
    }

    @Test
    fun `EmotionRepository - deleteById removes record`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        val id = repo.insert(checkIn())
        repo.deleteById(id)
        assertNull(repo.getById(id))
    }

    @Test
    fun `EmotionRepository - observeAll emits`() = runTest {
        val repo: EmotionRepository = RepositoryModule.provideEmotionRepository()
        assertNotNull(repo.observeAll().first())
    }

    // ════════════════════════════════════════════════════════════════════════
    // BudgetRepository
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `BudgetRepository - insert and getAll`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        repo.insert(budget())
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `BudgetRepository - getByDate returns correct budget`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 3, 15)
        repo.insert(budget(d))
        assertNotNull(repo.getByDate(d))
    }

    @Test
    fun `BudgetRepository - getByDate returns null for missing date`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        assertNull(repo.getByDate(LocalDate(2099, 1, 1)))
    }

    @Test
    fun `BudgetRepository - getByDateRange filters correctly`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d1 = LocalDate(2024, 1, 1)
        val d2 = LocalDate(2024, 1, 5)
        val d3 = LocalDate(2024, 1, 10)
        repo.insert(budget(d1))
        repo.insert(budget(d2))
        repo.insert(budget(d3))
        val result = repo.getByDateRange(d1, d2)
        assertEquals(2, result.size)
    }

    @Test
    fun `BudgetRepository - getRecent returns most recent N`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        repeat(5) { i -> repo.insert(budget(LocalDate(2024, 1, i + 1))) }
        assertEquals(3, repo.getRecent(3L).size)
    }

    @Test
    fun `BudgetRepository - update replaces budget`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d).copy(fpEarned = 5))
        repo.update(budget(d).copy(fpEarned = 20))
        assertEquals(20, repo.getByDate(d)?.fpEarned)
    }

    @Test
    fun `BudgetRepository - upsert inserts and updates`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.upsert(budget(d).copy(fpEarned = 5))
        assertEquals(1, repo.getAll().size)
        repo.upsert(budget(d).copy(fpEarned = 99))
        assertEquals(1, repo.getAll().size)
        assertEquals(99, repo.getByDate(d)?.fpEarned)
    }

    @Test
    fun `BudgetRepository - incrementFpEarned adds to existing`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d).copy(fpEarned = 10))
        repo.incrementFpEarned(d, 5)
        assertEquals(15, repo.getByDate(d)?.fpEarned)
    }

    @Test
    fun `BudgetRepository - incrementFpEarned creates default entry when date absent`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2025, 6, 1)
        repo.incrementFpEarned(d, 10)
        assertEquals(10, repo.getByDate(d)?.fpEarned)
    }

    @Test
    fun `BudgetRepository - incrementFpSpent adds to existing`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d).copy(fpSpent = 2))
        repo.incrementFpSpent(d, 3)
        assertEquals(5, repo.getByDate(d)?.fpSpent)
    }

    @Test
    fun `BudgetRepository - incrementFpSpent creates default entry when date absent`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2025, 7, 1)
        repo.incrementFpSpent(d, 8)
        assertEquals(8, repo.getByDate(d)?.fpSpent)
    }

    @Test
    fun `BudgetRepository - incrementFpBonus adds to existing`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d).copy(fpBonus = 0))
        repo.incrementFpBonus(d, 3)
        assertEquals(3, repo.getByDate(d)?.fpBonus)
    }

    @Test
    fun `BudgetRepository - incrementFpBonus creates default entry when date absent`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2025, 8, 1)
        repo.incrementFpBonus(d, 5)
        assertEquals(5, repo.getByDate(d)?.fpBonus)
    }

    @Test
    fun `BudgetRepository - deleteByDate removes entry`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d))
        repo.deleteByDate(d)
        assertNull(repo.getByDate(d))
    }

    @Test
    fun `BudgetRepository - sumFpEarned sums over range`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        repo.insert(budget(LocalDate(2024, 1, 1)).copy(fpEarned = 10))
        repo.insert(budget(LocalDate(2024, 1, 2)).copy(fpEarned = 15))
        repo.insert(budget(LocalDate(2024, 1, 5)).copy(fpEarned = 20))
        val sum = repo.sumFpEarned(LocalDate(2024, 1, 1), LocalDate(2024, 1, 3))
        assertEquals(25L, sum)
    }

    @Test
    fun `BudgetRepository - observeAll and observeByDate emit`() = runTest {
        val repo: BudgetRepository = RepositoryModule.provideBudgetRepository()
        val d = LocalDate(2024, 1, 1)
        repo.insert(budget(d))
        assertEquals(1, repo.observeAll().first().size)
        assertNotNull(repo.observeByDate(d).first())
    }

    // ════════════════════════════════════════════════════════════════════════
    // SuggestionRepository
    // ════════════════════════════════════════════════════════════════════════

    @Test
    fun `SuggestionRepository - insert and getAll`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion())
        assertEquals(1, repo.getAll().size)
    }

    @Test
    fun `SuggestionRepository - getById returns correct record`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        val id = repo.insert(suggestion())
        assertNotNull(repo.getById(id))
    }

    @Test
    fun `SuggestionRepository - getById returns null for missing`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        assertNull(repo.getById(999L))
    }

    @Test
    fun `SuggestionRepository - getByCategory filters correctly`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion(SuggestionCategory.EXERCISE))
        repo.insert(suggestion(SuggestionCategory.COOKING))
        assertEquals(1, repo.getByCategory(SuggestionCategory.EXERCISE).size)
    }

    @Test
    fun `SuggestionRepository - getByTimeOfDay returns null-tod and matching-tod`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion(tod = null))           // always included
        repo.insert(suggestion(tod = TimeOfDay.MORNING))
        repo.insert(suggestion(tod = TimeOfDay.EVENING))
        val result = repo.getByTimeOfDay(TimeOfDay.MORNING)
        assertEquals(2, result.size)
    }

    @Test
    fun `SuggestionRepository - getByCategoryAndTimeOfDay filters both`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion(SuggestionCategory.EXERCISE, null))
        repo.insert(suggestion(SuggestionCategory.EXERCISE, TimeOfDay.MORNING))
        repo.insert(suggestion(SuggestionCategory.COOKING, TimeOfDay.MORNING))
        val result = repo.getByCategoryAndTimeOfDay(SuggestionCategory.EXERCISE, TimeOfDay.MORNING)
        assertEquals(2, result.size)
    }

    @Test
    fun `SuggestionRepository - getCustom returns only custom`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion().copy(isCustom = false))
        repo.insert(suggestion().copy(isCustom = true))
        assertEquals(1, repo.getCustom().size)
    }

    @Test
    fun `SuggestionRepository - getTopAccepted returns up to limit sorted`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repeat(5) { i -> repo.insert(suggestion().copy(timesAccepted = i)) }
        val top = repo.getTopAccepted(3L)
        assertEquals(3, top.size)
        assertTrue(top[0].timesAccepted >= top[1].timesAccepted)
    }

    @Test
    fun `SuggestionRepository - update replaces record`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        val id = repo.insert(suggestion().copy(text = "old"))
        val existing = repo.getById(id)!!
        repo.update(existing.copy(text = "new"))
        assertEquals("new", repo.getById(id)?.text)
    }

    @Test
    fun `SuggestionRepository - update with unknown id does not crash`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.update(suggestion().copy(id = 999L)) // should not throw
    }

    @Test
    fun `SuggestionRepository - recordShown increments timesShown`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        val id = repo.insert(suggestion())
        repo.recordShown(id)
        repo.recordShown(id)
        assertEquals(2, repo.getById(id)?.timesShown)
    }

    @Test
    fun `SuggestionRepository - recordShown with unknown id does not crash`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.recordShown(999L) // should not throw
    }

    @Test
    fun `SuggestionRepository - recordAccepted increments timesAccepted`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        val id = repo.insert(suggestion())
        repo.recordAccepted(id)
        assertEquals(1, repo.getById(id)?.timesAccepted)
    }

    @Test
    fun `SuggestionRepository - recordAccepted with unknown id does not crash`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.recordAccepted(999L) // should not throw
    }

    @Test
    fun `SuggestionRepository - deleteById removes record`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        val id = repo.insert(suggestion())
        repo.deleteById(id)
        assertNull(repo.getById(id))
    }

    @Test
    fun `SuggestionRepository - observeAll emits`() = runTest {
        val repo: SuggestionRepository = RepositoryModule.provideSuggestionRepository()
        repo.insert(suggestion())
        assertEquals(1, repo.observeAll().first().size)
    }
}
