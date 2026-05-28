package dev.bilbo.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
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
import dev.bilbo.data.BudgetRepository
import dev.bilbo.data.EmotionRepository
import dev.bilbo.data.IntentRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.domain.HeuristicInsight
import dev.bilbo.domain.InsightType
import dev.bilbo.domain.WeeklyInsight
import dev.bilbo.intelligence.tier2.HeuristicEngine
import dev.bilbo.intelligence.tier3.CloudInsightClient
import dev.bilbo.intelligence.tier3.InsightPromptBuilder
import dev.bilbo.shared.data.repository.InsightRepository
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.time.Clock

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
/**
 * Bundles the repositories and intelligence collaborators the weekly worker needs,
 * so the worker's assisted constructor stays within the parameter-count budget.
 */
class WeeklyInsightDependencies
    @javax.inject.Inject
    constructor(
        val usageRepository: UsageRepository,
        val emotionRepository: EmotionRepository,
        val intentRepository: IntentRepository,
        val budgetRepository: BudgetRepository,
        val insightRepository: InsightRepository,
        val heuristicEngine: HeuristicEngine,
        val cloudInsightClient: CloudInsightClient,
        val promptBuilder: InsightPromptBuilder,
    )

@HiltWorker
class WeeklyInsightWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted workerParams: WorkerParameters,
        private val deps: WeeklyInsightDependencies,
    ) : CoroutineWorker(appContext, workerParams) {
        private val usageRepository get() = deps.usageRepository
        private val emotionRepository get() = deps.emotionRepository
        private val intentRepository get() = deps.intentRepository
        private val budgetRepository get() = deps.budgetRepository
        private val insightRepository get() = deps.insightRepository
        private val heuristicEngine get() = deps.heuristicEngine
        private val cloudInsightClient get() = deps.cloudInsightClient
        private val promptBuilder get() = deps.promptBuilder

        companion object {
            const val WORK_NAME = "bilbo_weekly_insight"
            private const val NOTIFICATION_CHANNEL_ID = "bilbo_insights"
            private const val NOTIFICATION_ID = 1002

            private const val DAYS_PER_WEEK = 7
            private const val DAYS_PER_WEEK_L = 7L
            private const val WINDOW_DAYS = 6
            private const val SECONDS_PER_MINUTE = 60L
            private const val MINUTES_PER_HOUR = 60
            private const val RECENT_BUDGET_LIMIT = 7L
            private const val SUNDAY_EVENING_HOUR = 20
            private const val MIN_STREAK_FOR_MENTION = 3
            private const val ACCURACY_TOLERANCE = 0.20
            private const val IMPROVEMENT_THRESHOLD = -0.05
            private const val REGRESSION_THRESHOLD = 0.10
            private const val PERCENT_SCALE = 100

            /**
             * Schedules the weekly insight worker to run Sunday evening (~8 PM).
             * Uses KEEP policy — if already scheduled, do not replace.
             */
            fun schedule(workManager: WorkManager) {
                val initialDelayMs = millisUntilSundayEvening()

                val request =
                    PeriodicWorkRequestBuilder<WeeklyInsightWorker>(
                        repeatInterval = DAYS_PER_WEEK_L,
                        repeatIntervalTimeUnit = TimeUnit.DAYS,
                    ).setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
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
                val target =
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, SUNDAY_EVENING_HOUR)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                // Advance to the next Sunday
                val currentDow = target.get(Calendar.DAY_OF_WEEK) // Sunday=1
                val daysUntilSunday =
                    if (currentDow == Calendar.SUNDAY) {
                        0
                    } else {
                        (Calendar.SUNDAY + DAYS_PER_WEEK - currentDow) % DAYS_PER_WEEK
                    }
                target.add(Calendar.DAY_OF_MONTH, daysUntilSunday)

                // If this Sunday 8 PM has already passed, advance a full week more
                if (target.timeInMillis <= now.timeInMillis) {
                    target.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK)
                }
                return target.timeInMillis - now.timeInMillis
            }
        }

        /** Aggregated weekly statistics derived from the trailing 7-day window. */
        private data class WeekStats(
            val totalScreenTimeMinutes: Int,
            val nutritiveMinutes: Int,
            val emptyCalorieMinutes: Int,
            val intentAccuracy: Float,
            val fpEarned: Int,
            val fpSpent: Int,
            val streakDays: Int,
            val weekOverWeekChange: Double?,
        )

        // ── Worker entry point ────────────────────────────────────────────────────

        override suspend fun doWork(): Result =
            runCatching { performWeeklyInsight() }
                .fold(
                    onSuccess = { Result.success() },
                    onFailure = { Result.retry() },
                )

        // ── Core logic ────────────────────────────────────────────────────────────

        private suspend fun performWeeklyInsight() {
            val tz = TimeZone.currentSystemDefault()
            val today =
                Clock.System
                    .now()
                    .toLocalDateTime(tz)
                    .date
            val weekStart = today.minus(DatePeriod(days = WINDOW_DAYS))

            // Convert LocalDate boundaries to Instant for repository queries
            val fromInstant = weekStart.atStartOfDayIn(tz)
            val toInstant = today.plus(DatePeriod(days = 1)).atStartOfDayIn(tz)
            val priorWeekStart = weekStart.minus(DatePeriod(days = DAYS_PER_WEEK))
            val priorFromInstant = priorWeekStart.atStartOfDayIn(tz)

            // 1. Load trailing 7-day data
            val sessions = usageRepository.getByDateRange(fromInstant, toInstant)
            val checkIns = emotionRepository.getByDateRange(fromInstant, toInstant)
            val intents = intentRepository.getByDateRange(fromInstant, toInstant)
            val priorSessions = usageRepository.getByDateRange(priorFromInstant, fromInstant)
            val budgets = budgetRepository.getRecent(limit = RECENT_BUDGET_LIMIT)

            // 2. Run Tier-2 heuristic analysis (always — used as fallback and for cloud payload)
            val heuristicInsights =
                heuristicEngine.analyzeWeek(
                    sessions = sessions,
                    checkIns = checkIns,
                    intents = intents,
                    priorWeekSessions = priorSessions,
                    timeZone = tz,
                )

            // 3. Compute aggregate stats
            val stats = computeWeekStats(sessions, priorSessions, intents, budgets)

            // 4. Build the narrative (cloud if enabled and available, else Tier-2 template)
            val tier3Narrative =
                takeIf { isCloudAiEnabled() }
                    ?.tryFetchCloudNarrative(weekStart, budgets, heuristicInsights, checkIns, sessions, stats)
                    ?: buildTier2Narrative(heuristicInsights, stats)

            // 5. Build and persist WeeklyInsight
            insightRepository.storeWeeklyInsight(
                buildWeeklyInsight(weekStart, heuristicInsights, tier3Narrative, stats),
            )

            // 6. Post notification
            postWeeklyInsightNotification()
        }

        private fun computeWeekStats(
            sessions: List<dev.bilbo.domain.UsageSession>,
            priorSessions: List<dev.bilbo.domain.UsageSession>,
            intents: List<dev.bilbo.domain.IntentDeclaration>,
            budgets: List<dev.bilbo.domain.DopamineBudget>,
        ): WeekStats {
            val totalScreenTimeMinutes = (sessions.sumOf { it.durationSeconds } / SECONDS_PER_MINUTE).toInt()
            val nutritiveMinutes = minutesInCategory(sessions, dev.bilbo.domain.AppCategory.NUTRITIVE)
            val emptyCalorieMinutes = minutesInCategory(sessions, dev.bilbo.domain.AppCategory.EMPTY_CALORIES)

            val completedIntents = intents.filter { it.actualDurationMinutes != null }
            val accurateIntents = completedIntents.count(::isAccurateIntent)
            val intentAccuracy =
                if (completedIntents.isNotEmpty()) accurateIntents.toFloat() / completedIntents.size else 0f

            val thisWeekTotal = sessions.sumOf { it.durationSeconds }
            val priorWeekTotal = priorSessions.sumOf { it.durationSeconds }
            val weekOverWeekChange =
                if (priorWeekTotal > 0) (thisWeekTotal - priorWeekTotal).toDouble() / priorWeekTotal else null

            return WeekStats(
                totalScreenTimeMinutes = totalScreenTimeMinutes,
                nutritiveMinutes = nutritiveMinutes,
                emptyCalorieMinutes = emptyCalorieMinutes,
                intentAccuracy = intentAccuracy,
                fpEarned = budgets.sumOf { it.fpEarned },
                fpSpent = budgets.sumOf { it.fpSpent },
                streakDays = computeStreak(budgets),
                weekOverWeekChange = weekOverWeekChange,
            )
        }

        private fun minutesInCategory(
            sessions: List<dev.bilbo.domain.UsageSession>,
            category: dev.bilbo.domain.AppCategory,
        ): Int =
            sessions
                .filter { it.category == category }
                .sumOf { it.durationSeconds / SECONDS_PER_MINUTE }
                .toInt()

        private fun isAccurateIntent(intent: dev.bilbo.domain.IntentDeclaration): Boolean {
            val delta =
                kotlin.math
                    .abs((intent.actualDurationMinutes ?: 0) - intent.declaredDurationMinutes)
                    .toDouble() / intent.declaredDurationMinutes.coerceAtLeast(1)
            return delta <= ACCURACY_TOLERANCE
        }

        private fun buildWeeklyInsight(
            weekStart: kotlinx.datetime.LocalDate,
            heuristicInsights: List<HeuristicInsight>,
            narrative: String?,
            stats: WeekStats,
        ): WeeklyInsight =
            WeeklyInsight(
                weekStart = weekStart,
                tier2Insights = heuristicInsights,
                tier3Narrative = narrative,
                totalScreenTimeMinutes = stats.totalScreenTimeMinutes,
                nutritiveMinutes = stats.nutritiveMinutes,
                emptyCalorieMinutes = stats.emptyCalorieMinutes,
                fpEarned = stats.fpEarned,
                fpSpent = stats.fpSpent,
                intentAccuracyPercent = stats.intentAccuracy,
                streakDays = stats.streakDays,
            )

        // ── Cloud AI fetch ────────────────────────────────────────────────────────

        private suspend fun tryFetchCloudNarrative(
            weekStart: kotlinx.datetime.LocalDate,
            budgets: List<dev.bilbo.domain.DopamineBudget>,
            heuristicInsights: List<HeuristicInsight>,
            checkIns: List<dev.bilbo.domain.EmotionalCheckIn>,
            sessions: List<dev.bilbo.domain.UsageSession>,
            stats: WeekStats,
        ): String? {
            if (!cloudInsightClient.canRequest()) return null

            return try {
                val budget = budgets.firstOrNull() ?: return null
                val payload =
                    promptBuilder.buildPayload(
                        weekStart = weekStart,
                        budget = budget,
                        insight = buildWeeklyInsight(weekStart, heuristicInsights, null, stats),
                        checkIns = checkIns,
                        sessions = sessions,
                        weekOverWeekChange = stats.weekOverWeekChange,
                    )

                val anonymousUserId = getAnonymousUserId()
                val result = cloudInsightClient.fetchNarrativeForWeek(payload, anonymousUserId)

                when (result) {
                    is CloudInsightClient.InsightResult.Success -> result.narrative
                    else -> null
                }
            } catch (ignored: Exception) {
                null // Silently fall back to Tier-2
            }
        }

        // ── Tier-2 narrative template ─────────────────────────────────────────────

        private fun buildTier2Narrative(
            heuristicInsights: List<HeuristicInsight>,
            stats: WeekStats,
        ): String {
            val hours = stats.totalScreenTimeMinutes / MINUTES_PER_HOUR
            val mins = stats.totalScreenTimeMinutes % MINUTES_PER_HOUR
            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

            val topInsight =
                heuristicInsights
                    .filter { it.type == InsightType.ACHIEVEMENT || it.type == InsightType.TREND }
                    .maxByOrNull { it.confidence }

            return buildString {
                append("This week you spent $timeStr on your phone.")
                if (stats.streakDays >= MIN_STREAK_FOR_MENTION) {
                    append(" You kept a ${stats.streakDays}-day streak — great consistency!")
                }
                stats.weekOverWeekChange?.let { change ->
                    val pct = (kotlin.math.abs(change) * PERCENT_SCALE).toInt()
                    if (change < IMPROVEMENT_THRESHOLD) {
                        append(" That's $pct% less than last week — nice progress!")
                    } else if (change > REGRESSION_THRESHOLD) {
                        append(" Usage was up $pct% from last week — worth watching.")
                    }
                }
                topInsight?.let { append(" ${it.message}") }
            }
        }

        // ── Helpers ───────────────────────────────────────────────────────────────

        private fun computeStreak(budgets: List<dev.bilbo.domain.DopamineBudget>): Int {
            var streak = 0
            for (budget in budgets.sortedByDescending { it.date }) {
                if (budget.fpEarned > 0) streak++ else break
            }
            return streak
        }

        private fun isCloudAiEnabled(): Boolean {
            val prefs = applicationContext.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
            return prefs.getBoolean("cloud_ai_enabled", false)
        }

        private fun getAnonymousUserId(): String {
            val prefs = applicationContext.getSharedPreferences("bilbo_prefs", Context.MODE_PRIVATE)
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
                val channel =
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID,
                        "Bilbo Insights",
                        NotificationManager.IMPORTANCE_DEFAULT,
                    ).apply {
                        description = "Weekly wellness summary from Bilbo"
                    }
                notificationManager.createNotificationChannel(channel)
            }

            // Intent to open the insights screen
            val intent =
                applicationContext.packageManager
                    .getLaunchIntentForPackage(applicationContext.packageName)
                    ?.apply { putExtra("open_screen", "weekly_insight") }
            val pendingIntent =
                PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val notification =
                NotificationCompat
                    .Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
                    .setSmallIcon(dev.bilbo.app.R.drawable.ic_notification)
                    .setContentTitle("Your weekly insight is ready ✨")
                    .setContentText("See how your screen time looked this week.")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }
