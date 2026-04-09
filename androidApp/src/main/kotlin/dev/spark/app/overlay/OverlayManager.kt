package dev.spark.app.overlay

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dev.spark.tracking.AppInfo
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages system-level overlay windows for Spark using [WindowManager] with
 * [WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY].
 *
 * ### Permissions
 * Requires `android.permission.SYSTEM_ALERT_WINDOW`.  Call
 * [hasOverlayPermission] before [showGatekeeper] and direct the user to
 * [requestOverlayPermission] if needed.
 *
 * ### Lifecycle
 * Each call to [showGatekeeper] replaces any existing overlay.  [dismiss]
 * removes the view from the window and releases Compose resources.  The caller
 * must ensure [dismiss] is always called (e.g. on service destroy) to prevent
 * window leaks.
 *
 * @param context Application context — must NOT be an Activity context since the
 *                overlay outlives any single activity.
 */
@Singleton
class OverlayManager @Inject constructor(
    private val context: Context,
) {

    private val windowManager: WindowManager by lazy {
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    /** Currently-displayed overlay view, or null when hidden. */
    private var currentOverlayView: View? = null

    /** Fake lifecycle owner so ComposeView can work outside an Activity. */
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /** Returns true if the SYSTEM_ALERT_WINDOW permission is granted. */
    fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(context)

    /**
     * Open the system Settings screen where the user can grant
     * SYSTEM_ALERT_WINDOW to Spark.
     */
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        context.startActivity(intent)
    }

    /**
     * Inflate [content] as a system overlay.  If an overlay is already shown
     * it is first dismissed.
     *
     * @param appInfo  The app that triggered this gatekeeper (used by content).
     * @param content  Composable lambda — receives [appInfo] and a dismiss
     *                 callback.
     */
    fun showGatekeeper(
        appInfo: AppInfo,
        content: @Composable (appInfo: AppInfo, onDismiss: () -> Unit) -> Unit,
    ) {
        if (!hasOverlayPermission()) {
            Timber.w("OverlayManager: SYSTEM_ALERT_WINDOW not granted — skipping overlay")
            return
        }

        // Remove existing overlay first
        dismiss()

        val lco = OverlayLifecycleOwner().also { lifecycleOwner = it }
        lco.start()

        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lco)
            setViewTreeViewModelStoreOwner(lco)
            setViewTreeSavedStateRegistryOwner(lco)
            setContent {
                content(appInfo) { dismiss() }
            }
        }

        val params = buildLayoutParams()
        try {
            windowManager.addView(composeView, params)
            currentOverlayView = composeView
            Timber.d("OverlayManager: overlay shown for ${appInfo.packageName}")
        } catch (e: Exception) {
            Timber.e(e, "OverlayManager: failed to add overlay view")
            lco.stop()
            lifecycleOwner = null
        }
    }

    /** Remove and release the current overlay. */
    fun dismiss() {
        currentOverlayView?.let { view ->
            try {
                windowManager.removeView(view)
                Timber.d("OverlayManager: overlay dismissed")
            } catch (e: Exception) {
                Timber.e(e, "OverlayManager: error removing overlay view")
            }
        }
        currentOverlayView = null
        lifecycleOwner?.stop()
        lifecycleOwner = null
    }

    /** Returns true if an overlay is currently visible. */
    fun isShowing(): Boolean = currentOverlayView != null

    // ── Internal helpers ──────────────────────────────────────────────────────

    private fun buildLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            // Allow soft keyboard if the intention text field is focused
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                    or WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE
        }
    }

    // ── Fake lifecycle for ComposeView outside Activity ───────────────────────

    /**
     * Minimal [LifecycleOwner] / [ViewModelStoreOwner] / [SavedStateRegistryOwner]
     * implementation that lets a [ComposeView] function correctly when it is not
     * attached to an Activity.
     */
    private class OverlayLifecycleOwner :
        LifecycleOwner,
        ViewModelStoreOwner,
        SavedStateRegistryOwner {

        private val registry = LifecycleRegistry(this)
        private val savedStateController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = registry
        override val viewModelStore: ViewModelStore get() = store
        override val savedStateRegistry: SavedStateRegistry
            get() = savedStateController.savedStateRegistry

        fun start() {
            savedStateController.performRestore(null)
            registry.currentState = Lifecycle.State.RESUMED
        }

        fun stop() {
            registry.currentState = Lifecycle.State.DESTROYED
            store.clear()
        }
    }
}
