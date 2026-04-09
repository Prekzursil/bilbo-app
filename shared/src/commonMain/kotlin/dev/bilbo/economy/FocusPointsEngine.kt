package dev.bilbo.economy

import dev.bilbo.domain.*
import kotlinx.datetime.*

/**
 * Core Focus Points (FP) engine.
 *
 * Manages all FP transactions: earning, spending, bonuses, and penalties.
 * The engine is stateless with respect to persistence — callers must supply
 * the current [DopamineBudget] and receive an updated copy.
 *
 * All mutations return a new [DopamineBudget] (immutable update pattern).
 */
class FocusPointsEngine {

    companion object {
        private val EARN_RATE = FPEconomy.EARN_PER_NUTRITIVE_MINUTE
        private val COST_RATE = FPEconomy.COST_PER_EMPTY_CALORIE_MINUTE
        private val DAILY_BASELINE = FPEconomy.DAILY_BASELINE
        private val EARN_CAP = FPEconomy.DAILY_EARN_CAP
    }

    // -------------------------------------------------------------------------
    // Core transactions
    // -------------------------------------------------------------------------

    /**
     * Awards FP for a completed Nutritive session.
     * Respects the daily earn cap — returns the updated budget and the actual FP awarded.
     *
     * @param budget Current budget for the day.
     * @param sessionMinutes Duration of the Nutritive session in whole minutes.
     * @return Pair of (updated budget, actual FP awarded after cap).
     */
    fun earnPoints(budget: DopamineBudget, sessionMinutes: Int): Pair<DopamineBudget, Int> {
        val rawEarn = sessionMinutes * EARN_RATE
        val alreadyEarned = budget.fpEarned
        val headroom = EARN_CAP - alreadyEarned
        val actualEarn = maxOf(0, minOf(rawEarn, headroom))
        return budget.copy(fpEarned = alreadyEarned + actualEarn) to actualEarn
    }

    /**
     * Deducts FP for Empty Calorie usage.
     * Balance can go negative (the app shows a deficit state, not a hard stop at 0).
     *
     * @param budget Current budget.
     * @param sessionMinutes Duration of empty-calorie usage in whole minutes.
     * @return Pair of (updated budget, FP deducted).
     */
    fun spendPoints(budget: DopamineBudget, sessionMinutes: Int): Pair<DopamineBudget, Int> {
        val cost = sessionMinutes * COST_RATE
        return budget.copy(fpSpent = budget.fpSpent + cost) to cost
    }

    /**
     * Applies a positive bonus to the budget (e.g. breathing exercise, analog activity).
     * Bonus FP do NOT count against the daily earn cap.
     *
     * @param budget Current budget.
     * @param bonusAmount FP to add.
     * @param reason Human-readable reason for the bonus (for logging).
     * @return Updated budget.
     */
    fun applyBonus(budget: DopamineBudget, bonusAmount: Int, reason: String = ""): DopamineBudget {
        return budget.copy(fpBonus = budget.fpBonus + bonusAmount)
    }

    /**
     * Applies a penalty (e.g. overriding a hard lock, ignoring a nudge).
     * Penalties are implemented as additional spend.
     *
     * @param budget Current budget.
     * @param penaltyAmount FP to deduct.
     * @param reason Human-readable reason (for logging).
     * @return Updated budget.
     */
    fun applyPenalty(budget: DopamineBudget, penaltyAmount: Int, reason: String = ""): DopamineBudget {
        return budget.copy(fpSpent = budget.fpSpent + penaltyAmount)
    }

    /**
     * Returns the current FP balance for the given budget.
     * Formula: baseline + earned + bonus + rolloverIn − spent
     */
    fun getBalance(budget: DopamineBudget): Int = budget.currentBalance()

    // -------------------------------------------------------------------------
    // Preset bonus helpers
    // -------------------------------------------------------------------------

    /** Awards the breathing exercise bonus. */
    fun awardBreathingBonus(budget: DopamineBudget): DopamineBudget =
        applyBonus(budget, FPEconomy.BONUS_BREATHING_EXERCISE, "Breathing exercise completed")

    /** Awards the analog activity accepted bonus. */
    fun awardAnalogBonus(budget: DopamineBudget): DopamineBudget =
        applyBonus(budget, FPEconomy.BONUS_ANALOG_ACCEPTED, "Analog suggestion accepted")

    /** Awards the accurate intent bonus. */
    fun awardAccurateIntentBonus(budget: DopamineBudget): DopamineBudget =
        applyBonus(budget, FPEconomy.BONUS_ACCURATE_INTENT, "Intent declared and respected")

    /** Awards the 7-day streak bonus. */
    fun awardStreakBonus(budget: DopamineBudget): DopamineBudget =
        applyBonus(budget, FPEconomy.STREAK_BONUS_7_DAY, "7-day streak achieved!")

    // -------------------------------------------------------------------------
    // Preset penalty helpers
    // -------------------------------------------------------------------------

    /** Applies the hard-lock override penalty. */
    fun penalizeHardLockOverride(budget: DopamineBudget): DopamineBudget =
        applyPenalty(budget, FPEconomy.PENALTY_HARD_LOCK_OVERRIDE, "Hard lock overridden")

    /** Applies the nudge-ignored penalty. */
    fun penalizeNudgeIgnored(budget: DopamineBudget): DopamineBudget =
        applyPenalty(budget, FPEconomy.PENALTY_NUDGE_IGNORE, "Nudge ignored")

    // -------------------------------------------------------------------------
    // Rollover calculation
    // -------------------------------------------------------------------------

    /**
     * Calculates how many FP roll over from [today] into tomorrow.
     * Rollover = 50% of remaining positive balance (if any).
     */
    fun calculateRollover(today: DopamineBudget): Int {
        val balance = getBalance(today)
        if (balance <= 0) return 0
        return (balance * FPEconomy.ROLLOVER_PERCENTAGE).toInt()
    }

    /**
     * Creates a fresh [DopamineBudget] for [date], carrying over rollover from [previousDay].
     */
    fun createDayBudget(date: LocalDate, previousDay: DopamineBudget?): DopamineBudget {
        val rollover = previousDay?.let { calculateRollover(it) } ?: 0
        return DopamineBudget(
            date = date,
            fpEarned = 0,
            fpSpent = 0,
            fpBonus = 0,
            fpRolloverIn = rollover,
            fpRolloverOut = previousDay?.let { calculateRollover(it) } ?: 0,
            nutritiveMinutes = 0,
            emptyCalorieMinutes = 0,
            neutralMinutes = 0
        )
    }

    /**
     * Returns a human-readable balance summary string.
     */
    fun describeBudget(budget: DopamineBudget): String {
        val balance = getBalance(budget)
        val earnedStr = "${budget.fpEarned}/${EARN_CAP} FP earned today"
        val balanceStr = "$balance FP available"
        return "$balanceStr · $earnedStr"
    }
}
