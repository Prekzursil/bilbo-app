package dev.spark.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dev.spark.data.BudgetRepository
import dev.spark.data.EmotionRepository
import dev.spark.data.IntentRepository
import dev.spark.data.UsageRepository
import dev.spark.domain.HeuristicInsight
import dev.spark.domain.InsightType
import dev.spark.domain.WeeklyInsight
import dev.spark.intelligence.tier2.HeuristicEngine
import dev.spark.intelligence.tier3.CloudInsightClient
import dev.spark.intelligence.tier3.InsightPromptBuilder
import dev.spark.shared.data.repository.InsightRepository
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
 * HiltWorker that fires once per week on Sunday evening (~8 PM local time).
 *
 * Responsibilities:
 *  1. Load trailing 7-day data from local repositories.
 *  2. Run Tier-2 [HeuristicEngine.analyzeWeek] to generate local insights.
 *  3. If cloud AI is enabled in preferences: build an anonymized payload via
 *     [InsightPromptBuilder], send to [CloudInsightClient], and store the narrative.
 *  4. If cloud is disabled or the call fails: use Tier-2 template insights as fallback.
 *  5. Persist the final [WeeklyInsight] in the repository.
 *  6. Post a "Your weekly insight is ready" notification.
 */
@HiltWorker
class WeeklyInsightWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val usageRepository: UsageRepository,
    private val emotionRepository: EmotionRepository,
    private val intentRepository: IntentRepository,
    private val budgetRepository: BudgetRepository,
    private val insightRepository: InsightRepository,
    private val heuristicEngine: HeuristicEngine,
    private val cloudInsightClient: CloudInsightClient,
    private val promptBuilder: InsightPromptBuilder,
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "spark_weekly_insight"
        private const val NOTIFICATION_CHANNEL_ID = "spark_insights"
        private const val NOTIFICATION_ID = 1002

        /**
         * Schedules the weekly insight worker to run Sunday evening (~8 PM).
         * Uses KEEP policy — if already scheduled, do not replace.
         */
        fun schedule(workManager: WorkManager) {
            val initialDelayMs = millisUntilSundayEvening()

            val request = PeriodicWorkRequestBuilder<WeeklyInsightWorker>(
                repeatInterval = 7L,
                repeatIntervalTimeUnit = TimeUnit.DAYS,
            )
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        fun cancel(workManager: WorkManager) {
            workManager.cancelUniqueWork(WORK_NAME)
        }

        /**
         * Computes milliseconds from now until next Sunday at 8 PM local time.
         */
        private fun millisUntilSundayEvening(): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            // Advance to the next Sunday
            val currentDow = target.get(Calendar.DAY_OF_WEEK) // Sunday=1
            val daysUntilSunday = if (currentDow == Calendar.SUNDAY) 0
            else (Calendar.SUNDAY + 7 - currentDow) % 7
            target.add(Calendar.DAY_OF_MONTH, daysUntilSunday)

            // If this Sunday 8 PM has already passed, advance 7 more days
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_MONTH, 7)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }

    // ── Worker entry point ────────────────────────────────────────────────────

    override suspend fun doWork(): Result {
        return runCatching { performWeeklyInsight() }
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() },
            )
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private suspend fun performWeeklyInsight() {
        val tz = TimeZone.currentSystemDefault()
        val today = Clock.System.now().toLocalDateTime(tz).date
        val weekStart = today.minus(DatePeriod(days = 6))

        // Convert LocalDate boundaries to Instant for repository queries
        val fromInstant = weekStart.atStartOfDayIn(tz)
        val toInstant = today.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
        val priorWeekStart = weekStart.minus(DatePeriod(days = 7))
        val priorFromInstant = priorWeekStart.atStartOfDayIn(tz)

        // 1. Load trailing 7-day data
        val sessions = usageRepository.getByDateRange(fromInstant, toInstant)
        val checkIns = emotionRepository.getByDateRange(fromInstant, toInstant)
        val intents = intentRepository.getByDateRange(fromInstant, toInstant)
        val priorSessions = usageRepository.getByDateRange(priorFromInstant, fromInstant)

        // 2. Run Tier-2 heuristic analysis (always — used as fallback and for cloud payload)
        val heuristicInsights = heuristicEngine.analyzeWeek(
            weekStart = weekStart,
            sessions = sessions,
            checkIns = checkIns,
            intents = intents,
            priorWeekSessions = priorSessions,
            timeZone = tz,
        )

        // 3. Compute aggregate stats
        val totalScreenTimeMinutes = (sessions.sumOf { it.durationSeconds } / 60L).toInt()
        val nutritiveMinutes = sessions
            .filter { it.category == dev.spark.domain.AppCategory.NUTRITIVE }
            .sumOf { it.durationSeconds / 60L }.toInt()
        val emptyCalorieMinutes = sessions
            .filter { it.category == dev.spark.domain.AppCategory.EMPTY_CALORIES }
            .sumOf { it.durationSeconds / 60L }.toInt()

        // Compute intent accuracy
        val completedIntents = intents.filter { it.actualDurationMinutes != null }
        val accurateIntents = completedIntents.count { intent ->
            val delta = kotlin.math.abs(
                (intent.actualDurationMinutes ?: 0) - intent.declaredDurationMinutes
            ).toDouble() / intent.declaredDurationMinutes.coerceAtLeast(1)
            delta <= 0.20
        }
        val intentAccuracy = if (completedIntents.isNotEmpty())
            accurateIntents.toFloat() / completedIntents.size else 0f

        val budgets = budgetRepository.getRecent(limit = 7)
        val fpEarned = budgets.sumOf { it.fpEarned }
        val fpSpent = budgets.sumOf { it.fpSpent }
        val streakDays = computeStreak(budgets)

        // Week-over-week change
        val thisWeekTotal = sessions.sumOf { it.durationSeconds }
        val priorWeekTotal = priorSessions.sumOf { it.durationSeconds }
        val weekOverWeekChange = if (priorWeekTotal > 0)
            (thisWeekTotal - priorWeekTotal).toDouble() / priorWeekTotal else null

        // 4. Check if cloud AI is enabled
        val cloudEnabled = isCloudAiEnabled()
        var tier3Narrative: String? = null

        if (cloudEnabled) {
            tier3Narrative = tryFetchCloudNarrative(
                weekStart = weekStart,
                budgets = budgets,
                heuristicInsights = heuristicInsights,
                checkIns = checkIns,
                sessions = sessions,
                weekOverWeekChange = weekOverWeekChange,
                totalScreenTimeMinutes = totalScreenTimeMinutes,
                nutritiveMinutes = nutritiveMinutes,
                emptyCalorieMinutes = emptyCalorieMinutes,
                fpEarned = fpEarned,
                fpSpent = fpSpent,
                intentAccuracy = intentAccuracy,
                streakDays = streakDays,
            )
        }

        // 5. If cloud disabled or failed, build Tier-2 template narrative
        if (tier3Narrative == null) {
            tier3Narrative = buildTier2Narrative(
                heuristicInsights = heuristicInsights,
                totalMinutes = totalScreenTimeMinutes,
                streakDays = streakDays,
                weekOverWeekChange = weekOverWeekChange,
            )
        }

        // 6. Build and persist WeeklyInsight
        val weeklyInsight = WeeklyInsight(
            weekStart = weekStart,
            tier2Insights = heuristicInsights,
            tier3Narrative = tier3Narrative,
            totalScreenTimeMinutes = totalScreenTimeMinutes,
            nutritiveMinutes = nutritiveMinutes,
            emptyCalorieMinutes = emptyCalorieMinutes,
            fpEarned = fpEarned,
            fpSpent = fpSpent,
            intentAccuracyPercent = intentAccuracy,
            streakDays = streakDays,
        )

        insightRepository.storeWeeklyInsight(weeklyInsight)

        // 7. Post notification
        postWeeklyInsightNotification()
    }

    // ── Cloud AI fetch ────────────────────────────────────────────────────────

    private suspend fun tryFetchCloudNarrative(
        weekStart: kotlinx.datetime.LocalDate,
        budgets: List<dev.spark.domain.DopamineBudget>,
        heuristicInsights: List<HeuristicInsight>,
        checkIns: List<dev.spark.domain.EmotionalCheckIn>,
        sessions: List<dev.spark.domain.UsageSession>,
        weekOverWeekChange: Double?,
        totalScreenTimeMinutes: Int,
        nutritiveMinutes: Int,
        emptyCalorieMinutes: Int,
        fpEarned: Int,
        fpSpent: Int,
        intentAccuracy: Float,
        streakDays: Int,
    ): String? {
        if (!cloudInsightClient.canRequest()) return null

        return try {
            val budget = budgets.firstOrNull() ?: return null
            val weeklyInsightForPayload = WeeklyInsight(
                weekStart = weekStart,
                tier2Insights = heuristicInsights,
                totalScreenTimeMinutes = totalScreenTimeMinutes,
                nutritiveMinutes = nutritiveMinutes,
                emptyCalorieMinutes = emptyCalorieMinutes,
                fpEarned = fpEarned,
                fpSpent = fpSpent,
                intentAccuracyPercent = intentAccuracy,
                streakDays = streakDays,
            )
            val payload = promptBuilder.buildPayload(
                weekStart = weekStart,
                budget = budget,
                insight = weeklyInsightForPayload,
                checkIns = checkIns,
                sessions = sessions,
                weekOverWeekChange = weekOverWeekChange,
            )

            val anonymousUserId = getAnonymousUserId()
            val result = cloudInsightClient.fetchNarrativeForWeek(payload, anonymousUserId)

            when (result) {
                is CloudInsightClient.InsightResult.Success -> result.narrative
                else -> null
            }
        } catch (e: Exception) {
            null // Silently fall back to Tier-2
        }
    }

    // ── Tier-2 narrative template ─────────────────────────────────────────────

    private fun buildTier2Narrative(
        heuristicInsights: List<HeuristicInsight>,
        totalMinutes: Int,
        streakDays: Int,
        weekOverWeekChange: Double?,
    ): String {
        val hours = totalMinutes / 60
        val mins = totalMinutes % 60
        val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

        val topInsight = heuristicInsights
            .filter { it.type == InsightType.ACHIEVEMENT || it.type == InsightType.TREND }
            .maxByOrNull { it.confidence }

        return buildString {
            append("This week you spent $timeStr on your phone.")
            if (streakDays >= 3) {
                append(" You kept a $streakDays-day streak — great consistency!")
            }
            weekOverWeekChange?.let { change ->
                val pct = (kotlin.math.abs(change) * 100).toInt()
                if (change < -0.05) append(" That's $pct% less than last week — nice progress!")
                else if (change > 0.10) append(" Usage was up $pct% from last week — worth watching.")
            }
            topInsight?.let { append(" ${it.message}") }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun computeStreak(budgets: List<dev.spark.domain.DopamineBudget>): Int {
        var streak = 0
        for (budget in budgets.sortedByDescending { it.date }) {
            if (budget.fpEarned > 0) streak++ else break
        }
        return streak
    }

    private fun isCloudAiEnabled(): Boolean {
        val prefs = applicationContext.getSharedPreferences("spark_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("cloud_ai_enabled", false)
    }

    private fun getAnonymousUserId(): String {
        val prefs = applicationContext.getSharedPreferences("spark_prefs", Context.MODE_PRIVATE)
        return prefs.getString("anon_user_id", null)
            ?: java.util.UUID.randomUUID().toString().also { uuid ->
                prefs.edit().putString("anon_user_id", uuid).apply()
            }
    }

    private fun postWeeklyInsightNotification() {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (no-op on < API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Spark Insights",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Weekly wellness summary from Spark"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent to open the insights screen
        val intent = applicationContext.packageManager
            .getLaunchIntentForPackage(applicationContext.packageName)
            ?.apply { putExtra("open_screen", "weekly_insight") }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(dev.spark.app.R.drawable.ic_notification)
            .setContentTitle("Your weekly insight is ready ✨")
            .setContentText("See how your screen time looked this week.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
