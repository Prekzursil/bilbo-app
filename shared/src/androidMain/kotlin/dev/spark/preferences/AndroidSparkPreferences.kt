package dev.spark.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AndroidSparkPreferences(context: Context) : SparkPreferences {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("spark_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    // ── Enforcement ──────────────────────────────────────────────────────

    override var defaultEnforcementMode: DefaultEnforcementMode
        get() = DefaultEnforcementMode.valueOf(prefs.getString("enforcement_mode", DefaultEnforcementMode.SOFT_LOCK.name)!!)
        set(value) { prefs.edit().putString("enforcement_mode", value.name).apply() }

    override var cooldownMinutes: Int
        get() = prefs.getInt("cooldown_minutes", 15)
        set(value) { prefs.edit().putInt("cooldown_minutes", value).apply() }

    override var bypassList: List<String>
        get() = json.decodeFromString(prefs.getString("bypass_list", "[]")!!)
        set(value) { prefs.edit().putString("bypass_list", json.encodeToString(value)).apply() }

    // ── Economy ──────────────────────────────────────────────────────────

    override var fpEnabled: Boolean
        get() = prefs.getBoolean("fp_enabled", true)
        set(value) { prefs.edit().putBoolean("fp_enabled", value).apply() }

    override var dailyBaselineFP: Int
        get() = prefs.getInt("daily_baseline_fp", 60)
        set(value) { prefs.edit().putInt("daily_baseline_fp", value).apply() }

    override var antiGamingEnabled: Boolean
        get() = prefs.getBoolean("anti_gaming_enabled", true)
        set(value) { prefs.edit().putBoolean("anti_gaming_enabled", value).apply() }

    // ── Emotional ────────────────────────────────────────────────────────

    override var checkInEnabled: Boolean
        get() = prefs.getBoolean("check_in_enabled", true)
        set(value) { prefs.edit().putBoolean("check_in_enabled", value).apply() }

    override var coolingOffEnabled: Boolean
        get() = prefs.getBoolean("cooling_off_enabled", true)
        set(value) { prefs.edit().putBoolean("cooling_off_enabled", value).apply() }

    // ── AI ───────────────────────────────────────────────────────────────

    override var cloudInsightsEnabled: Boolean
        get() = prefs.getBoolean("cloud_insights_enabled", true)
        set(value) { prefs.edit().putBoolean("cloud_insights_enabled", value).apply() }

    override var viewAnonymization: Boolean
        get() = prefs.getBoolean("view_anonymization", true)
        set(value) { prefs.edit().putBoolean("view_anonymization", value).apply() }

    // ── Social ───────────────────────────────────────────────────────────

    override var sharingLevel: SharingLevelPref
        get() = SharingLevelPref.valueOf(prefs.getString("sharing_level", SharingLevelPref.FRIENDS.name)!!)
        set(value) { prefs.edit().putString("sharing_level", value.name).apply() }

    override var userInterests: List<String>
        get() = json.decodeFromString(prefs.getString("user_interests", "[]")!!)
        set(value) { prefs.edit().putString("user_interests", json.encodeToString(value)).apply() }

    // ── Notifications ────────────────────────────────────────────────────

    override var notificationPreferences: NotificationPreferences
        get() = try {
            json.decodeFromString(prefs.getString("notification_prefs", "{}")!!)
        } catch (_: Exception) {
            NotificationPreferences()
        }
        set(value) { prefs.edit().putString("notification_prefs", json.encodeToString(value)).apply() }

    // ── Onboarding ───────────────────────────────────────────────────────

    override var onboardingCompleted: Boolean
        get() = prefs.getBoolean("onboarding_completed", false)
        set(value) { prefs.edit().putBoolean("onboarding_completed", value).apply() }

    override var seedDataLoaded: Boolean
        get() = prefs.getBoolean("seed_data_loaded", false)
        set(value) { prefs.edit().putBoolean("seed_data_loaded", value).apply() }

    // ── User ─────────────────────────────────────────────────────────────

    override var userId: String?
        get() = prefs.getString("user_id", null)
        set(value) { prefs.edit().putString("user_id", value).apply() }

    override var deviceToken: String?
        get() = prefs.getString("device_token", null)
        set(value) { prefs.edit().putString("device_token", value).apply() }

    // ── Clear ────────────────────────────────────────────────────────────

    override fun clear() { prefs.edit().clear().apply() }
}

actual fun sparkPreferences(): SparkPreferences {
    throw IllegalStateException(
        "On Android, inject AndroidSparkPreferences(context) via your DI framework. " +
        "This expect/actual factory is not usable without a Context."
    )
}
