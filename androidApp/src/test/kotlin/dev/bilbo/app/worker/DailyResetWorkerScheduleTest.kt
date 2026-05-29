package dev.bilbo.app.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Robolectric unit test for the [DailyResetWorker] companion scheduler helpers.
 * Covers both `schedule` (enqueue with UPDATE policy) and `cancel` (delegated
 * cancellation). The worker's `doWork` body itself requires the full Hilt
 * graph and is exercised by the instrumentation suite — see the build script's
 * Kover excludes notes.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DailyResetWorkerScheduleTest {
    @Test
    fun `schedule enqueues unique periodic work with UPDATE policy`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val captor = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(any<String>(), any(), capture(captor))
        } returns mockk(relaxed = true)

        DailyResetWorker.schedule(workManager)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                DailyResetWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any(),
            )
        }
        assertTrue(captor.isCaptured)
    }

    @Test
    fun `cancel delegates to WorkManager with WORK_NAME`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        DailyResetWorker.cancel(workManager)
        verify(exactly = 1) { workManager.cancelUniqueWork(DailyResetWorker.WORK_NAME) }
    }

    @Test
    fun `WORK_NAME is the expected stable identifier`() {
        assertEquals("bilbo_daily_reset", DailyResetWorker.WORK_NAME)
    }
}
