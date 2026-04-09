package dev.spark.app.tracking

import android.content.Context
import androidx.lifecycle.MutableLiveData
import dev.spark.tracking.AppInfo
import dev.spark.tracking.AppMonitor
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * GitHub flavor implementation of [AppMonitor].
 *
 * Delegates foreground-app detection to [SparkAccessibilityService], which
 * fires [TYPE_WINDOW_STATE_CHANGED] events.  Because an
 * [android.accessibilityservice.AccessibilityService] runs in the same process
 * but in a different component, communication uses a companion-object singleton
 * holding a [MutableLiveData].
 *
 * The user must manually enable "Spark" under
 * Settings → Accessibility → Downloaded apps before this monitor becomes
 * functional.  [getCurrentForegroundApp] returns null until the service fires
 * its first event.
 */
@Singleton
class AccessibilityAppMonitor @Inject constructor(
    private val context: Context,
) : AppMonitor {

    companion object {
        /**
         * Bridge between [SparkAccessibilityService] and this monitor.
         *
         * [SparkAccessibilityService] posts new [AppInfo] values here on every
         * [android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED].
         */
        val foregroundAppLiveData: MutableLiveData<AppInfo> = MutableLiveData()
    }

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
        foregroundAppLiveData.observeForever { appInfo ->
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
