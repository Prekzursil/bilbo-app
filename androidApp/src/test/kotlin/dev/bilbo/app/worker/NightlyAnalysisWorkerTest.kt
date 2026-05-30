package dev.bilbo.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.AppCategory
import dev.bilbo.domain.Emotion
import dev.bilbo.domain.EmotionalCheckIn
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.InsightType
import dev.bilbo.domain.UsageSession
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.shared.data.repository.InsightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Clock

/**
 * Unit tests for [NightlyAnalysisWorker] covering doWork result paths
 * and [buildCorrelationSummary] branches.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NightlyAnalysisWorkerTest {

    private lateinit var context: Context
    private val usageRepository = mockk<UsageRepository>(relaxed = true)
    private val emotionRepository = mockk<EmotionRepository>(relaxed = true)
    private val intentRepository = mockk<IntentRepository>(relaxed = true)
    private val insightRepository = mockk<InsightRepository>(relaxed = true)
    private val heuristicEngine = mockk<HeuristicEngine>(relaxed = true)

    private val now = Clock.System.now()
    private val tz = TimeZone.currentSystemDefault()
    private val today = now.toLocalDateTime(tz).date

    @Before
    fun setUp() {
        coEvery { usageRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { emotionRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { intentRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { insightRepository.storeHeuristicInsights(any(), any()) } returns Unit
        coEvery { insightRepository.updateCorrelationCache(any(), any()) } returns Unit
        every { heuristicEngine.analyzeWeek(any(), any(), any(), any(), any()) } returns emptyList()
    }

    private fun buildWorker(): NightlyAnalysisWorker =
        TestListenableWorkerBuilder<NightlyAnalysisWorker>(context)
            .setWorkerFactory(NightlyWorkerFactory(
                usageRepository, emotionRepository, intentRepository,
                insightRepository, heuristicEngine,
            ))
            .build()

    // ── doWork: success path ───────────────────────────────────────────────────

    @Test
    fun `doWork returns success with all empty repositories`() {
        context = ApplicationProvider.getApplicationContext()
        val result = kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork stores heuristic insights from heuristicEngine`() {
        context = ApplicationProvider.getApplicationContext()
        every { heuristicEngine.analyzeWeek(any(), any(), any(), any(), any()) } returns listOf(
            HeuristicInsight(type = InsightType.TREND, message = "Up trend", confidence = 0.8f),
            HeuristicInsight(type = InsightType.CORRELATION, message = "Corr", confidence = 0.7f),
        )
        kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        coVerify(exactly = 1) { insightRepository.storeHeuristicInsights(any(), any()) }
    }

    @Test
    fun `doWork invokes updateCorrelationCache`() {
        context = ApplicationProvider.getApplicationContext()
        kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        coVerify(exactly = 1) { insightRepository.updateCorrelationCache(any(), any()) }
    }

    // ── doWork: retry on exception ─────────────────────────────────────────────

    @Test
    fun `doWork returns retry when usageRepository throws`() {
        context = ApplicationProvider.getApplicationContext()
        coEvery { usageRepository.getByDateRange(any(), any()) } throws RuntimeException("db fail")
        val result = kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `doWork returns retry when heuristicEngine throws`() {
        context = ApplicationProvider.getApplicationContext()
        every { heuristicEngine.analyzeWeek(any(), any(), any(), any(), any()) } throws RuntimeException("engine crash")
        val result = kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        assertEquals(Result.retry(), result)
    }

    // ── buildCorrelationSummary branches ─────────────────────────────────────

    @Test
    fun `correlation summary is empty when sessions and checkIns are empty`() {
        context = ApplicationProvider.getApplicationContext()
        // Empty input → empty map
        coEvery { usageRepository.getByDateRange(any(), any()) } returns emptyList()
        coEvery { emotionRepository.getByDateRange(any(), any()) } returns emptyList()

        kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        coVerify {
            insightRepository.updateCorrelationCache(any(), match { it.isEmpty() })
        }
    }

    @Test
    fun `correlation summary includes stress rate when stressed checkIns present`() {
        context = ApplicationProvider.getApplicationContext()
        val start = now
        val end = now
        coEvery { usageRepository.getByDateRange(any(), any()) } returns listOf(
            UsageSession(
                packageName = "com.tiktok", appLabel = "TikTok",
                category = AppCategory.EMPTY_CALORIES,
                startTime = start, endTime = end, durationSeconds = 600L,
            ),
        )
        coEvery { emotionRepository.getByDateRange(any(), any()) } returns listOf(
            EmotionalCheckIn(timestamp = now, preSessionEmotion = Emotion.STRESSED),
            EmotionalCheckIn(timestamp = now, preSessionEmotion = Emotion.ANXIOUS),
            EmotionalCheckIn(timestamp = now, preSessionEmotion = Emotion.HAPPY),
        )

        kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        coVerify {
            insightRepository.updateCorrelationCache(
                any(),
                match { map ->
                    val rate = map["stress_to_empty_calorie_rate"] ?: 0f
                    // 2 stressed+anxious out of 3 total = ~0.667
                    rate > 0.5f && map.containsKey("empty_calorie_session_count")
                },
            )
        }
    }

    @Test
    fun `correlation summary with sessions but no checkIns returns empty map`() {
        context = ApplicationProvider.getApplicationContext()
        coEvery { usageRepository.getByDateRange(any(), any()) } returns listOf(
            UsageSession(
                packageName = "com.test", appLabel = "Test",
                category = AppCategory.NUTRITIVE,
                startTime = now, endTime = now, durationSeconds = 300L,
            ),
        )
        coEvery { emotionRepository.getByDateRange(any(), any()) } returns emptyList()

        kotlinx.coroutines.runBlocking { buildWorker().doWork() }
        coVerify {
            insightRepository.updateCorrelationCache(any(), match { it.isEmpty() })
        }
    }

    // ── schedule / cancel ─────────────────────────────────────────────────────

    @Test
    fun `schedule enqueues unique periodic work with UPDATE policy`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val requestSlot = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(any<String>(), any(), capture(requestSlot))
        } returns mockk(relaxed = true)

        NightlyAnalysisWorker.schedule(workManager)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                NightlyAnalysisWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any(),
            )
        }
        assertTrue(requestSlot.isCaptured)
    }

    @Test
    fun `cancel delegates to WorkManager with WORK_NAME`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        NightlyAnalysisWorker.cancel(workManager)
        verify(exactly = 1) { workManager.cancelUniqueWork(NightlyAnalysisWorker.WORK_NAME) }
    }

    @Test
    fun `WORK_NAME has expected value`() {
        assertEquals("bilbo_nightly_analysis", NightlyAnalysisWorker.WORK_NAME)
    }
}

// ── Worker factory ────────────────────────────────────────────────────────────

private class NightlyWorkerFactory(
    private val usageRepository: UsageRepository,
    private val emotionRepository: EmotionRepository,
    private val intentRepository: IntentRepository,
    private val insightRepository: InsightRepository,
    private val heuristicEngine: HeuristicEngine,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): androidx.work.ListenableWorker? {
        if (workerClassName != NightlyAnalysisWorker::class.java.name) return null
        return NightlyAnalysisWorker(
            appContext = appContext,
            workerParams = workerParameters,
            usageRepository = usageRepository,
            emotionRepository = emotionRepository,
            intentRepository = intentRepository,
            insightRepository = insightRepository,
            heuristicEngine = heuristicEngine,
        )
    }
}
