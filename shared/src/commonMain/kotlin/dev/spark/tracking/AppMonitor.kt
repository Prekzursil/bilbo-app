package dev.spark.tracking

/**
 * Represents a currently-active foreground application.
 *
 * @param packageName The Android package name (e.g. "com.instagram.android").
 * @param appLabel    The human-readable app name resolved via PackageManager.
 * @param category    Bilbo's classification of this app, or null if not yet
 *                    profiled.
 */
data class AppInfo(
    val packageName: String,
    val appLabel: String,
    val category: dev.spark.domain.AppCategory?
)

/**
 * Abstraction over platform-specific foreground-app detection.
 *
 * Two concrete implementations exist:
 *  - `PollingAppMonitor`  (playstore flavor) — polls [UsageStatsManager] every
 *    5 seconds.
 *  - `AccessibilityAppMonitor` (github flavor) — driven by
 *    [BilboAccessibilityService] events.
 *
 * Both are injected via Hilt in [UsageTrackingService]; callers should only
 * interact with this interface.
 */
interface AppMonitor {

    /**
     * Return the app currently in the foreground, or null if it cannot be
     * determined (e.g. permission not granted, screen off).
     */
    fun getCurrentForegroundApp(): AppInfo?

    /**
     * Begin observing foreground app changes.  Safe to call multiple times;
     * subsequent calls are no-ops if already monitoring.
     */
    fun startMonitoring()

    /**
     * Stop observing foreground app changes and release any held resources.
     */
    fun stopMonitoring()

    /**
     * Register a [callback] that will be invoked on every foreground-app
     * transition.  Only one callback is retained at a time; calling this again
     * replaces the previous one.
     */
    fun onAppChanged(callback: (AppInfo) -> Unit)
}
