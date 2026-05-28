// BilboPreferences.kt
// Bilbo — Shared Preferences Interface
//
// Defines the cross-platform preferences contract via expect/actual.
// Covers all app-level toggles, values, and flags.

package dev.bilbo.preferences

import kotlinx.serialization.Serializable

// MARK: - Enforcement mode enum (shared)

enum class DefaultEnforcementMode {
    SOFT_LOCK,
    HARD_LOCK,
    TRACK_ONLY,
}

// MARK: - Sharing level enum (shared)

enum class SharingLevelPref {
    PRIVATE,
    FRIENDS,
    CIRCLE,
    PUBLIC,
}

// MARK: - Notification preference model

@Serializable
data class NotificationPreferences(
    val nudgeEnabled: Boolean = true,
    val weeklyInsightEnabled: Boolean = true,
    val challengeUpdateEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = true,
    val quietStartHour: Int = 22,
    val quietStartMinute: Int = 0,
    val quietEndHour: Int = 8,
    val quietEndMinute: Int = 0,
)

// MARK: - BilboPreferences interface

/** Enforcement-related preference toggles and values. */
interface EnforcementPreferences {
    var defaultEnforcementMode: DefaultEnforcementMode
    var cooldownMinutes: Int
    var bypassList: List<String> // Bundle IDs / package names exempt from enforcement
}

/** Focus-economy preference toggles and values. */
interface EconomyPreferences {
    var fpEnabled: Boolean
    var dailyBaselineFP: Int
    var antiGamingEnabled: Boolean
}

/** Emotional / AI / social experience preference toggles. */
interface ExperiencePreferences {
    var checkInEnabled: Boolean
    var coolingOffEnabled: Boolean
    var cloudInsightsEnabled: Boolean
    var viewAnonymization: Boolean
    var sharingLevel: SharingLevelPref
    var userInterests: List<String> // Tags used for analog suggestions
    var notificationPreferences: NotificationPreferences
}

/** Onboarding / account preference state. */
interface AccountPreferences {
    var onboardingCompleted: Boolean
    var seedDataLoaded: Boolean
    var userId: String?
    var deviceToken: String?
}

/**
 * Cross-platform preferences interface, composed of focused facets so no single
 * interface exceeds the Strict-Zero complexity ceiling.
 * Implemented per-platform via expect/actual.
 */
interface BilboPreferences :
    EnforcementPreferences,
    EconomyPreferences,
    ExperiencePreferences,
    AccountPreferences {
    /** Resets all preferences to their defaults. */
    fun clear()
}

// MARK: - expect factory

/**
 * Platform-provided factory for BilboPreferences.
 * Returns the correct implementation per platform.
 */
expect fun bilboPreferences(): BilboPreferences
