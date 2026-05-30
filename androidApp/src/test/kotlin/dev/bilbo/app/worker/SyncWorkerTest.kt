package dev.bilbo.app.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import dev.bilbo.domain.WeeklyInsight
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.shared.data.repository.InsightRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * Unit tests for [SyncWorker.doWork] using [TestListenableWorkerBuilder].
 *
 * Covers:
 *  - doWork returns [Result.success] when InsightRepository succeeds
 *  - doWork returns [Result.retry] on first exception (runAttemptCount = 0 < MAX_RETRY)
 *  - doWork returns [Result.failure] once MAX_RETRY_ATTEMPTS is exceeded
 *  - getAllWeeklyInsights is called exactly once per doWork invocation
 *  - WORK_NAME constant value
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SyncWorkerTest {

    private lateinit var context: Context
    private val insightRepository = mockk<InsightRepository>(relaxed = true)

    private val weeklyInsight = WeeklyInsight(
        weekStart = LocalDate(2025, 1, 1),
        tier2Insights = emptyList(),
        tier3Narrative = null,
        totalScreenTimeMinutes = 90,
        nutritiveMinutes = 30,
        emptyCalorieMinutes = 60,
        fpEarned = 10,
        fpSpent = 5,
        intentAccuracyPercent = 80f,
        streakDays = 3,
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        coEvery { insightRepository.getAllWeeklyInsights() } returns emptyList()
    }

    private fun buildWorker(runAttemptCount: Int = 0): SyncWorker {
        return TestListenableWorkerBuilder<SyncWorker>(context)
            .setWorkerFactory(SyncWorkerFactory(insightRepository))
            .setRunAttemptCount(runAttemptCount)
            .build()
    }

    // ── Success path ──────────────────────────────────────────────────────────

    @Test
    fun `doWork returns success when repository succeeds with empty list`() {
        val result = runBlocking { buildWorker().doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork returns success when repository returns multiple insights`() {
        coEvery { insightRepository.getAllWeeklyInsights() } returns listOf(weeklyInsight)
        val result = runBlocking { buildWorker().doWork() }
        assertEquals(Result.success(), result)
    }

    @Test
    fun `doWork calls getAllWeeklyInsights exactly once`() {
        runBlocking { buildWorker().doWork() }
        coVerify(exactly = 1) { insightRepository.getAllWeeklyInsights() }
    }

    // ── Retry path ────────────────────────────────────────────────────────────

    @Test
    fun `doWork returns retry when repository throws on first attempt`() {
        coEvery { insightRepository.getAllWeeklyInsights() } throws RuntimeException("network error")
        val result = runBlocking { buildWorker(runAttemptCount = 0).doWork() }
        assertEquals(Result.retry(), result)
    }

    @Test
    fun `doWork returns retry on second attempt when under MAX_RETRY_ATTEMPTS`() {
        coEvery { insightRepository.getAllWeeklyInsights() } throws RuntimeException("db error")
        val result = runBlocking { buildWorker(runAttemptCount = 2).doWork() }
        assertEquals(Result.retry(), result)
    }

    // ── Failure path ──────────────────────────────────────────────────────────

    @Test
    fun `doWork returns failure when runAttemptCount exceeds MAX_RETRY_ATTEMPTS`() {
        coEvery { insightRepository.getAllWeeklyInsights() } throws RuntimeException("persistent error")
        // MAX_RETRY_ATTEMPTS = 3; runAttemptCount = 3 means 4th attempt
        val result = runBlocking { buildWorker(runAttemptCount = 3).doWork() }
        assertEquals(Result.failure(), result)
    }

    @Test
    fun `doWork returns failure on any attempt above threshold`() {
        coEvery { insightRepository.getAllWeeklyInsights() } throws RuntimeException("error")
        val result = runBlocking { buildWorker(runAttemptCount = 5).doWork() }
        assertEquals(Result.failure(), result)
    }

    // ── WORK_NAME constant ────────────────────────────────────────────────────

    @Test
    fun `WORK_NAME constant has expected value`() {
        assertEquals("bilbo_sync_worker", SyncWorker.WORK_NAME)
    }
}

// ── Worker factory ────────────────────────────────────────────────────────────

private class SyncWorkerFactory(
    private val insightRepository: InsightRepository,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters,
    ): androidx.work.ListenableWorker? {
        if (workerClassName != SyncWorker::class.java.name) return null
        return SyncWorker(appContext, workerParameters, insightRepository)
    }
}
