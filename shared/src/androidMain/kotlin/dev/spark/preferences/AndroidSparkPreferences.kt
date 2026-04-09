// AndroidSparkPreferences.kt
// Spark — Android Preferences (DataStore implementation)
//
// Uses Jetpack DataStore Preferences for type-safe async storage.
// Exposes synchronous-style properties backed by runBlocking for
// KMP interface compatibility. Production callers should prefer
// the Flow-based accessors directly.

package dev.spark.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// MARK: - DataStore singleton extension

private val Context.sparkDataStore: DataStore<Preferences> by preferencesDataStore(name = "spark_prefs")

// MARK: - Keys

private object Keys {
    val ENFORCEMENT_MODE          = stringPreferencesKey("enforcement_mode")
    val COOLDOWN_MINUTES          = intPreferencesKey("cooldown_minutes")
    val BYPASS_LIST               = stringPreferencesKey("bypass_list")            // JSON array
    val FP_ENABLED                = booleanPreferencesKey("fp_enabled")
    val DAILY_BASELINE_FP         = intPreferencesKey("daily_baseline_fp")
    val ANTI_GAMING_ENABLED       = booleanPreferencesKey("anti_gaming_enabled")
    val CHECK_IN_ENABLED          = booleanPreferencesKey("check_in_enabled")
    val COOLING_OFF_ENABLED       = booleanPreferencesKey("cooling_off_enabled")
    val CLOUD_INSIGHTS_ENABLED    = booleanPreferencesKey("cloud_insights_enabled")
    val VIEW_ANONYMIZATION        = booleanPreferencesKey("view_anonymization")
    val SHARING_LEVEL             = stringPreferencesKey("sharing_level")
    val USER_INTERESTS            = stringPreferencesKey("user_interests")         // JSON array
    val NOTIFICATION_PREFS        = stringPreferencesKey("notification_prefs")     // JSON object
    val ONBOARDING_COMPLETED      = booleanPreferencesKey("onboarding_completed")
    val SEED_DATA_LOADED          = booleanPreferencesKey("seed_data_loaded")
    val USER_ID                   = stringPreferencesKey("user_id")
    val DEVICE_TOKEN              = stringPreferencesKey("device_token")
}

// MARK: - Implementation

class AndroidSparkPreferences(private val context: Context) : SparkPreferences {

    private val store = context.sparkDataStore
    private val json = Json { ignoreUnknownKeys = true }

    // ── Helpers ──────────────────────────────────────────────────────────

    private fun <T> get(block: Preferences.() -> T): T =
        runBlocking { store.data.first().block() }

    private fun <T> set(block: MutablePreferences.() -> Unit) =
        runBlocking { store.edit { prefs -> prefs.block() } }

    // ── Enforcement ──────────────────────────────────────────────────────

    override var defaultEnforcementMode: DefaultEnforcementMode
        get() = DefaultEnforcementMode.valueOf(get { get(Keys.ENFORCEMENT_MODE) ?: DefaultEnforcementMode.SOFT_LOCK.name })
        set(value) = set { this[Keys.ENFORCEMENT_MODE] = value.name }

    override var cooldownMinutes: Int
        get() = get { get(Keys.COOLDOWN_MINUTES) ?: 15 }
        set(value) = set { this[Keys.COOLDOWN_MINUTES] = value }

    override var bypassList: List<String>
        get() = json.decodeFromString(get { get(Keys.BYPASS_LIST) ?: "[]" })
        set(value) = set { this[Keys.BYPASS_LIST] = json.encodeToString(value) }

    // ── Economy ──────────────────────────────────────────────────────────

    override var fpEnabled: Boolean
        get() = get { get(Keys.FP_ENABLED) ?: true }
        set(value) = set { this[Keys.FP_ENABLED] = value }

    override var dailyBaselineFP: Int
        get() = get { get(Keys.DAILY_BASELINE_FP) ?: 60 }
        set(value) = set { this[Keys.DAILY_BASELINE_FP] = value }

    override var antiGamingEnabled: Boolean
        get() = get { get(Keys.ANTI_GAMING_ENABLED) ?: true }
        set(value) = set { this[Keys.ANTI_GAMING_ENABLED] = value }

    // ── Emotional ────────────────────────────────────────────────────────

    override var checkInEnabled: Boolean
        get() = get { get(Keys.CHECK_IN_ENABLED) ?: true }
        set(value) = set { this[Keys.CHECK_IN_ENABLED] = value }

    override var coolingOffEnabled: Boolean
        get() = get { get(Keys.COOLING_OFF_ENABLED) ?: true }
        set(value) = set { this[Keys.COOLING_OFF_ENABLED] = value }

    // ── AI ───────────────────────────────────────────────────────────────

    override var cloudInsightsEnabled: Boolean
        get() = get { get(Keys.CLOUD_INSIGHTS_ENABLED) ?: true }
        set(value) = set { this[Keys.CLOUD_INSIGHTS_ENABLED] = value }

    override var viewAnonymization: Boolean
        get() = get { get(Keys.VIEW_ANONYMIZATION) ?: true }
        set(value) = set { this[Keys.VIEW_ANONYMIZATION] = value }

    // ── Social ───────────────────────────────────────────────────────────

    override var sharingLevel: SharingLevelPref
        get() = SharingLevelPref.valueOf(get { get(Keys.SHARING_LEVEL) ?: SharingLevelPref.FRIENDS.name })
        set(value) = set { this[Keys.SHARING_LEVEL] = value.name }

    override var userInterests: List<String>
        get() = json.decodeFromString(get { get(Keys.USER_INTERESTS) ?: "[]" })
        set(value) = set { this[Keys.USER_INTERESTS] = json.encodeToString(value) }

    // ── Notifications ────────────────────────────────────────────────────

    override var notificationPreferences: NotificationPreferences
        get() = try {
            json.decodeFromString(get { get(Keys.NOTIFICATION_PREFS) ?: "{}" })
        } catch (e: Exception) {
            NotificationPreferences()
        }
        set(value) = set { this[Keys.NOTIFICATION_PREFS] = json.encodeToString(value) }

    // ── Onboarding ───────────────────────────────────────────────────────

    override var onboardingCompleted: Boolean
        get() = get { get(Keys.ONBOARDING_COMPLETED) ?: false }
        set(value) = set { this[Keys.ONBOARDING_COMPLETED] = value }

    override var seedDataLoaded: Boolean
        get() = get { get(Keys.SEED_DATA_LOADED) ?: false }
        set(value) = set { this[Keys.SEED_DATA_LOADED] = value }

    // ── User ─────────────────────────────────────────────────────────────

    override var userId: String?
        get() = get { get(Keys.USER_ID) }
        set(value) = set { if (value != null) this[Keys.USER_ID] = value else remove(Keys.USER_ID) }

    override var deviceToken: String?
        get() = get { get(Keys.DEVICE_TOKEN) }
        set(value) = set { if (value != null) this[Keys.DEVICE_TOKEN] = value else remove(Keys.DEVICE_TOKEN) }

    // ── Clear ────────────────────────────────────────────────────────────

    override fun clear() = runBlocking { store.edit { it.clear() } }
}

// MARK: - actual factory

actual fun sparkPreferences(): SparkPreferences {
    throw IllegalStateException(
        "On Android, inject AndroidSparkPreferences(context) via your DI framework. " +
        "This expect/actual factory is not usable without a Context."
    )
}
