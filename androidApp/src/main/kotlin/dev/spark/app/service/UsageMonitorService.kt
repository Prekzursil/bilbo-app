package dev.spark.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.spark.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Foreground service that monitors app usage via the UsageStatsManager API
 * (playstore flavor) or the AccessibilityService (github flavor, controlled
 * by [dev.spark.app.BuildConfig.USE_ACCESSIBILITY_SERVICE]).
 *
 * Runs a polling loop that snapshots usage stats every [POLL_INTERVAL_MS] ms
 * and forwards data to the shared repository for persistence and sync.
 */
@AndroidEntryPoint
class UsageMonitorService : Service() {

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "usage_monitor_channel"
        private const val POLL_INTERVAL_MS = 60_000L // 1 minute
        const val ACTION_START = "dev.spark.app.action.START_MONITOR"
        const val ACTION_STOP = "dev.spark.app.action.STOP_MONITOR"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    inner class LocalBinder : Binder() {
        fun getService(): UsageMonitorService = this@UsageMonitorService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
        }

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            } else {
                0
            }
        )

        startMonitoringLoop()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (true) {
                try {
                    pollUsageStats()
                } catch (e: Exception) {
                    Timber.e(e, "Error polling usage stats")
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private suspend fun pollUsageStats() {
        // TODO: Query UsageStatsManager for app usage in the last minute
        // and forward to InsightRepository for local storage and sync.
        Timber.d("Polling usage stats…")
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bilbo is tracking screen time")
            .setContentText("Helping you build mindful habits")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Usage Monitor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Tracks your app usage to provide wellness insights"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
