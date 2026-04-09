package dev.spark.app.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import dev.spark.app.BuildConfig
import timber.log.Timber

/**
 * Accessibility Service used **only** in the `github` distribution flavor,
 * where [BuildConfig.USE_ACCESSIBILITY_SERVICE] is `true`.
 *
 * Monitors foreground app changes via [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED]
 * to provide accurate per-app screen time data without requiring the
 * `PACKAGE_USAGE_STATS` permission (which requires special Google Play approval).
 *
 * This service is declared in AndroidManifest.xml but the user must manually
 * enable it in System Settings → Accessibility → Spark.
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

        Timber.d("Foreground app changed: $currentPackageName -> $packageName")
        currentPackageName = packageName

        // TODO: Record session end for previous package and start for new package
        // via the shared InsightRepository (injected after migration to Hilt entry point)
    }

    override fun onInterrupt() {
        Timber.w("SparkAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("SparkAccessibilityService destroyed")
    }
}
