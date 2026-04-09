package dev.bilbo.domain

data class AppProfile(
    val packageName: String,
    val appLabel: String,
    val category: AppCategory,
    val enforcementMode: EnforcementMode,
    val coolingOffEnabled: Boolean = false,
    val isBypassed: Boolean = false,
    val isCustomClassification: Boolean = false
)

enum class AppCategory {
    NUTRITIVE, NEUTRAL, EMPTY_CALORIES
}

enum class EnforcementMode {
    NUDGE, HARD_LOCK
}
