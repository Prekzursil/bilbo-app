package dev.spark.app.emotional

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.spark.app.overlay.OverlayManager
import dev.spark.app.ui.overlay.AIInterventionCard
import dev.spark.app.ui.overlay.CoolingOffScreen
import dev.spark.app.ui.overlay.EmotionalCheckInScreen
import dev.spark.app.ui.overlay.PostSessionMoodScreen
import dev.spark.data.AppProfileRepository
import dev.spark.data.BudgetRepository
import dev.spark.data.EmotionRepository
import dev.spark.data.IntentRepository
import dev.spark.domain.AppCategory
import dev.spark.domain.Emotion
import dev.spark.domain.EmotionalCheckIn
import dev.spark.domain.FPEconomy
import dev.spark.economy.FocusPointsEngine
import dev.spark.tracking.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates the full emotional intelligence flow for a session.
 *
 * ### Flow sequence
 * ```
 * [Gatekeeper "Start"]
 *     ↓
 * [EmotionalCheckInScreen]   (if emotional check-in feature enabled)
 *     ↓
 * [AIInterventionCard]       (if negative emotion + Empty Calorie app)
 *     ├── "Yes, breathe"  → [CoolingOffScreen (10s)] → +3 FP → app opens
 *     └── "Continue"      → cooling-off if configured → app opens
 *     ↓
 * [App opens]
 *     ↓
 * [Enforcement fires]
 *     ↓
 * [PostSessionMoodScreen]    (optional, shown over enforcement overlay)
 * ```
 *
 * ### Configuration flags
 * Feature flags are stored in [EmotionalFlowSettings] and can be toggled from
 * Settings.  Defaults: all enabled.
 */
@Singleton
class EmotionalFlowController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: OverlayManager,
    private val emotionRepository: EmotionRepository,
    private val appProfileRepository: AppProfileRepository,
    private val budgetRepository: BudgetRepository,
    private val intentRepository: IntentRepository,
    private val focusPointsEngine: FocusPointsEngine,
    private val settings: EmotionalFlowSettings,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Emits app-open events so callers can react when the flow completes and
     * the app should actually be opened.
     */
    private val _openAppSignal = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openAppSignal: SharedFlow<String> = _openAppSignal

    // ── Main entry points ─────────────────────────────────────────────────────

    /**
     * Called by [GatekeeperController] after the user taps "Start".
     *
     * @param appInfo        The app about to be opened.
     * @param declarationId  The newly created [IntentDeclaration] ID.
     * @param onAppOpen      Called when the flow completes and the app should open.
     */
    fun startFlow(appInfo: AppInfo, declarationId: Long, onAppOpen: () -> Unit) {
        if (!settings.isEmotionalCheckInEnabled) {
            // No emotional flow — open app immediately
            onAppOpen()
            return
        }

        showCheckIn(appInfo, declarationId, onAppOpen)
    }

    /**
     * Called by [EnforcementController] when enforcement fires.
     * Optionally shows [PostSessionMoodScreen] over the enforcement overlay.
     *
     * @param checkInId The emotional check-in linked to this session, or null.
     */
    fun triggerPostSessionMood(checkInId: Long?, onComplete: () -> Unit) {
        if (!settings.isPostSessionMoodEnabled || checkInId == null) {
            onComplete()
            return
        }

        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(
                appInfo = AppInfo(packageName = "post_session_mood", appLabel = "", category = null),
            ) { _, onDismiss ->
                PostSessionMoodScreen(
                    checkInId = checkInId,
                    onMoodSelected = { emotion ->
                        scope.launch { persistPostMood(checkInId, emotion) }
                        onDismiss()
                        onComplete()
                    },
                    onSkip = {
                        onDismiss()
                        onComplete()
                    },
                )
            }
        }
    }

    // ── Step 1: Emotional check-in ────────────────────────────────────────────

    private fun showCheckIn(appInfo: AppInfo, declarationId: Long, onAppOpen: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(appInfo) { info, onDismiss ->
                EmotionalCheckInScreen(
                    onEmotionSelected = { emotion ->
                        onDismiss()
                        scope.launch {
                            val checkInId = persistCheckIn(emotion, declarationId)
                            handleEmotionSelected(info, emotion, checkInId, onAppOpen)
                        }
                    },
                    onSkip = {
                        onDismiss()
                        onAppOpen()
                    },
                )
            }
        }
    }

    // ── Step 2: Check if intervention needed ──────────────────────────────────

    private suspend fun handleEmotionSelected(
        appInfo: AppInfo,
        emotion: Emotion,
        checkInId: Long,
        onAppOpen: () -> Unit,
    ) {
        val isNegativeEmotion = isNegativeEmotion(emotion)

        // Get app profile to determine if it's Empty Calorie
        val profile = appProfileRepository.getByPackageName(appInfo.packageName)
        val isEmptyCalorie = profile?.category == AppCategory.EMPTY_CALORIES

        if (isNegativeEmotion && isEmptyCalorie && settings.isAIInterventionEnabled) {
            // Show the AI intervention card with heuristic-backed data
            val pattern = getEmotionPattern(appInfo.packageName, emotion)
            showIntervention(appInfo, emotion, pattern, onAppOpen)
        } else if (profile?.coolingOffEnabled == true && settings.isCoolingOffEnabled) {
            // Cooling-off configured for this app regardless of emotion
            showCoolingOff(appInfo, onAppOpen)
        } else {
            onAppOpen()
        }
    }

    // ── Step 3: AI intervention card ─────────────────────────────────────────

    private fun showIntervention(
        appInfo: AppInfo,
        emotion: Emotion,
        pattern: EmotionPattern?,
        onAppOpen: () -> Unit,
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(appInfo) { info, onDismiss ->
                AIInterventionCard(
                    emotion = emotion,
                    appName = info.appLabel,
                    avgDurationMins = pattern?.avgDurationMins ?: 20,
                    postMood = pattern?.typicalPostMood,
                    onBreathe = {
                        onDismiss()
                        showCoolingOff(info, onAppOpen)
                    },
                    onContinue = {
                        onDismiss()
                        scope.launch {
                            val profile = appProfileRepository.getByPackageName(info.packageName)
                            if (profile?.coolingOffEnabled == true && settings.isCoolingOffEnabled) {
                                showCoolingOff(info, onAppOpen)
                            } else {
                                onAppOpen()
                            }
                        }
                    },
                )
            }
        }
    }

    // ── Step 4: Cooling-off breathing screen ──────────────────────────────────

    private fun showCoolingOff(appInfo: AppInfo, onAppOpen: () -> Unit) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(appInfo) { _, onDismiss ->
                CoolingOffScreen(
                    onComplete = {
                        // Award +3 FP breathing bonus
                        scope.launch { awardBreathingBonus() }
                        onDismiss()
                        onAppOpen()
                    },
                )
            }
        }
    }

    // ── Persistence helpers ───────────────────────────────────────────────────

    private suspend fun persistCheckIn(emotion: Emotion, declarationId: Long): Long {
        return try {
            emotionRepository.insert(
                EmotionalCheckIn(
                    timestamp = Clock.System.now(),
                    preSessionEmotion = emotion,
                    linkedIntentId = declarationId,
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "EmotionalFlowController: failed to persist check-in")
            -1L
        }
    }

    private suspend fun persistPostMood(checkInId: Long, emotion: Emotion) {
        try {
            emotionRepository.updatePostMood(checkInId, emotion)
            Timber.d("EmotionalFlowController: post-session mood saved — $emotion for checkIn $checkInId")
        } catch (e: Exception) {
            Timber.e(e, "EmotionalFlowController: failed to update post-session mood")
        }
    }

    private suspend fun awardBreathingBonus() {
        try {
            val now = kotlinx.datetime.Clock.System.now()
            val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(tz).date
            budgetRepository.incrementFpBonus(today, FPEconomy.BONUS_BREATHING_EXERCISE)
            Timber.d("EmotionalFlowController: awarded ${FPEconomy.BONUS_BREATHING_EXERCISE} FP breathing bonus")
        } catch (e: Exception) {
            Timber.e(e, "EmotionalFlowController: failed to award breathing bonus")
        }
    }

    // ── Heuristic data ────────────────────────────────────────────────────────

    private suspend fun getEmotionPattern(packageName: String, emotion: Emotion): EmotionPattern? {
        return try {
            val checkIns = emotionRepository.getAll()
            val appIntents = intentRepository.getByApp(packageName)

            if (checkIns.isEmpty() || appIntents.isEmpty()) return null

            // Filter check-ins matching this emotion linked to this app's intents
            val appIntentIds = appIntents.map { it.id }.toSet()
            val matchingCheckIns = checkIns.filter { ci ->
                ci.preSessionEmotion == emotion &&
                ci.linkedIntentId != null &&
                ci.linkedIntentId in appIntentIds
            }

            if (matchingCheckIns.size < 3) return null

            // Average duration for this emotion + app combo
            val durations = matchingCheckIns.mapNotNull { ci ->
                appIntents.firstOrNull { it.id == ci.linkedIntentId }?.declaredDurationMinutes
            }
            val avgDuration = if (durations.isEmpty()) 20 else durations.average().toInt()

            // Most common post-session mood
            val postMoods = matchingCheckIns.mapNotNull { it.postSessionMood }
            val typicalPostMood = postMoods
                .groupBy { it }
                .maxByOrNull { it.value.size }
                ?.key

            EmotionPattern(
                emotion = emotion,
                avgDurationMins = avgDuration,
                typicalPostMood = typicalPostMood,
            )
        } catch (e: Exception) {
            Timber.e(e, "EmotionalFlowController: error computing emotion pattern")
            null
        }
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private fun isNegativeEmotion(emotion: Emotion): Boolean = when (emotion) {
        Emotion.BORED, Emotion.STRESSED, Emotion.ANXIOUS, Emotion.SAD, Emotion.LONELY -> true
        Emotion.HAPPY, Emotion.CALM -> false
    }
}

// ── Data structures ───────────────────────────────────────────────────────────

/** Aggregated heuristic data for a (packageName, emotion) pair. */
data class EmotionPattern(
    val emotion: Emotion,
    val avgDurationMins: Int,
    val typicalPostMood: Emotion?,
)

/** Feature flag container for the emotional intelligence flow. */
data class EmotionalFlowSettings(
    val isEmotionalCheckInEnabled: Boolean = true,
    val isAIInterventionEnabled: Boolean = true,
    val isCoolingOffEnabled: Boolean = true,
    val isPostSessionMoodEnabled: Boolean = true,
)


