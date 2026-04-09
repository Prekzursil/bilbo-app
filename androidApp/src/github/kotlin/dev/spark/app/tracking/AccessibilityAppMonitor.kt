package dev.spark.app.tracking

import android.content.Context
import dev.spark.tracking.AppInfo
import dev.spark.tracking.AppMonitor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub flavor implementation of [AppMonitor].
 *
 * Delegates foreground-app detection to [BilboAccessibilityService], which
 * fires [TYPE_WINDOW_STATE_CHANGED] events.  Because an
 * [android.accessibilityservice.AccessibilityService] runs in the same process
 * but in a different component, communication uses a companion-object singleton
 * holding a [MutableLiveData].
 *
 * The user must manually enable "Bilbo" under
 * Settings → Accessibility → Downloaded apps before this monitor becomes
 * functional.  [getCurrentForegroundApp] returns null until the service fires
 * its first event.
 */
@Singleton
class AccessibilityAppMonitor @Inject constructor(
    private val context: Context,
) : AppMonitor {

    private var appChangedCallback: ((AppInfo) -> Unit)? = null
    private var isMonitoring = false

    /** Last-known foreground app as observed from LiveData. */
    private var lastKnownApp: AppInfo? = null

    // ── AppMonitor impl ───────────────────────────────────────────────────────

    override fun getCurrentForegroundApp(): AppInfo? = lastKnownApp

    override fun startMonitoring() {
        if (isMonitoring) return
        isMonitoring = true

        // Observe LiveData on the main thread; callback is forwarded on every
        // distinct package change.
        ForegroundAppBridge.foregroundAppLiveData.observeForever { appInfo ->
            if (!isMonitoring) return@observeForever
            if (appInfo == null) return@observeForever
            if (appInfo.packageName == lastKnownApp?.packageName) return@observeForever

            Timber.d("AccessibilityAppMonitor: foreground → ${appInfo.packageName}")
            lastKnownApp = appInfo
            appChangedCallback?.invoke(appInfo)
        }
        Timber.d("AccessibilityAppMonitor: started — waiting for accessibility events")
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return
        isMonitoring = false
        // Removing the observer here would require a LifecycleOwner reference;
        // instead we gate processing on the isMonitoring flag.
        Timber.d("AccessibilityAppMonitor: stopped")
    }

    override fun onAppChanged(callback: (AppInfo) -> Unit) {
        appChangedCallback = callback
    }
}
