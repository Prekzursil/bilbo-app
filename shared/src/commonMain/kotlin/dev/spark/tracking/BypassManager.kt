package dev.spark.tracking

/**
 * Maintains the list of apps that should never trigger the Intent Gatekeeper.
 *
 * Default bypass entries cover essential Android system and utility apps.
 * Users can add or remove entries at runtime; changes are reflected immediately
 * in [shouldBypass] without a restart.
 */
class BypassManager {

    companion object {
        /**
         * Default package names that are bypassed out of the box.
         * Covers the most common AOSP and OEM system apps.
         */
        val DEFAULT_BYPASS_PACKAGES: Set<String> = setOf(
            // Phone / Dialer
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",

            // Messaging / SMS
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging",

            // Maps / Navigation
            "com.google.android.apps.maps",
            "com.waze",

            // Camera
            "com.android.camera",
            "com.android.camera2",
            "com.google.android.GoogleCamera",
            "com.samsung.android.app.camera",

            // System Settings
            "com.android.settings",
            "com.samsung.android.settings",

            // Clock / Alarms
            "com.android.deskclock",
            "com.google.android.deskclock",
            "com.samsung.android.app.clockpackage",

            // Calculator
            "com.android.calculator2",
            "com.google.android.calculator",
            "com.samsung.android.app.calculator",

            // Spark itself
            "dev.spark.app",
            "dev.spark.app.github",
            "dev.spark.app.debug",

            // Emergency / accessibility
            "com.android.emergency",
            "com.android.systemui",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.samsung.android.app.launcher",
        )
    }

    /** Mutable set combining defaults and user additions. */
    private val bypassSet: MutableSet<String> = DEFAULT_BYPASS_PACKAGES.toMutableSet()

    /**
     * Returns `true` if the given [packageName] should skip the gatekeeper.
     */
    fun shouldBypass(packageName: String): Boolean = packageName in bypassSet

    /**
     * Add [packageName] to the bypass list.  Idempotent.
     */
    fun addBypass(packageName: String) {
        bypassSet.add(packageName)
    }

    /**
     * Remove [packageName] from the bypass list.
     * Default entries can also be removed — changes persist for the lifetime
     * of this instance.
     */
    fun removeBypass(packageName: String) {
        bypassSet.remove(packageName)
    }

    /**
     * Replace the entire user-configured portion of the bypass list.
     * Default entries are re-seeded and [userPackages] are added on top.
     */
    fun setUserBypass(userPackages: Set<String>) {
        bypassSet.clear()
        bypassSet.addAll(DEFAULT_BYPASS_PACKAGES)
        bypassSet.addAll(userPackages)
    }

    /** Return a snapshot of the current bypass set. */
    fun getBypassList(): Set<String> = bypassSet.toSet()
}
