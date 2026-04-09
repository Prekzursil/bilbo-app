package dev.spark.app.enforcement

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.spark.app.overlay.OverlayManager
import dev.spark.app.service.TimerService
import dev.spark.app.ui.overlay.HardLockOverlayScreen
import dev.spark.app.ui.overlay.NudgeOverlayScreen
import dev.spark.data.AppProfileRepository
import dev.spark.data.BudgetRepository
import dev.spark.data.IntentRepository
import dev.spark.data.SuggestionRepository
import dev.spark.domain.EnforcementMode
import dev.spark.domain.FPEconomy
import dev.spark.economy.FocusPointsEngine
import dev.spark.enforcement.CooldownManager
import dev.spark.tracking.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for timer-expiry broadcasts from [TimerService] and orchestrates
 * the appropriate enforcement response: a dismissible nudge or a full hard lock.
 *
 * ### Cooldown re-interception
 * After a [EnforcementMode.HARD_LOCK] fires, [CooldownManager] tracks the locked
 * app.  If the user attempts to reopen the app during cooldown, the [AppMonitor]
 * callback (via [GatekeeperController]) calls [checkAndEnforceCooldown] which
 * shows the hard-lock overlay again without requiring a new timer expiry.
 *
 * ### Focus Points
 * - Nudge extension: −5 FP
 * - Hard lock override: −10 FP (matches [FPEconomy.PENALTY_HARD_LOCK_OVERRIDE])
 */
@Singleton
class EnforcementController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: OverlayManager,
    private val appProfileRepository: AppProfileRepository,
    private val intentRepository: IntentRepository,
    private val budgetRepository: BudgetRepository,
    private val suggestionRepository: SuggestionRepository,
    private val cooldownManager: CooldownManager,
    private val focusPointsEngine: FocusPointsEngine,
) {

    companion object {
        private const val HARD_LOCK_COOLDOWN_MINUTES = 30
        private const val NUDGE_EXTENSION_MINUTES    = 5
        private const val NUDGE_EXTENSION_FP_COST    = 5
        private const val HARD_LOCK_OVERRIDE_FP_COST = 10
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val timerExpiredReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action != TimerService.ACTION_TIMER_EXPIRED) return

            val declarationId = intent.getLongExtra(TimerService.EXTRA_DECLARATION_ID, -1L)
            val appPackage    = intent.getStringExtra(TimerService.EXTRA_APP_PACKAGE) ?: return
            val appLabel      = intent.getStringExtra(TimerService.EXTRA_APP_LABEL) ?: appPackage

            Timber.d("EnforcementController: timer expired — declarationId=$declarationId, app=$appLabel")
            handleTimerExpired(declarationId, appPackage, appLabel)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Register the broadcast receiver.  Call once from the owning service's
     * [Service.onCreate] or DI module.
     */
    fun register() {
        val filter = IntentFilter(TimerService.ACTION_TIMER_EXPIRED)
        LocalBroadcastManager.getInstance(context).registerReceiver(timerExpiredReceiver, filter)
        // Also register for system-wide broadcasts (service may be in different process)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(timerExpiredReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(timerExpiredReceiver, filter)
        }
        cooldownManager.restoreFromPersistence()
        Timber.d("EnforcementController: registered")
    }

    /** Unregister the broadcast receiver.  Call from the owning service's [Service.onDestroy]. */
    fun unregister() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(timerExpiredReceiver)
        try { context.unregisterReceiver(timerExpiredReceiver) } catch (e: Exception) { /* already unregistered */ }
        Timber.d("EnforcementController: unregistered")
    }

    // ── Public enforcement API ────────────────────────────────────────────────

    /**
     * Called by [GatekeeperController] for every foreground-app change.
     * If [packageName] is in an active cooldown, the hard-lock overlay is shown
     * immediately without waiting for a timer expiry.
     *
     * @return true if enforcement was applied (cooldown active), false otherwise.
     */
    fun checkAndEnforceCooldown(packageName: String, appLabel: String): Boolean {
        if (!cooldownManager.isLocked(packageName)) return false

        Timber.d("EnforcementController: re-enforcing cooldown for $packageName")
        val remainingSecs = cooldownManager.getRemainingSeconds(packageName) ?: 0L
        showHardLockOverlay(packageName, appLabel, remainingSecs)
        return true
    }

    // ── Internal handlers ─────────────────────────────────────────────────────

    private fun handleTimerExpired(declarationId: Long, appPackage: String, appLabel: String) {
        scope.launch {
            try {
                val profile = appProfileRepository.getByPackageName(appPackage)
                val enforcementMode = profile?.enforcementMode ?: EnforcementMode.NUDGE

                // Mark declaration as enforced with the correct mode
                markDeclarationEnforced(declarationId, enforcementMode)

                val today = Clock.System.now()
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                val budget = budgetRepository.getByDate(today)
                val fpBalance = budget?.let { focusPointsEngine.getBalance(it) } ?: 0

                // Compute actual session duration
                val actualMinutes = computeActualMinutes(declarationId)

                Timber.d("EnforcementController: enforcing $enforcementMode for $appLabel (balance=$fpBalance FP)")

                when (enforcementMode) {
                    EnforcementMode.NUDGE -> showNudgeOnMainThread(
                        appPackage    = appPackage,
                        appLabel      = appLabel,
                        declaredMins  = getDeclarationDuration(declarationId),
                        actualMinutes = actualMinutes,
                        fpBalance     = fpBalance,
                    )
                    EnforcementMode.HARD_LOCK -> {
                        cooldownManager.lockApp(appPackage, HARD_LOCK_COOLDOWN_MINUTES)
                        val remainingSecs = cooldownManager.getRemainingSeconds(appPackage) ?: (HARD_LOCK_COOLDOWN_MINUTES * 60L)
                        val suggestion = getSuggestion()
                        showHardLockOnMainThread(appPackage, appLabel, remainingSecs, suggestion, fpBalance)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "EnforcementController: error handling timer expiry for $appLabel")
            }
        }
    }

    private fun showNudgeOnMainThread(
        appPackage: String,
        appLabel: String,
        declaredMins: Int,
        actualMinutes: Int,
        fpBalance: Int,
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(
                appInfo = AppInfo(packageName = appPackage, appLabel = appLabel, category = null),
            ) { _, onDismiss ->
                NudgeOverlayScreen(
                    appName         = appLabel,
                    declaredMinutes = declaredMins,
                    actualMinutes   = actualMinutes,
                    fpBalance       = fpBalance,
                    onGotIt         = {
                        logNudgeAcknowledged(appPackage)
                        onDismiss()
                    },
                    onExtend5Min    = {
                        scope.launch { handleNudgeExtension(appPackage, appLabel) }
                        onDismiss()
                    },
                )
            }
        }
    }

    private fun showHardLockOnMainThread(
        appPackage: String,
        appLabel: String,
        remainingSecs: Long,
        suggestion: String,
        fpBalance: Int,
    ) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            showHardLockOverlay(appPackage, appLabel, remainingSecs, suggestion, fpBalance)
        }
    }

    private fun showHardLockOverlay(
        appPackage: String,
        appLabel: String,
        remainingSecs: Long,
        suggestion: String = "Take a short walk outside 🚶",
        fpBalance: Int = 0,
    ) {
        overlayManager.showGatekeeper(
            appInfo = AppInfo(packageName = appPackage, appLabel = appLabel, category = null),
        ) { _, onDismiss ->
            HardLockOverlayScreen(
                appName          = appLabel,
                cooldownMinutes  = HARD_LOCK_COOLDOWN_MINUTES,
                remainingSeconds = remainingSecs,
                suggestion       = suggestion,
                fpBalance        = fpBalance,
                onGoHome         = {
                    navigateHome()
                    onDismiss()
                },
                onOverride       = {
                    scope.launch { handleHardLockOverride(appPackage) }
                    onDismiss()
                },
            )
        }
    }

    // ── FP Economy actions ────────────────────────────────────────────────────

    private suspend fun handleNudgeExtension(appPackage: String, appLabel: String) {
        try {
            val today = todayDate()
            val budget = budgetRepository.getByDate(today) ?: return
            val balance = focusPointsEngine.getBalance(budget)
            if (balance < NUDGE_EXTENSION_FP_COST) return

            budgetRepository.incrementFpSpent(today, NUDGE_EXTENSION_FP_COST)
            Timber.d("EnforcementController: nudge extension granted for $appLabel (−${NUDGE_EXTENSION_FP_COST} FP)")
        } catch (e: Exception) {
            Timber.e(e, "EnforcementController: error handling nudge extension")
        }
    }

    private suspend fun handleHardLockOverride(appPackage: String) {
        try {
            val today = todayDate()
            budgetRepository.incrementFpSpent(today, HARD_LOCK_OVERRIDE_FP_COST)
            cooldownManager.unlockApp(appPackage)
            Timber.d("EnforcementController: hard lock override for $appPackage (−${HARD_LOCK_OVERRIDE_FP_COST} FP)")
        } catch (e: Exception) {
            Timber.e(e, "EnforcementController: error handling hard lock override")
        }
    }

    private fun todayDate(): kotlinx.datetime.LocalDate {
        return Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun navigateHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private suspend fun markDeclarationEnforced(declarationId: Long, mode: EnforcementMode = EnforcementMode.NUDGE) {
        try {
            val actualMinutes = computeActualMinutes(declarationId)
            intentRepository.updateActualDuration(declarationId, actualMinutes)
            intentRepository.updateEnforcement(
                id             = declarationId,
                wasEnforced    = true,
                enforcementType = mode,
                wasOverridden  = false,
            )
        } catch (e: Exception) {
            Timber.e(e, "EnforcementController: could not mark declaration $declarationId as enforced")
        }
    }

    private suspend fun computeActualMinutes(declarationId: Long): Int {
        return try {
            val declaration = intentRepository.getById(declarationId) ?: return 0
            val nowSecs = Clock.System.now().epochSeconds
            val startSecs = declaration.timestamp.epochSeconds
            ((nowSecs - startSecs) / 60).toInt()
        } catch (e: Exception) { 0 }
    }

    private suspend fun getDeclarationDuration(declarationId: Long): Int {
        return try {
            intentRepository.getById(declarationId)?.declaredDurationMinutes ?: 0
        } catch (e: Exception) { 0 }
    }

    private suspend fun getSuggestion(): String {
        return try {
            suggestionRepository.getAll().randomOrNull()?.text ?: "Take a short walk outside 🚶"
        } catch (e: Exception) {
            "Take a short walk outside 🚶"
        }
    }

    private fun logNudgeAcknowledged(appPackage: String) {
        Timber.i("EnforcementController: nudge acknowledged for $appPackage")
    }
}
