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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WeeklyInsightWorkerScheduleTest {
    @Test
    fun `schedule enqueues unique periodic work with KEEP policy`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val captor = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(any<String>(), any(), capture(captor))
        } returns mockk(relaxed = true)

        WeeklyInsightWorker.schedule(workManager)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                WeeklyInsightWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                any(),
            )
        }
        assertTrue(captor.isCaptured)
    }

    @Test
    fun `cancel delegates to WorkManager with WORK_NAME`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        WeeklyInsightWorker.cancel(workManager)
        verify(exactly = 1) { workManager.cancelUniqueWork(WeeklyInsightWorker.WORK_NAME) }
    }

    @Test
    fun `WORK_NAME stable identifier`() {
        assertEquals("bilbo_weekly_insight", WeeklyInsightWorker.WORK_NAME)
    }
}
