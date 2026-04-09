package dev.bilbo.app.tracking

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.Process
import androidx.core.content.ContextCompat
import dev.bilbo.tracking.AppInfo
import dev.bilbo.tracking.AppMonitor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Store flavor implementation of [AppMonitor].
 *
 * Uses [UsageStatsManager] to determine the current foreground application by
 * querying the last-used app within a 5-second window.  A [Handler]/[Looper]
 * drives the polling loop at [POLL_INTERVAL_MS] intervals.
 *
 * The monitor automatically pauses when the screen turns off and resumes when
 * the screen turns back on, avoiding unnecessary battery drain.
 *
 * ### Permission requirement
 * Requires `android.permission.PACKAGE_USAGE_STATS` (a protected permission
 * that users must grant via Settings → Apps → Special app access → Usage
 * access).  If the permission is not granted, [getCurrentForegroundApp]
 * returns null and no polling occurs.
 */
@Singleton
class PollingAppMonitor @Inject constructor(
    private val context: Context,
) : AppMonitor {

    companion object {
        private const val POLL_INTERVAL_MS = 5_000L
        private const val QUERY_WINDOW_MS = 5_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var appChangedCallback: ((AppInfo) -> Unit)? = null
    private var lastForegroundPackage: String? = null
    private var isMonitoring = false

    private val usageStatsManager: UsageStatsManager by lazy {
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    }

    // ── Screen on/off receiver ────────────────────────────────────────────────

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    Timber.d("PollingAppMonitor: screen off — pausing poll")
                    handler.removeCallbacks(pollRunnable)
                }
                Intent.ACTION_SCREEN_ON -> {
                    Timber.d("PollingAppMonitor: screen on — resuming poll")
                    if (isMonitoring) schedulePoll()
                }
            }
        }
    }

    // ── Polling runnable ──────────────────────────────────────────────────────

    private val pollRunnable = object : Runnable {
        override fun run() {
            if (!isMonitoring) return
            try {
                pollForegroundApp()
            } catch (e: Exception) {
                Timber.e(e, "PollingAppMonitor: error during poll")
            }
            schedulePoll()
        }
    }

    // ── AppMonitor impl ───────────────────────────────────────────────────────

    override fun getCurrentForegroundApp(): AppInfo? {
        if (!hasUsageStatsPermission()) return null
        return queryCurrentForeground()
    }

    override fun startMonitoring() {
        if (isMonitoring) return
        if (!hasUsageStatsPermission()) {
            Timber.w("PollingAppMonitor: PACKAGE_USAGE_STATS not granted — monitoring skipped")
            return
        }
        isMonitoring = true
        registerScreenReceiver()
        schedulePoll()
        Timber.d("PollingAppMonitor: started")
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        handler.removeCallbacks(pollRunnable)
        try {
            context.unregisterReceiver(screenReceiver)
        } catch (_: IllegalArgumentException) {
            // Not registered — ignore
        }
        Timber.d("PollingAppMonitor: stopped")
    }

    override fun onAppChanged(callback: (AppInfo) -> Unit) {
        appChangedCallback = callback
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun schedulePoll() {
        handler.postDelayed(pollRunnable, POLL_INTERVAL_MS)
    }

    private fun pollForegroundApp() {
        val appInfo = queryCurrentForeground() ?: return
        if (appInfo.packageName == lastForegroundPackage) return

        Timber.d("PollingAppMonitor: foreground changed → ${appInfo.packageName}")
        lastForegroundPackage = appInfo.packageName
        appChangedCallback?.invoke(appInfo)
    }

    private fun queryCurrentForeground(): AppInfo? {
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - QUERY_WINDOW_MS,
            now,
        )
        if (stats.isNullOrEmpty()) return null

        val topStat = stats.maxByOrNull { it.lastTimeUsed } ?: return null
        val packageName = topStat.packageName ?: return null

        val appLabel = resolveLabel(packageName)
        return AppInfo(
            packageName = packageName,
            appLabel = appLabel,
            category = null, // resolved by SessionTracker via AppProfileRepository
        )
    }

    private fun resolveLabel(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    private fun registerScreenReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        ContextCompat.registerReceiver(
            context,
            screenReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
