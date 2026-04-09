package dev.bilbo.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.bilbo.app.R
import dev.bilbo.data.AppProfileRepository
import dev.bilbo.data.UsageRepository
import dev.bilbo.app.overlay.GatekeeperController
import dev.bilbo.tracking.AppMonitor
import dev.bilbo.tracking.SessionTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import timber.log.Timber
import javax.inject.Inject

/**
 * Long-running foreground service that orchestrates real-time app-usage
 * tracking for Bilbo.
 *
 * ### Responsibilities
 * 1. Creates the "Bilbo Tracking" notification channel and shows a persistent
 *    notification.
 * 2. Injects the correct [AppMonitor] implementation — [PollingAppMonitor] for
 *    the Play Store flavor, [AccessibilityAppMonitor] for the GitHub flavor —
 *    via Hilt (see [AppMonitorModule]).
 * 3. Creates a [SessionTracker] and wires it to [AppMonitor] callbacks.
 * 4. Registers an internal [BroadcastReceiver] for screen on/off events.
 * 5. Returns [START_STICKY] so Android restarts it after process death.
 *
 * ### Battery optimisation
 * Call [requestBatteryOptimisationExemption] once (e.g. from Onboarding) to
 * ask the user to whitelist Bilbo from battery optimisation; otherwise Android
 * may kill the service aggressively on some OEMs.
 */
@AndroidEntryPoint
class UsageTrackingService : android.app.Service() {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "bilbo_tracking_channel"
        const val ACTION_START = "dev.bilbo.app.action.START_TRACKING"
        const val ACTION_STOP = "dev.bilbo.app.action.STOP_TRACKING"
        const val ACTION_SCREEN_OFF = "dev.bilbo.app.action.SCREEN_OFF"
        const val ACTION_SCREEN_ON = "dev.bilbo.app.action.SCREEN_ON"

        /**
         * Helper: launch the system dialog to exempt Bilbo from battery
         * optimisation.  Must be called from a UI context.
         */
        fun requestBatteryOptimisationExemption(context: Context) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            }
        }
    }

    // ── Hilt injected ─────────────────────────────────────────────────────────

    @Inject lateinit var appMonitor: AppMonitor
    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var appProfileRepository: AppProfileRepository
    @Inject lateinit var gatekeeperController: GatekeeperController

    // ── Internal state ────────────────────────────────────────────────────────

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sessionTracker: SessionTracker

    /** Handles screen on/off events inside the service boundary. */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Timber.d("UsageTrackingService: screen off")
                    appMonitor.stopMonitoring()
                    sessionTracker.onScreenOff()
                }
                Intent.ACTION_USER_PRESENT -> {
                    Timber.d("UsageTrackingService: user present (unlocked)")
                    sessionTracker.onScreenOn()
                    appMonitor.startMonitoring()
                }
                Intent.ACTION_SCREEN_ON -> {
                    // Screen on but still on lock screen — monitoring starts on
                    // ACTION_USER_PRESENT (unlock) instead.
                    Timber.d("UsageTrackingService: screen on (lock screen)")
                }
            }
        }
    }

    // ── Service lifecycle ─────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        sessionTracker = SessionTracker(usageRepository, appProfileRepository)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        // Promote to foreground immediately
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            },
        )

        wireMonitor()
        gatekeeperController.attach(appMonitor)
        registerScreenReceiver()
        appMonitor.startMonitoring()

        Timber.d("UsageTrackingService: started")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        appMonitor.stopMonitoring()
        sessionTracker.stop()
        gatekeeperController.dismiss()
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) { /* not registered */ }
        serviceScope.cancel()
        Timber.d("UsageTrackingService: destroyed")
    }

    // ── Wiring ────────────────────────────────────────────────────────────────

    private fun wireMonitor() {
        appMonitor.onAppChanged { appInfo ->
            sessionTracker.onAppChanged(appInfo)
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    // ── Notification ──────────────────────────────────────────────────────────

    private fun buildNotification(): Notification {
        // Tap the notification to open the app
        val contentIntent = packageManager
            .getLaunchIntentForPackage(packageName)
            ?.let { launchIntent ->
                PendingIntent.getActivity(
                    this, 0, launchIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        // "Stop" action
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, UsageTrackingService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bilbo is monitoring your screen time")
            .setContentText("Helping you build mindful digital habits")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setContentIntent(contentIntent)
            .addAction(0, "Stop", stopIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Bilbo Tracking",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Tracks your app usage to provide wellness insights"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
