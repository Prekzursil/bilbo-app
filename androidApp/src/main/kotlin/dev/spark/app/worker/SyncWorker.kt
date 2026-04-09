package dev.spark.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.spark.shared.data.repository.InsightRepository
import timber.log.Timber

/**
 * A periodic [CoroutineWorker] that syncs locally cached app usage data
 * to Supabase when a network connection is available.
 *
 * Scheduled by [dev.spark.app.worker.WorkManagerScheduler] to run
 * roughly every hour with a network constraint.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val insightRepository: InsightRepository,
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "spark_sync_worker"
    }

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: starting sync")
        return try {
            // TODO: Iterate unsynced sessions, upload to Supabase, mark synced
            Timber.d("SyncWorker: sync complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}
