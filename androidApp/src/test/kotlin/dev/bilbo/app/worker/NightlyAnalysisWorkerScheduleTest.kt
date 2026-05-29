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
class NightlyAnalysisWorkerScheduleTest {
    @Test
    fun `schedule enqueues unique periodic work with UPDATE policy`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val captor = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(any<String>(), any(), capture(captor))
        } returns mockk(relaxed = true)

        NightlyAnalysisWorker.schedule(workManager)

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                NightlyAnalysisWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                any(),
            )
        }
        assertTrue(captor.isCaptured)
    }

    @Test
    fun `cancel delegates to WorkManager with WORK_NAME`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        NightlyAnalysisWorker.cancel(workManager)
        verify(exactly = 1) { workManager.cancelUniqueWork(NightlyAnalysisWorker.WORK_NAME) }
    }

    @Test
    fun `WORK_NAME stable identifier`() {
        assertEquals("bilbo_nightly_analysis", NightlyAnalysisWorker.WORK_NAME)
    }
}
