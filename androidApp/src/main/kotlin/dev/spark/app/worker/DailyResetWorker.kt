package dev.spark.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.spark.data.BudgetRepository
import dev.spark.data.UsageRepository
import dev.spark.economy.BudgetEnforcer
import dev.spark.intelligence.tier2.HeuristicEngine
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager worker that fires once per day, aligned as close to midnight as possible.
 *
 * Responsibilities:
 *  1. Retrieve yesterday's [DopamineBudget] from [BudgetRepository].
 *  2. Finalize it (set rolloverOut = 50 % of positive balance).
 *  3. Call [BudgetEnforcer.resetForNewDay] to create today's budget (baseline + rollover).
 *  4. Persist the new budget via [BudgetRepository.upsert].
 *  5. Trigger [HeuristicEngine.analyzeWeek] on Sundays for the Tier-2 nightly analysis.
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val budgetRepository: BudgetRepository,
    private val usageRepository: UsageRepository,
    private val budgetEnforcer: BudgetEnforcer,
    private val heuristicEngine: HeuristicEngine,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "spark_daily_reset"

        /**
         * Enqueues (or re-queues) the daily reset worker.
         * Uses [ExistingPeriodicWorkPolicy.UPDATE] so changes to the schedule take effect
         * without the user needing to reinstall the app.
         *
         * The initial delay is calculated to bring the first execution as close to the
         * next local midnight as possible.
         */
        fun schedule(workManager: WorkManager) {
            val initialDelayMs = millisUntilMidnight()

            val request = PeriodicWorkRequestBuilder<DailyResetWorker>(
                repeatInterval = 24L,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        /** Cancel any pending daily-reset work (e.g. on sign-out). */
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        /** Returns the number of milliseconds from now until the next midnight. */
        private fun millisUntilMidnight(): Long {
            val now = Calendar.getInstance()
            val midnight = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.DAY_OF_MONTH, 1)          // advance to tomorrow midnight
            }
            return midnight.timeInMillis - now.timeInMillis
        }
    }

    // ── Worker entry point ────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return runCatching { performDailyReset() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private suspend fun performDailyReset() {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date

        // 1. Load yesterday's budget (if it exists).
        val yesterday = budgetRepository.getRecent(limit = 1).firstOrNull()

        // 2. If yesterday's budget exists and is not already finalized, finalize it.
        val finalizedYesterday = yesterday?.let { budgetEnforcer.finalizeDayBudget(it) }
        if (finalizedYesterday != null && finalizedYesterday != yesterday) {
            budgetRepository.upsert(finalizedYesterday)
        }

        // 3. Create & persist today's budget (baseline 15 FP + rollover).
        val todayBudget = budgetEnforcer.resetForNewDay(finalizedYesterday, tz)
        budgetRepository.upsert(todayBudget)

        // 4. Tier-2 nightly analysis — run on Sundays (day-of-week == 7 in kotlinx-datetime).
        val dayOfWeek = today.dayOfWeek.ordinal  // Mon=0 … Sun=6
        if (dayOfWeek == 6) {
            triggerWeeklyAnalysis(today)
        }
    }

    /**
     * Triggers the Tier-2 [HeuristicEngine.analyzeWeek] for the completed week.
     * Data is fetched from the repositories; results are logged (or could be persisted).
     */
    private suspend fun triggerWeeklyAnalysis(today: kotlinx.datetime.LocalDate) {
        val weekStart = today.minus(kotlinx.datetime.DatePeriod(days = 6))

        val sessions = usageRepository.getByDateRange(weekStart, today)
        val priorWeekStart = weekStart.minus(kotlinx.datetime.DatePeriod(days = 7))
        val priorSessions = usageRepository.getByDateRange(priorWeekStart, weekStart)

        heuristicEngine.analyzeWeek(
            weekStart = weekStart,
            sessions = sessions,
            checkIns = emptyList(),   // EmotionRepository injected separately if needed
            intents = emptyList(),
            priorWeekSessions = priorSessions,
        )
    }
}
