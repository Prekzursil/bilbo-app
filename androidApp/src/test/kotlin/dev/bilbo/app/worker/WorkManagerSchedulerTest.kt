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

/**
 * Unit tests for [WorkManagerScheduler]. WorkManager builders require some
 * Android platform classes (Data, OutOfQuotaPolicy) so we use Robolectric
 * to provide them — the WorkManager itself is mocked.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkManagerSchedulerTest {
    @Test
    fun `scheduleSyncWork enqueues unique periodic work with KEEP policy`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val requestSlot = slot<PeriodicWorkRequest>()
        every {
            workManager.enqueueUniquePeriodicWork(any<String>(), any(), capture(requestSlot))
        } returns mockk(relaxed = true)
        val scheduler = WorkManagerScheduler(workManager)

        scheduler.scheduleSyncWork()

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                SyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                any(),
            )
        }
        // The request was built — just sanity-check we got a non-null one.
        assertEquals(true, requestSlot.isCaptured)
    }

    @Test
    fun `cancelSyncWork delegates to WorkManager with WORK_NAME`() {
        val workManager = mockk<WorkManager>(relaxed = true)
        val scheduler = WorkManagerScheduler(workManager)

        scheduler.cancelSyncWork()

        verify(exactly = 1) { workManager.cancelUniqueWork(SyncWorker.WORK_NAME) }
    }
}
