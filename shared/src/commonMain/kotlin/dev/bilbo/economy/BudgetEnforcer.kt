package dev.bilbo.economy

import dev.bilbo.domain.*
import kotlin.time.Clock
import kotlinx.datetime.*

/**
 * Handles daily budget lifecycle: reset, rollover calculation, and enforcement checks.
 *
 * The [BudgetEnforcer] works alongside [FocusPointsEngine] — the FP engine handles
 * point arithmetic while [BudgetEnforcer] manages the day boundary logic and
 * enforcement gates.
 */
class BudgetEnforcer(
    private val fpEngine: FocusPointsEngine = FocusPointsEngine(),
    private val clock: Clock = Clock.System
) {

    sealed class EnforcementGate {
        /** App access is fully permitted. */
        data object Permitted : EnforcementGate()
        /** Balance is negative — user should be warned but not hard-blocked (NUDGE mode). */
        data class LowBalance(val balance: Int) : EnforcementGate()
        /** Balance is depleted and the app is in HARD_LOCK mode — access denied. */
        data class HardBlocked(val balance: Int) : EnforcementGate()
        /** Budget for today hasn't been created yet — create it first. */
        data object NoBudgetForToday : EnforcementGate()
    }

    // -------------------------------------------------------------------------
    // Daily reset & rollover
    // -------------------------------------------------------------------------

    /**
     * Resets the budget for a new calendar day, carrying over rollover FP.
     * Should be called at midnight (or on first launch of a new day).
     *
     * @param previousBudget Yesterday's finalized budget.
     * @param timeZone Used to determine today's date.
     * @return A fresh [DopamineBudget] for today.
     */
    fun resetForNewDay(
        previousBudget: DopamineBudget?,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): DopamineBudget {
        val today = clock.now().toLocalDateTime(timeZone).date
        return fpEngine.createDayBudget(today, previousBudget)
    }

    /**
     * Returns true if [budget] belongs to today's calendar day.
     */
    fun isTodayBudget(
        budget: DopamineBudget,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Boolean {
        val today = clock.now().toLocalDateTime(timeZone).date
        return budget.date == today
    }

    /**
     * Ensures we have a valid budget for today. If [existingBudget] is from a prior
     * day (or null), creates a new one with appropriate rollover.
     *
     * @param existingBudget The last stored budget, or null if none exists.
     * @return A valid [DopamineBudget] for today.
     */
    fun ensureTodayBudget(
        existingBudget: DopamineBudget?,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): DopamineBudget {
        if (existingBudget != null && isTodayBudget(existingBudget, timeZone)) {
            return existingBudget
        }
        return resetForNewDay(existingBudget, timeZone)
    }

    // -------------------------------------------------------------------------
    // Enforcement gate
    // -------------------------------------------------------------------------

    /**
     * Evaluates whether an Empty Calorie app should be gated, given the current [budget]
     * and the app's [enforcementMode].
     *
     * Returns an [EnforcementGate] indicating whether access is permitted, warned, or blocked.
     */
    fun evaluateGate(
        budget: DopamineBudget,
        enforcementMode: EnforcementMode,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): EnforcementGate {
        if (!isTodayBudget(budget, timeZone)) return EnforcementGate.NoBudgetForToday

        val balance = fpEngine.getBalance(budget)
        return when {
            balance > 0 -> EnforcementGate.Permitted
            enforcementMode == EnforcementMode.HARD_LOCK ->
                EnforcementGate.HardBlocked(balance)
            else ->
                EnforcementGate.LowBalance(balance)
        }
    }

    // -------------------------------------------------------------------------
    // Rollover
    // -------------------------------------------------------------------------

    /**
     * Computes the rollover amount for [today] (50% of positive balance).
     */
    fun computeRollover(today: DopamineBudget): Int = fpEngine.calculateRollover(today)

    /**
     * Finalizes [today]'s budget by setting [DopamineBudget.fpRolloverOut] to the
     * calculated rollover. Call this at end-of-day before persisting.
     */
    fun finalizeDayBudget(today: DopamineBudget): DopamineBudget {
        val rollover = computeRollover(today)
        return today.copy(fpRolloverOut = rollover)
    }

    // -------------------------------------------------------------------------
    // Summary helpers
    // -------------------------------------------------------------------------

    /**
     * Returns a structured summary of the day's FP activity.
     */
    fun getDailySummary(budget: DopamineBudget): DailySummary {
        val balance = fpEngine.getBalance(budget)
        val earnedToday = budget.fpEarned
        val capRemaining = FPEconomy.DAILY_EARN_CAP - earnedToday
        val rolloverIn = budget.fpRolloverIn
        val rolloverOut = computeRollover(budget)

        return DailySummary(
            date = budget.date,
            balance = balance,
            fpEarned = earnedToday,
            fpSpent = budget.fpSpent,
            fpBonus = budget.fpBonus,
            fpRolloverIn = rolloverIn,
            projectedRolloverOut = rolloverOut,
            earnCapRemaining = maxOf(0, capRemaining),
            isEarnCapHit = earnedToday >= FPEconomy.DAILY_EARN_CAP,
            nutritiveMinutes = budget.nutritiveMinutes,
            emptyCalorieMinutes = budget.emptyCalorieMinutes
        )
    }

    data class DailySummary(
        val date: LocalDate,
        val balance: Int,
        val fpEarned: Int,
        val fpSpent: Int,
        val fpBonus: Int,
        val fpRolloverIn: Int,
        val projectedRolloverOut: Int,
        val earnCapRemaining: Int,
        val isEarnCapHit: Boolean,
        val nutritiveMinutes: Int,
        val emptyCalorieMinutes: Int
    ) {
        /** Percentage of the daily earn cap achieved (0–100). */
        val earnCapPercent: Int get() =
            ((fpEarned.toFloat() / FPEconomy.DAILY_EARN_CAP) * 100).toInt().coerceIn(0, 100)
    }

    // -------------------------------------------------------------------------
    // Multi-day history helpers
    // -------------------------------------------------------------------------

    /**
     * Given a list of daily budgets ordered oldest-to-newest, fills in any missing days
     * with zero-usage budgets and returns the complete list. Useful for chart rendering.
     */
    fun fillMissingDays(
        budgets: List<DopamineBudget>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<DopamineBudget> {
        if (budgets.isEmpty()) return emptyList()
        val byDate = budgets.associateBy { it.date }
        val result = mutableListOf<DopamineBudget>()
        var current = fromDate
        while (current <= toDate) {
            result += byDate[current] ?: DopamineBudget(
                date = current,
                fpEarned = 0,
                fpSpent = 0,
                fpBonus = 0,
                fpRolloverIn = 0,
                fpRolloverOut = 0,
                nutritiveMinutes = 0,
                emptyCalorieMinutes = 0,
                neutralMinutes = 0
            )
            current = current.plus(1, DateTimeUnit.DAY)
        }
        return result
    }
}
