package dev.spark.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import dev.spark.app.BuildConfig
import dev.spark.tracking.AppInfo
import timber.log.Timber

/**
 * Accessibility Service used **only** in the `github` distribution flavor,
 * where [BuildConfig.USE_ACCESSIBILITY_SERVICE] is `true`.
 *
 * Monitors foreground app changes via [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]
 * to provide accurate per-app screen time data without requiring the
 * `PACKAGE_USAGE_STATS` permission (which requires special Google Play approval).
 *
 * Detected app changes are broadcast to [dev.spark.app.tracking.AccessibilityAppMonitor]
 * via the companion-object [MutableLiveData] bridge, which is observed from the
 * [UsageTrackingService].
 *
 * The user must manually enable "Spark" under
 * Settings → Accessibility → Downloaded apps.
 */
class SparkAccessibilityService : AccessibilityService() {

    private var currentPackageName: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        Timber.d("SparkAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!BuildConfig.USE_ACCESSIBILITY_SERVICE) return
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == currentPackageName) return

        Timber.d("SparkAccessibilityService: foreground changed: $currentPackageName → $packageName")
        currentPackageName = packageName

        // Resolve a human-readable label via PackageManager.
        val appLabel = resolveLabel(packageName)

        val appInfo = AppInfo(
            packageName = packageName,
            appLabel = appLabel,
            category = null, // resolved later by SessionTracker
        )

        // Post to the bridge so AccessibilityAppMonitor can pick it up.
        try {
            dev.spark.app.tracking.AccessibilityAppMonitor.foregroundAppLiveData
                .postValue(appInfo)
        } catch (e: Exception) {
            Timber.e(e, "SparkAccessibilityService: failed to post foreground app")
        }
    }

    override fun onInterrupt() {
        Timber.w("SparkAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SparkAccessibilityService destroyed")
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun resolveLabel(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }
}
