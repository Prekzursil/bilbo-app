package dev.bilbo.app.ui

// MARK: - Top-level route constants

object BilboRoute {
    // Top-level tabs
    const val DASHBOARD = "dashboard"
    const val FOCUS = "focus"
    const val INSIGHTS = "insights"
    const val SOCIAL = "social"
    const val SETTINGS = "settings"

    // Dashboard sub-routes
    const val BUDGET = "budget"

    // Insights sub-routes
    const val WEEKLY_INSIGHT = "insights/weekly/{weekStart}"
    const val ANALOG_SUGGESTIONS = "analog/suggestions"
    const val INTERESTS_ONBOARDING = "interests/setup"

    // Social sub-routes
    const val SOCIAL_HUB = "social/hub"
    const val BUDDY_PAIRS = "social/buddies"
    const val CIRCLES = "social/circles"
    const val CHALLENGES = "social/challenges"
    const val LEADERBOARD = "social/leaderboard"
    const val DIGEST = "social/digest"

    // Settings sub-routes
    const val SETTINGS_ENFORCEMENT = "settings/enforcement"
    const val SETTINGS_ECONOMY = "settings/economy"
    const val SETTINGS_EMOTIONAL = "settings/emotional"
    const val SETTINGS_AI = "settings/ai"
    const val SETTINGS_SOCIAL = "settings/social"
    const val SETTINGS_NOTIFICATIONS = "settings/notifications"
    const val SETTINGS_DATA = "settings/data"
    const val DATA_ANONYMIZATION = "settings/data/anonymization"

    // Onboarding (full-screen, no bottom bar)
    const val ONBOARDING = "onboarding"
}
