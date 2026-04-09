package dev.bilbo.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import timber.log.Timber

/**
 * System-level [BroadcastReceiver] for lock / unlock events.
 *
 * Listens for:
 * - [Intent.ACTION_USER_PRESENT] — device fully unlocked (past biometrics/PIN).
 * - [Intent.ACTION_SCREEN_OFF]   — screen turned off / device locked.
 *
 * On unlock, (re)starts [UsageTrackingService] so tracking resumes immediately.
 * On screen-off, forwards the event to the running service so it can close the
 * active [dev.bilbo.tracking.SessionTracker] session without a full restart.
 *
 * The Intent Gatekeeper also observes unlock events (via [UsageTrackingService])
 * to decide whether to surface the gatekeeper overlay on the next foreground-app
 * transition.
 *
 * This receiver is registered in AndroidManifest.xml and fires even when the
 * app process is not running — that's how tracking auto-restarts after a reboot
 * or after the process is killed.
 */
class UnlockReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> {
                Timber.d("UnlockReceiver: device unlocked — ensuring UsageTrackingService is running")
                startTrackingService(context)
            }
            Intent.ACTION_SCREEN_OFF -> {
                Timber.d("UnlockReceiver: screen off — forwarding to UsageTrackingService")
                // The service handles this via its own internal BroadcastReceiver;
                // here we forward as an explicit intent in case the service is alive.
                forwardToService(context, UsageTrackingService.ACTION_SCREEN_OFF)
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun startTrackingService(context: Context) {
        val serviceIntent = Intent(context, UsageTrackingService::class.java).apply {
            action = UsageTrackingService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Timber.e(e, "UnlockReceiver: failed to start UsageTrackingService")
        }
    }

    private fun forwardToService(context: Context, action: String) {
        val serviceIntent = Intent(context, UsageTrackingService::class.java).apply {
            this.action = action
        }
        try {
            context.startService(serviceIntent)
        } catch (_: Exception) {
            // Service may not be running yet — that's fine
        }
    }
}
