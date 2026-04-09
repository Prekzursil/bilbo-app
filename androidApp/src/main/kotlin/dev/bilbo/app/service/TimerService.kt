package dev.bilbo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import dagger.hilt.android.AndroidEntryPoint
import dev.bilbo.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that manages active intent-session countdowns.
 *
 * ### How it works
 * 1. [GatekeeperController] broadcasts [ACTION_START_TIMER] after an intent declaration
 *    is created.  This service receives the broadcast and starts a coroutine-based
 *    countdown for that declaration.
 * 2. Multiple timers can run concurrently — one per (declarationId, appPackage) pair.
 * 3. Every second the timer ticks, [ACTION_TIMER_TICK] is broadcast via
 *    [LocalBroadcastManager] so UI layers can update countdowns.
 * 4. At T-2 minutes a heads-up notification is posted.
 * 5. When a timer expires, [ACTION_TIMER_EXPIRED] is broadcast so
 *    [EnforcementController] can react.
 * 6. State is persisted to [SharedPreferences] so timers survive process death —
 *    on [onCreate], any previously persisted timers are resumed.
 */
@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    companion object {
        // ── Actions ──────────────────────────────────────────────────────────
        const val ACTION_START_TIMER     = "dev.bilbo.app.action.START_TIMER"
        const val ACTION_STOP_TIMER      = "dev.bilbo.app.action.STOP_TIMER"
        const val ACTION_TIMER_TICK      = "dev.bilbo.app.action.TIMER_TICK"
        const val ACTION_TIMER_EXPIRED   = "dev.bilbo.app.action.TIMER_EXPIRED"
        const val ACTION_TIMER_WARNING   = "dev.bilbo.app.action.TIMER_WARNING"

        // ── Extras ───────────────────────────────────────────────────────────
        const val EXTRA_DECLARATION_ID   = "extra_declaration_id"
        const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
        const val EXTRA_APP_PACKAGE      = "extra_app_package"
        const val EXTRA_APP_LABEL        = "extra_app_label"
        const val EXTRA_REMAINING_SECS   = "extra_remaining_secs"

        // ── Notification ──────────────────────────────────────────────────────
        const val CHANNEL_TIMER          = "bilbo_timer"
        const val CHANNEL_WARNING        = "bilbo_timer_warning"
        private const val NOTIF_FOREGROUND_ID = 1001

        // ── Prefs ─────────────────────────────────────────────────────────────
        private const val PREFS_NAME     = "bilbo_timer_state"
        private const val PREFS_ACTIVE_IDS = "active_declaration_ids"

        private fun prefsKeyEnd(declarationId: Long) = "timer_end_$declarationId"
        private fun prefsKeyPkg(declarationId: Long) = "timer_pkg_$declarationId"
        private fun prefsKeyLabel(declarationId: Long) = "timer_label_$declarationId"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Map of declarationId → running countdown Job */
    private val activeTimers = mutableMapOf<Long, Job>()

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(NOTIF_FOREGROUND_ID, buildForegroundNotification())
        resumePersistedTimers()
        Timber.d("TimerService: created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_TIMER -> {
                val declarationId = intent.getLongExtra(EXTRA_DECLARATION_ID, -1L)
                val durationMins  = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                val appPackage    = intent.getStringExtra(EXTRA_APP_PACKAGE) ?: ""
                val appLabel      = intent.getStringExtra(EXTRA_APP_LABEL) ?: appPackage

                if (declarationId != -1L && durationMins > 0) {
                    startTimer(declarationId, durationMins * 60L, appPackage, appLabel)
                }
            }
            ACTION_STOP_TIMER -> {
                val declarationId = intent.getLongExtra(EXTRA_DECLARATION_ID, -1L)
                if (declarationId != -1L) stopTimer(declarationId)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        Timber.d("TimerService: destroyed, ${activeTimers.size} timers cancelled")
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // ── Timer management ──────────────────────────────────────────────────────

    private fun startTimer(
        declarationId: Long,
        durationSeconds: Long,
        appPackage: String,
        appLabel: String,
    ) {
        // Cancel any existing timer for this declaration
        activeTimers[declarationId]?.cancel()

        val endEpochSecs = System.currentTimeMillis() / 1000L + durationSeconds
        persistTimerState(declarationId, endEpochSecs, appPackage, appLabel)

        Timber.d("TimerService: starting timer for declaration $declarationId ($durationSeconds s) — $appLabel")

        val job = serviceScope.launch {
            var remainingSecs = durationSeconds
            var warningFired  = false

            while (isActive && remainingSecs > 0) {
                broadcastTick(declarationId, remainingSecs, appPackage)

                // 2-minute warning
                if (!warningFired && remainingSecs <= 120) {
                    warningFired = true
                    postWarningNotification(declarationId, appLabel, remainingSecs)
                    broadcastWarning(declarationId, remainingSecs, appPackage)
                }

                delay(1_000L)
                remainingSecs--
            }

            if (isActive) {
                // Timer expired normally
                Timber.d("TimerService: timer expired for declaration $declarationId — $appLabel")
                clearPersistedTimer(declarationId)
                broadcastExpired(declarationId, appPackage, appLabel)
            }
        }

        activeTimers[declarationId] = job
    }

    private fun stopTimer(declarationId: Long) {
        activeTimers.remove(declarationId)?.cancel()
        clearPersistedTimer(declarationId)
        Timber.d("TimerService: timer stopped for declaration $declarationId")

        if (activeTimers.isEmpty()) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    // ── Broadcasts ────────────────────────────────────────────────────────────

    private fun broadcastTick(declarationId: Long, remainingSecs: Long, appPackage: String) {
        val intent = Intent(ACTION_TIMER_TICK).apply {
            putExtra(EXTRA_DECLARATION_ID, declarationId)
            putExtra(EXTRA_REMAINING_SECS, remainingSecs)
            putExtra(EXTRA_APP_PACKAGE, appPackage)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastWarning(declarationId: Long, remainingSecs: Long, appPackage: String) {
        val intent = Intent(ACTION_TIMER_WARNING).apply {
            putExtra(EXTRA_DECLARATION_ID, declarationId)
            putExtra(EXTRA_REMAINING_SECS, remainingSecs)
            putExtra(EXTRA_APP_PACKAGE, appPackage)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun broadcastExpired(declarationId: Long, appPackage: String, appLabel: String) {
        val intent = Intent(ACTION_TIMER_EXPIRED).apply {
            `package` = packageName
            putExtra(EXTRA_DECLARATION_ID, declarationId)
            putExtra(EXTRA_APP_PACKAGE, appPackage)
            putExtra(EXTRA_APP_LABEL, appLabel)
        }
        // Send both local (UI) and global (EnforcementController via system)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        sendBroadcast(intent)
        activeTimers.remove(declarationId)
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private fun persistTimerState(
        declarationId: Long,
        endEpochSecs: Long,
        appPackage: String,
        appLabel: String,
    ) {
        val currentIds = prefs.getStringSet(PREFS_ACTIVE_IDS, emptySet())!!.toMutableSet()
        currentIds.add(declarationId.toString())
        prefs.edit()
            .putStringSet(PREFS_ACTIVE_IDS, currentIds)
            .putLong(prefsKeyEnd(declarationId), endEpochSecs)
            .putString(prefsKeyPkg(declarationId), appPackage)
            .putString(prefsKeyLabel(declarationId), appLabel)
            .apply()
    }

    private fun clearPersistedTimer(declarationId: Long) {
        val currentIds = prefs.getStringSet(PREFS_ACTIVE_IDS, emptySet())!!.toMutableSet()
        currentIds.remove(declarationId.toString())
        prefs.edit()
            .putStringSet(PREFS_ACTIVE_IDS, currentIds)
            .remove(prefsKeyEnd(declarationId))
            .remove(prefsKeyPkg(declarationId))
            .remove(prefsKeyLabel(declarationId))
            .apply()
    }

    /**
     * On service restart after process death, resume any timers that had
     * remaining time.  Timers that already expired fire immediately as expired.
     */
    private fun resumePersistedTimers() {
        val ids = prefs.getStringSet(PREFS_ACTIVE_IDS, emptySet()) ?: return
        val nowSecs = System.currentTimeMillis() / 1000L

        ids.forEach { idStr ->
            val declarationId = idStr.toLongOrNull() ?: return@forEach
            val endEpoch = prefs.getLong(prefsKeyEnd(declarationId), 0L)
            val appPackage = prefs.getString(prefsKeyPkg(declarationId), "") ?: ""
            val appLabel   = prefs.getString(prefsKeyLabel(declarationId), appPackage) ?: appPackage

            val remaining = endEpoch - nowSecs
            if (remaining > 0) {
                Timber.d("TimerService: resuming timer for declaration $declarationId ($remaining s remaining)")
                startTimer(declarationId, remaining, appPackage, appLabel)
            } else {
                // Expired while the service was dead — fire expired now
                Timber.d("TimerService: declaration $declarationId expired while dead — firing expired")
                clearPersistedTimer(declarationId)
                broadcastExpired(declarationId, appPackage, appLabel)
            }
        }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_TIMER,
                "Session Timer",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Shows active app session timers" }

            val warningChannel = NotificationChannel(
                CHANNEL_WARNING,
                "Session Warning",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Alerts when your session time is almost up"
                enableVibration(true)
            }

            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(warningChannel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_TIMER)
            .setContentTitle("Bilbo is tracking your session")
            .setContentText("Timer active — stay intentional.")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun postWarningNotification(declarationId: Long, appLabel: String, remainingSecs: Long) {
        val mins = (remainingSecs / 60).toInt()
        val secs = (remainingSecs % 60).toInt()
        val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"

        val notification = NotificationCompat.Builder(this, CHANNEL_WARNING)
            .setContentTitle("⏱ Almost time!")
            .setContentText("Your $appLabel session ends in $timeStr. Wrap up.")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify((NOTIF_FOREGROUND_ID + declarationId).toInt(), notification)
    }
}
