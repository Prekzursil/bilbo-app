// IosBilboPreferences.kt
// Bilbo — iOS Preferences (NSUserDefaults implementation)
//
// Backed by the App Group UserDefaults so the main app and extensions
// share the same preference store.

package dev.spark.preferences

import platform.Foundation.NSUserDefaults
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// MARK: - App Group suite name (must match entitlements)
private const val APP_GROUP = "group.dev.spark.app"

// MARK: - Keys

private object Keys {
    const val ENFORCEMENT_MODE          = "enforcement_mode"
    const val COOLDOWN_MINUTES          = "cooldown_minutes"
    const val BYPASS_LIST               = "bypass_list"
    const val FP_ENABLED                = "fp_enabled"
    const val DAILY_BASELINE_FP         = "daily_baseline_fp"
    const val ANTI_GAMING_ENABLED       = "anti_gaming_enabled"
    const val CHECK_IN_ENABLED          = "check_in_enabled"
    const val COOLING_OFF_ENABLED       = "cooling_off_enabled"
    const val CLOUD_INSIGHTS_ENABLED    = "cloud_insights_enabled"
    const val VIEW_ANONYMIZATION        = "view_anonymization"
    const val SHARING_LEVEL             = "sharing_level"
    const val USER_INTERESTS            = "user_interests"
    const val NOTIFICATION_PREFS        = "notification_prefs"
    const val ONBOARDING_COMPLETED      = "onboarding_completed"
    const val SEED_DATA_LOADED          = "seed_data_loaded"
    const val USER_ID                   = "user_id"
    const val DEVICE_TOKEN              = "device_token"
}

// MARK: - Implementation

class IosBilboPreferences : BilboPreferences {

    private val defaults: NSUserDefaults =
        NSUserDefaults(suiteName = APP_GROUP) ?: NSUserDefaults.standardUserDefaults

    private val json = Json { ignoreUnknownKeys = true }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun getString(key: String, default: String? = null): String? =
        defaults.stringForKey(key) ?: default

    private fun setString(key: String, value: String?) {
        if (value != null) defaults.setObject(value, key) else defaults.removeObjectForKey(key)
    }

    private fun getBool(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

    private fun setBool(key: String, value: Boolean) = defaults.setBool(value, key)

    private fun getInt(key: String, default: Int): Int =
        (defaults.objectForKey(key)?.let { defaults.integerForKey(key) }?.toInt()) ?: default

    private fun setInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)

    private inline fun <reified T> getJson(key: String, default: T): T =
        try {
            json.decodeFromString(getString(key) ?: return default)
        } catch (e: Exception) {
            default
        }

    private inline fun <reified T> setJson(key: String, value: T) =
        setString(key, json.encodeToString(value))

    // ── Enforcement ──────────────────────────────────────────────────────

    override var defaultEnforcementMode: DefaultEnforcementMode
        get() = DefaultEnforcementMode.valueOf(getString(Keys.ENFORCEMENT_MODE) ?: DefaultEnforcementMode.SOFT_LOCK.name)
        set(value) = setString(Keys.ENFORCEMENT_MODE, value.name)

    override var cooldownMinutes: Int
        get() = getInt(Keys.COOLDOWN_MINUTES, 15)
        set(value) = setInt(Keys.COOLDOWN_MINUTES, value)

    override var bypassList: List<String>
        get() = getJson(Keys.BYPASS_LIST, emptyList())
        set(value) = setJson(Keys.BYPASS_LIST, value)

    // ── Economy ──────────────────────────────────────────────────────────

    override var fpEnabled: Boolean
        get() = getBool(Keys.FP_ENABLED, true)
        set(value) = setBool(Keys.FP_ENABLED, value)

    override var dailyBaselineFP: Int
        get() = getInt(Keys.DAILY_BASELINE_FP, 60)
        set(value) = setInt(Keys.DAILY_BASELINE_FP, value)

    override var antiGamingEnabled: Boolean
        get() = getBool(Keys.ANTI_GAMING_ENABLED, true)
        set(value) = setBool(Keys.ANTI_GAMING_ENABLED, value)

    // ── Emotional ────────────────────────────────────────────────────────

    override var checkInEnabled: Boolean
        get() = getBool(Keys.CHECK_IN_ENABLED, true)
        set(value) = setBool(Keys.CHECK_IN_ENABLED, value)

    override var coolingOffEnabled: Boolean
        get() = getBool(Keys.COOLING_OFF_ENABLED, true)
        set(value) = setBool(Keys.COOLING_OFF_ENABLED, value)

    // ── AI ───────────────────────────────────────────────────────────────

    override var cloudInsightsEnabled: Boolean
        get() = getBool(Keys.CLOUD_INSIGHTS_ENABLED, true)
        set(value) = setBool(Keys.CLOUD_INSIGHTS_ENABLED, value)

    override var viewAnonymization: Boolean
        get() = getBool(Keys.VIEW_ANONYMIZATION, true)
        set(value) = setBool(Keys.VIEW_ANONYMIZATION, value)

    // ── Social ───────────────────────────────────────────────────────────

    override var sharingLevel: SharingLevelPref
        get() = SharingLevelPref.valueOf(getString(Keys.SHARING_LEVEL) ?: SharingLevelPref.FRIENDS.name)
        set(value) = setString(Keys.SHARING_LEVEL, value.name)

    override var userInterests: List<String>
        get() = getJson(Keys.USER_INTERESTS, emptyList())
        set(value) = setJson(Keys.USER_INTERESTS, value)

    // ── Notifications ────────────────────────────────────────────────────

    override var notificationPreferences: NotificationPreferences
        get() = getJson(Keys.NOTIFICATION_PREFS, NotificationPreferences())
        set(value) = setJson(Keys.NOTIFICATION_PREFS, value)

    // ── Onboarding ───────────────────────────────────────────────────────

    override var onboardingCompleted: Boolean
        get() = getBool(Keys.ONBOARDING_COMPLETED, false)
        set(value) = setBool(Keys.ONBOARDING_COMPLETED, value)

    override var seedDataLoaded: Boolean
        get() = getBool(Keys.SEED_DATA_LOADED, false)
        set(value) = setBool(Keys.SEED_DATA_LOADED, value)

    // ── User ─────────────────────────────────────────────────────────────

    override var userId: String?
        get() = getString(Keys.USER_ID)
        set(value) = setString(Keys.USER_ID, value)

    override var deviceToken: String?
        get() = getString(Keys.DEVICE_TOKEN)
        set(value) = setString(Keys.DEVICE_TOKEN, value)

    // ── Clear ────────────────────────────────────────────────────────────

    override fun clear() {
        listOf(
            Keys.ENFORCEMENT_MODE, Keys.COOLDOWN_MINUTES, Keys.BYPASS_LIST,
            Keys.FP_ENABLED, Keys.DAILY_BASELINE_FP, Keys.ANTI_GAMING_ENABLED,
            Keys.CHECK_IN_ENABLED, Keys.COOLING_OFF_ENABLED,
            Keys.CLOUD_INSIGHTS_ENABLED, Keys.VIEW_ANONYMIZATION,
            Keys.SHARING_LEVEL, Keys.USER_INTERESTS, Keys.NOTIFICATION_PREFS,
            Keys.ONBOARDING_COMPLETED, Keys.SEED_DATA_LOADED,
            Keys.USER_ID, Keys.DEVICE_TOKEN
        ).forEach { defaults.removeObjectForKey(it) }
    }
}

// MARK: - actual factory

actual fun sparkPreferences(): BilboPreferences = IosBilboPreferences()
