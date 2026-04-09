package dev.bilbo.app.overlay

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.bilbo.app.ui.overlay.GatekeeperScreen
import dev.bilbo.data.IntentRepository
import dev.bilbo.tracking.AppInfo
import dev.bilbo.tracking.AppMonitor
import dev.bilbo.tracking.BypassManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestrates when to show or hide the Intent Gatekeeper overlay.
 *
 * ### Decision flow (per foreground-app change)
 * 1. Is [packageName] in [BypassManager]?  → skip.
 * 2. Is there an active (non-expired) [IntentDeclaration] for this app?  → skip.
 * 3. Otherwise → call [OverlayManager.showGatekeeper].
 *
 * [GatekeeperController] listens to [AppMonitor] directly and is wired into
 * [UsageTrackingService] by calling [attach] after the service is created.
 */
@Singleton
class GatekeeperController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val overlayManager: OverlayManager,
    private val bypassManager: BypassManager,
    private val intentRepository: IntentRepository,
) {

    companion object {
        /** Intent action broadcast when a timer should start for a declaration. */
        const val ACTION_START_TIMER = "dev.bilbo.app.action.START_TIMER"
        const val EXTRA_DECLARATION_ID = "extra_declaration_id"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Connect this controller to [appMonitor] so foreground-app transitions
     * trigger gatekeeper logic.  Call once from [UsageTrackingService].
     */
    fun attach(appMonitor: AppMonitor) {
        appMonitor.onAppChanged { appInfo ->
            handleForegroundChange(appInfo)
        }
        Timber.d("GatekeeperController: attached to AppMonitor")
    }

    /** Programmatically dismiss the overlay (e.g. when service is destroyed). */
    fun dismiss() {
        overlayManager.dismiss()
    }

    // ── Decision logic ────────────────────────────────────────────────────────

    private fun handleForegroundChange(appInfo: AppInfo) {
        // 1. Bypass list check — synchronous
        if (bypassManager.shouldBypass(appInfo.packageName)) {
            Timber.d("GatekeeperController: bypassing ${appInfo.packageName}")
            overlayManager.dismiss()
            return
        }

        // 2. Active declaration check — async
        scope.launch {
            val hasActiveDeclaration = checkActiveDeclaration(appInfo.packageName)
            if (hasActiveDeclaration) {
                Timber.d("GatekeeperController: active declaration exists for ${appInfo.packageName} — skipping gatekeeper")
                return@launch
            }

            // 3. Show gatekeeper
            Timber.d("GatekeeperController: showing gatekeeper for ${appInfo.packageName}")
            showGatekeeperFor(appInfo)
        }
    }

    private fun showGatekeeperFor(appInfo: AppInfo) {
        // OverlayManager.showGatekeeper must be called on the main thread
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            overlayManager.showGatekeeper(appInfo) { info, onDismiss ->
                GatekeeperScreen(
                    appInfo = info,
                    onStart = { intention, durationMinutes ->
                        handleStart(info, intention, durationMinutes)
                        onDismiss()
                    },
                    onDismiss = {
                        Timber.d("GatekeeperController: user dismissed gatekeeper for ${info.packageName}")
                        onDismiss()
                    },
                )
            }
        }
    }

    private fun handleStart(appInfo: AppInfo, intention: String, durationMinutes: Int) {
        scope.launch {
            try {
                val declaration = dev.bilbo.domain.IntentDeclaration(
                    timestamp = Clock.System.now(),
                    declaredApp = appInfo.packageName,
                    declaredDurationMinutes = durationMinutes,
                )
                val declarationId = intentRepository.insert(declaration)
                Timber.d(
                    "GatekeeperController: created declaration $declarationId for ${appInfo.packageName} " +
                    "($durationMinutes min, intention='$intention')"
                )
                // Broadcast to TimerService
                broadcastTimerStart(declarationId, durationMinutes)
            } catch (e: Exception) {
                Timber.e(e, "GatekeeperController: failed to save declaration")
            }
        }
    }

    private fun broadcastTimerStart(declarationId: Long, durationMinutes: Int) {
        val intent = Intent(ACTION_START_TIMER).apply {
            `package` = context.packageName
            putExtra(EXTRA_DECLARATION_ID, declarationId)
            putExtra(EXTRA_DURATION_MINUTES, durationMinutes)
        }
        context.sendBroadcast(intent)
    }

    // ── DB helpers ────────────────────────────────────────────────────────────

    private suspend fun checkActiveDeclaration(packageName: String): Boolean {
        return try {
            val declarations = intentRepository.getByApp(packageName)
            val now = Clock.System.now()
            declarations.any { declaration ->
                val endEpoch = declaration.timestamp.epochSeconds +
                        (declaration.declaredDurationMinutes * 60L)
                now.epochSeconds < endEpoch
            }
        } catch (e: Exception) {
            Timber.e(e, "GatekeeperController: error checking active declarations")
            false
        }
    }
}
