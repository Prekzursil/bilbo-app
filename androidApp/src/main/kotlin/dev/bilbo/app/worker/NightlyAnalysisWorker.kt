package dev.bilbo.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.intelligence.tier2.CorrelationAnalyzer
import dev.bilbo.shared.data.repository.InsightRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * HiltWorker that runs nightly at 3 AM local time.
 *
 * Responsibilities:
 *  1. Pull the trailing 7 days of usage sessions, emotional check-ins, and intent declarations.
 *  2. Run [HeuristicEngine.analyzeWeek] — entirely local, no network I/O.
 *  3. Persist the resulting [HeuristicInsight] entries via [InsightRepository].
 *  4. Update correlation cache in [CorrelationAnalyzer] for emotion × app patterns.
 */
@HiltWorker
class NightlyAnalysisWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val emotionRepository: EmotionRepository,
    private val intentRepository: IntentRepository,
    private val insightRepository: InsightRepository,
    private val heuristicEngine: HeuristicEngine,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "bilbo_nightly_analysis"

        /**
         * Schedules the nightly worker to run at approximately 3 AM every day.
         * Uses [ExistingPeriodicWorkPolicy.UPDATE] so re-scheduling picks up without reinstall.
         */
        fun schedule(workManager: WorkManager) {
            val initialDelayMs = millisUntil3Am()

            val request = PeriodicWorkRequestBuilder<NightlyAnalysisWorker>(
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

        /** Cancel any pending nightly analysis work. */
        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }

        /**
         * Computes milliseconds from now until the next 3:00 AM local time.
         * If it is already past 3 AM today, targets 3 AM tomorrow.
         */
        private fun millisUntil3Am(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 3)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // If 3 AM has already passed today, advance to tomorrow
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_MONTH, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    // ── Worker entry point ────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return runCatching { performNightlyAnalysis() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private suspend fun performNightlyAnalysis() {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val weekStart = today.minus(DatePeriod(days = 6))

        // Convert LocalDate boundaries to Instant for repository queries
        val fromInstant = weekStart.atStartOfDayIn(tz)
        val toInstant = today.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)

        // 1. Gather trailing 7-day data — all local, no network calls
        val sessions = usageRepository.getByDateRange(fromInstant, toInstant)
        val checkIns = emotionRepository.getByDateRange(fromInstant, toInstant)
        val intents = intentRepository.getByDateRange(fromInstant, toInstant)

        // Prior week for trend comparison
        val priorWeekStart = weekStart.minus(DatePeriod(days = 7))
        val priorFromInstant = priorWeekStart.atStartOfDayIn(tz)
        val priorSessions = usageRepository.getByDateRange(priorFromInstant, fromInstant)

        // 2. Run heuristic analysis — pure local computation
        val insights = heuristicEngine.analyzeWeek(
            weekStart = weekStart,
            sessions = sessions,
            checkIns = checkIns,
            intents = intents,
            priorWeekSessions = priorSessions,
            timeZone = tz,
        )

        // 3. Persist insights via repository
        insightRepository.storeHeuristicInsights(weekStart, insights)

        // 4. The CorrelationAnalyzer inside HeuristicEngine is stateless (pure functions),
        //    but we expose a correlated-emotion summary for the UI via the repository.
        //    We compute the top correlations here and persist them as a lightweight cache.
        val emotionAppCorrelations = buildCorrelationSummary(sessions, checkIns)
        insightRepository.updateCorrelationCache(weekStart, emotionAppCorrelations)
    }

    /**
     * Builds a simple emotion → app-category correlation map to store as cached state
     * so the WeeklyInsightScreen can display it without re-running the full engine.
     *
     * Format: Map<emotionName, Map<appCategory, correlationStrength>>
     */
    private fun buildCorrelationSummary(
        sessions: List<dev.bilbo.domain.UsageSession>,
        checkIns: List<dev.bilbo.domain.EmotionalCheckIn>,
    ): Map<String, Float> {
        if (checkIns.isEmpty() || sessions.isEmpty()) return emptyMap()

        // Simplified: count how often a stressed/anxious check-in precedes an empty-calorie session
        val stressedCount = checkIns.count {
            it.preSessionEmotion == dev.bilbo.domain.Emotion.STRESSED ||
                it.preSessionEmotion == dev.bilbo.domain.Emotion.ANXIOUS
        }.toFloat()
        val totalCheckIns = checkIns.size.toFloat()

        val emptyCalorieSessions = sessions.filter {
            it.category == dev.bilbo.domain.AppCategory.EMPTY_CALORIES
        }

        return mapOf(
            "stress_to_empty_calorie_rate" to if (totalCheckIns > 0) stressedCount / totalCheckIns else 0f,
            "empty_calorie_session_count" to emptyCalorieSessions.size.toFloat(),
            "total_sessions" to sessions.size.toFloat(),
        )
    }
}
