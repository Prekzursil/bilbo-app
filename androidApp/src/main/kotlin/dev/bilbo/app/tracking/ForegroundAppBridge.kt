package dev.bilbo.app.tracking

import androidx.lifecycle.MutableLiveData
import dev.bilbo.tracking.AppInfo

/**
 * Process-global bridge between [BilboAccessibilityService] (which runs as a
 * system-managed component) and the flavor-specific [AppMonitor] implementation.
 *
 * Lives in the `main` source set so it is visible to both flavors and the
 * accessibility service.
 */
object ForegroundAppBridge {
    val foregroundAppLiveData: MutableLiveData<AppInfo> = MutableLiveData()
}
