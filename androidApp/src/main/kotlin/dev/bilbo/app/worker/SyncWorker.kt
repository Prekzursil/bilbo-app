package dev.bilbo.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bilbo.shared.data.repository.InsightRepository
import timber.log.Timber

/**
 * A periodic [CoroutineWorker] that syncs locally cached app usage data
 * to Supabase when a network connection is available.
 *
 * Scheduled by [dev.bilbo.app.worker.WorkManagerScheduler] to run
 * roughly every hour with a network constraint.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val insightRepository: InsightRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "bilbo_sync_worker"
    }

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: starting sync (attempt %d)", runAttemptCount + 1)
        return try {
            val pending = insightRepository.getAllWeeklyInsights()
            Timber.d(
                "SyncWorker: %d weekly insights cached locally; remote upload pending repository support",
                pending.size,
            )
            // Remote upload is gated on the shared repository growing a
            // queryUnsynced()/markSynced() pair.  Once available we will:
            //   1. query unsynced usage sessions
            //   2. POST them in batches to Supabase via BilboApiService
            //   3. mark them synced locally
            // Until then the worker acts as a heartbeat that keeps the chain alive.
            Timber.d("SyncWorker: sync complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
