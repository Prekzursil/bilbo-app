// SparkPreferences.kt
// Spark — Shared Preferences Interface
//
// Defines the cross-platform preferences contract via expect/actual.
// Covers all app-level toggles, values, and flags.

package dev.spark.preferences

// MARK: - Enforcement mode enum (shared)

enum class DefaultEnforcementMode {
    SOFT_LOCK,
    HARD_LOCK,
    TRACK_ONLY
}

// MARK: - Sharing level enum (shared)

enum class SharingLevelPref {
    PRIVATE,
    FRIENDS,
    CIRCLE,
    PUBLIC
}

// MARK: - Notification preference model

data class NotificationPreferences(
    val nudgeEnabled: Boolean = true,
    val weeklyInsightEnabled: Boolean = true,
    val challengeUpdateEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 8,
    val quietEndMinute: Int = 0
)

// MARK: - SparkPreferences interface

/**
 * Cross-platform preferences interface.
 * Implemented per-platform via expect/actual.
 */
interface SparkPreferences {

    // ── Enforcement ──────────────────────────────────────────────────────
    var defaultEnforcementMode: DefaultEnforcementMode
    var cooldownMinutes: Int
    var bypassList: List<String>            // Bundle IDs / package names exempt from enforcement

    // ── Economy ──────────────────────────────────────────────────────────
    var fpEnabled: Boolean
    var dailyBaselineFP: Int
    var antiGamingEnabled: Boolean

    // ── Emotional ────────────────────────────────────────────────────────
    var checkInEnabled: Boolean
    var coolingOffEnabled: Boolean

    // ── AI ───────────────────────────────────────────────────────────────
    var cloudInsightsEnabled: Boolean
    var viewAnonymization: Boolean

    // ── Social ───────────────────────────────────────────────────────────
    var sharingLevel: SharingLevelPref
    var userInterests: List<String>         // Tags used for analog suggestions

    // ── Notifications ────────────────────────────────────────────────────
    var notificationPreferences: NotificationPreferences

    // ── Onboarding ───────────────────────────────────────────────────────
    var onboardingCompleted: Boolean
    var seedDataLoaded: Boolean

    // ── User ─────────────────────────────────────────────────────────────
    var userId: String?
    var deviceToken: String?

    // ── Helpers ──────────────────────────────────────────────────────────
    fun clear()
}

// MARK: - expect factory

/**
 * Platform-provided factory for SparkPreferences.
 * Returns the correct implementation per platform.
 */
expect fun sparkPreferences(): SparkPreferences
