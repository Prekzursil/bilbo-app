package dev.bilbo.app.worker

import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules and cancels [SyncWorker] periodic tasks via WorkManager.
 *
 * Call [scheduleSyncWork] once from application startup (after Hilt injection),
 * then [cancelSyncWork] if the user signs out.
 */
@Singleton
class WorkManagerScheduler @Inject constructor(
    private val workManager: WorkManager,
) {

    fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            repeatInterval = 1L,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 15L,
            flexTimeIntervalUnit = TimeUnit.MINUTES,
        )
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                30L,
                TimeUnit.MINUTES,
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest,
        )
    }

    fun cancelSyncWork() {
        workManager.cancelUniqueWork(SyncWorker.WORK_NAME)
    }
}
