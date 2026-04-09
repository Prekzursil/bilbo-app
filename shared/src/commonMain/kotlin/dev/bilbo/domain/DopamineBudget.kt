package dev.bilbo.domain

import kotlinx.datetime.LocalDate

data class DopamineBudget(
    val date: LocalDate,
    val fpEarned: Int,
    val fpSpent: Int,
    val fpBonus: Int,
    val fpRolloverIn: Int,
    val fpRolloverOut: Int,
    val nutritiveMinutes: Int,
    val emptyCalorieMinutes: Int,
    val neutralMinutes: Int
) {
    fun currentBalance(): Int = FPEconomy.DAILY_BASELINE + fpEarned + fpBonus + fpRolloverIn - fpSpent
}

object FPEconomy {
    const val EARN_PER_NUTRITIVE_MINUTE = 1
    const val COST_PER_EMPTY_CALORIE_MINUTE = 1
    const val BONUS_BREATHING_EXERCISE = 3
    const val BONUS_ANALOG_ACCEPTED = 5
    const val BONUS_ACCURATE_INTENT = 2
    const val PENALTY_HARD_LOCK_OVERRIDE = 10
    const val PENALTY_NUDGE_IGNORE = 3
    const val DAILY_EARN_CAP = 60
    const val DAILY_BASELINE = 15
    const val ROLLOVER_PERCENTAGE = 0.5
    const val MIN_SESSION_SECONDS = 60
    const val STREAK_BONUS_7_DAY = 20
}
