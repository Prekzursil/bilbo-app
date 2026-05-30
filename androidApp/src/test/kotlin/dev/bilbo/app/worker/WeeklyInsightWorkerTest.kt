package dev.bilbo.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.testing.TestListenableWorkerBuilder
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.DopamineBudget
import dev.bilbo.domain.EmotionalCheckIn
import dev.bilbo.domain.Emotion
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.InsightType
import dev.bilbo.domain.IntentDeclaration
import dev.bilbo.domain.UsageSession
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.intelligence.tier3.CloudInsightClient
import dev.bilbo.intelligence.tier3.InsightPromptBuilder
import dev.bilbo.shared.data.repository.InsightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Unit tests for [WeeklyInsightWorker.doWork] covering all result/branch paths.
 * Uses [TestListenableWorkerBuilder] to invoke the worker on Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeeklyInsightWorkerTest {

    private lateinit var context: Context
    private val usageRepository = mockk<UsageRepository>(relaxed = true)
    private val emotionRepository = mockk<EmotionRepository>(relaxed = true)
    private val intentRepository = mockk<IntentRepository>(relaxed = true)
    private val budgetRepository = mockk<BudgetRepository>(relaxed = true)
    private val insightRepository = mockk<InsightRepository>(relaxed = true)
    private val heuristicEngine = mockk<HeuristicEngine>(relaxed = true)
    private val cloudInsightClient = mockk<CloudInsightClient>(relaxed = true)
    private val promptBuilder = mockk<InsightPromptBuilder>(relaxed = true)

    private val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private val now = Clock.System.now()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Default: repositories return empty lists
        coEvery { usageRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { emotionRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { intentRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { budgetRepository.getRecent(any()) } returns emptyList()
        coEvery { insightRepository.storeWeeklyInsight(any()) } returns Unit

        // Default: heuristic engine returns empty insights
        every {
            heuristicEngine.analyzeWeek(any(), any(), any(), any(), any())
        } returns emptyList()

        // Default: cloud AI disabled (no SharedPrefs key)
        every { cloudInsightClient.canRequest() } returns false
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun buildWorker(): WeeklyInsightWorker {
        val deps = WeeklyInsightDependencies(
            usageRepository = usageRepository,
            emotionRepository = emotionRepository,
            intentRepository = intentRepository,
            budgetRepository = budgetRepository,
            insightRepository = insightRepository,
            heuristicEngine = heuristicEngine,
            cloudInsightClient = cloudInsightClient,
            promptBuilder = promptBuilder,
        )
        return TestListenableWorkerBuilder<WeeklyInsightWorker>(context)
            .setWorkerFactory(WeeklyInsightWorkerFactory(deps))
            .build()
    }

    // ── Happy path: cloud disabled → Tier-2 narrative ────────────────────────

    @Test
    fun `doWork returns success with empty data and cloud disabled`() {
        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork stores weekly insight even with empty repositories`() {
        val worker = buildWorker()
        kotlinx.coroutines.runBlocking { worker.doWork() }
        coVerify(exactly = 1) { insightRepository.storeWeeklyInsight(any()) }
    }

    // ── Tier-2 narrative branches ─────────────────────────────────────────────

    @Test
    fun `doWork builds narrative with streak mention when streakDays ge 3`() {
        // Budget with positive fpEarned for 5 consecutive days → streak = 5
        val budgets = (0..4).map { i ->
            DopamineBudget(
                date = today.minus(DatePeriod(days = i)),
                fpEarned = 5, fpSpent = 0, fpBonus = 0,
                fpRolloverIn = 0, fpRolloverOut = 0,
                nutritiveMinutes = 0, emptyCalorieMinutes = 0, neutralMinutes = 0,
            )
        }
        coEvery { budgetRepository.getRecent(any()) } returns budgets

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
        coVerify { insightRepository.storeWeeklyInsight(match { it.streakDays == 5 }) }
    }

    @Test
    fun `doWork week-over-week improvement branch`() {
        val tz = TimeZone.currentSystemDefault()
        // Prior week: 3600s; this week: 1800s → -50% change
        val priorStart = today.minus(DatePeriod(days = 13))
        val thisStart = today.minus(DatePeriod(days = 6))

        val priorInstant = priorStart.atTime(0, 0).toInstant(tz)
        val thisInstant = thisStart.atTime(0, 0).toInstant(tz)
        val endInstant = today.atTime(23, 59).toInstant(tz)

        coEvery { usageRepository.getByDateRange(thisInstant, any()) } returns listOf(
            UsageSession(
                packageName = "com.test", appLabel = "Test",
                category = AppCategory.EMPTY_CALORIES,
                startTime = thisInstant, endTime = endInstant,
                durationSeconds = 1800L,
            ),
        )
        coEvery { usageRepository.getByDateRange(priorInstant, thisInstant) } returns listOf(
            UsageSession(
                packageName = "com.test", appLabel = "Test",
                category = AppCategory.EMPTY_CALORIES,
                startTime = priorInstant, endTime = thisInstant,
                durationSeconds = 3600L,
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork week-over-week regression branch`() {
        val tz = TimeZone.currentSystemDefault()
        val priorStart = today.minus(DatePeriod(days = 13))
        val thisStart = today.minus(DatePeriod(days = 6))

        val priorInstant = priorStart.atTime(0, 0).toInstant(tz)
        val thisInstant = thisStart.atTime(0, 0).toInstant(tz)
        val endInstant = today.atTime(23, 59).toInstant(tz)

        coEvery { usageRepository.getByDateRange(thisInstant, any()) } returns listOf(
            UsageSession(
                packageName = "com.test", appLabel = "Test",
                category = AppCategory.EMPTY_CALORIES,
                startTime = thisInstant, endTime = endInstant,
                durationSeconds = 7200L, // more this week
            ),
        )
        coEvery { usageRepository.getByDateRange(priorInstant, thisInstant) } returns listOf(
            UsageSession(
                packageName = "com.test", appLabel = "Test",
                category = AppCategory.EMPTY_CALORIES,
                startTime = priorInstant, endTime = thisInstant,
                durationSeconds = 3600L, // less last week
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork topInsight appended when achievement insight present`() {
        every {
            heuristicEngine.analyzeWeek(any(), any(), any(), any(), any())
        } returns listOf(
            HeuristicInsight(
                type = InsightType.ACHIEVEMENT,
                message = "You did great!",
                confidence = 0.9f,
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    // ── Intent accuracy branches ───────────────────────────────────────────────

    @Test
    fun `doWork computes intent accuracy with accurate intents`() {
        val base = now
        coEvery { intentRepository.getByDateRange(any(), any()) } returns listOf(
            IntentDeclaration(
                id = 1L, timestamp = base,
                declaredApp = "com.test",
                declaredDurationMinutes = 30,
                actualDurationMinutes = 29, // within 20% tolerance
            ),
            IntentDeclaration(
                id = 2L, timestamp = base,
                declaredApp = "com.test",
                declaredDurationMinutes = 30,
                actualDurationMinutes = 60, // outside tolerance
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork computes intent accuracy with no completed intents`() {
        coEvery { intentRepository.getByDateRange(any(), any()) } returns listOf(
            IntentDeclaration(
                id = 1L, timestamp = now,
                declaredApp = "com.test",
                declaredDurationMinutes = 30,
                actualDurationMinutes = null, // not completed
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
        coVerify { insightRepository.storeWeeklyInsight(match { it.intentAccuracyPercent == 0f }) }
    }

    // ── Cloud AI path ─────────────────────────────────────────────────────────

    @Test
    fun `doWork uses cloud narrative when canRequest returns true and budget exists`() {
        // Enable cloud AI in SharedPreferences so isCloudAiEnabled() returns true
        context.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("cloud_ai_enabled", true).commit()
        every { cloudInsightClient.canRequest() } returns true
        val budget = DopamineBudget(
            date = today,
            fpEarned = 10, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0,
            nutritiveMinutes = 10, emptyCalorieMinutes = 5, neutralMinutes = 0,
        )
        coEvery { budgetRepository.getRecent(any()) } returns listOf(budget)

        val mockPayload = mockk<InsightPromptBuilder.WeeklySummaryPayload>(relaxed = true)
        coEvery { promptBuilder.buildPayload(any(), any(), any(), any(), any(), any()) } returns mockPayload
        coEvery {
            cloudInsightClient.fetchNarrativeForWeek(any(), any())
        } returns CloudInsightClient.InsightResult.Success("Cloud narrative text")

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
        coVerify { insightRepository.storeWeeklyInsight(match { it.tier3Narrative == "Cloud narrative text" }) }
    }

    @Test
    fun `doWork falls back to tier2 when cloud returns non-success`() {
        context.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("cloud_ai_enabled", true).commit()
        every { cloudInsightClient.canRequest() } returns true
        val budget = DopamineBudget(
            date = today,
            fpEarned = 10, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0,
            nutritiveMinutes = 10, emptyCalorieMinutes = 5, neutralMinutes = 0,
        )
        coEvery { budgetRepository.getRecent(any()) } returns listOf(budget)

        val mockPayload = mockk<InsightPromptBuilder.WeeklySummaryPayload>(relaxed = true)
        coEvery { promptBuilder.buildPayload(any(), any(), any(), any(), any(), any()) } returns mockPayload
        coEvery {
            cloudInsightClient.fetchNarrativeForWeek(any(), any())
        } returns CloudInsightClient.InsightResult.RateLimited

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
        // tier2 narrative is non-null (starts with "This week")
        coVerify { insightRepository.storeWeeklyInsight(match { it.tier3Narrative?.startsWith("This week") == true }) }
    }

    @Test
    fun `doWork falls back to tier2 when cloud throws exception`() {
        context.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("cloud_ai_enabled", true).commit()
        every { cloudInsightClient.canRequest() } returns true
        val budget = DopamineBudget(
            date = today,
            fpEarned = 10, fpSpent = 5, fpBonus = 0,
            fpRolloverIn = 0, fpRolloverOut = 0,
            nutritiveMinutes = 10, emptyCalorieMinutes = 5, neutralMinutes = 0,
        )
        coEvery { budgetRepository.getRecent(any()) } returns listOf(budget)
        val mockPayload = mockk<InsightPromptBuilder.WeeklySummaryPayload>(relaxed = true)
        coEvery { promptBuilder.buildPayload(any(), any(), any(), any(), any(), any()) } returns mockPayload
        coEvery {
            cloudInsightClient.fetchNarrativeForWeek(any(), any())
        } throws RuntimeException("network down")

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork falls back to tier2 when canRequest true but no budget`() {
        context.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("cloud_ai_enabled", true).commit()
        every { cloudInsightClient.canRequest() } returns true
        coEvery { budgetRepository.getRecent(any()) } returns emptyList()

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
    }

    // ── Failure / retry path ──────────────────────────────────────────────────

    @Test
    fun `doWork returns retry when a repository throws`() {
        coEvery { usageRepository.getByDateRange(any(), any()) } throws RuntimeException("db error")

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.retry(), result)
    }

    // ── minutesInCategory branches ────────────────────────────────────────────

    @Test
    fun `doWork counts nutritive and empty-calorie minutes correctly`() {
        val tz = TimeZone.currentSystemDefault()
        val start = today.atTime(9, 0).toInstant(tz)
        val end = today.atTime(10, 0).toInstant(tz)
        coEvery { usageRepository.getByDateRange(any(), any()) } returns listOf(
            UsageSession(
                packageName = "com.nutritive", appLabel = "Nutritive",
                category = AppCategory.NUTRITIVE,
                startTime = start, endTime = end, durationSeconds = 3600L,
            ),
            UsageSession(
                packageName = "com.empty", appLabel = "Empty",
                category = AppCategory.EMPTY_CALORIES,
                startTime = start, endTime = end, durationSeconds = 1800L,
            ),
        )

        val worker = buildWorker()
        val result = kotlinx.coroutines.runBlocking { worker.doWork() }
        assertEquals(Result.success(), result)
        coVerify {
            insightRepository.storeWeeklyInsight(match {
                it.nutritiveMinutes == 60 && it.emptyCalorieMinutes == 30
            })
        }
    }
}

// ── Worker factory for TestListenableWorkerBuilder ────────────────────────────

private class WeeklyInsightWorkerFactory(
    private val deps: WeeklyInsightDependencies,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ): androidx.work.ListenableWorker? {
        if (workerClassName != WeeklyInsightWorker::class.java.name) return null
        return WeeklyInsightWorker(appContext, workerParameters, deps)
    }
}
