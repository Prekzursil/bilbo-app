package dev.bilbo.intelligence.tier1

import dev.bilbo.domain.*

sealed class LaunchDecision {
    data object Allow : LaunchDecision()
    data class Block(val remainingMinutes: Int) : LaunchDecision()
    data class InsufficientFP(val balance: Int) : LaunchDecision()
    data object RequiresIntent : LaunchDecision()
}

sealed class EnforcementAction {
    data object ShowNudge : EnforcementAction()
    data class HardLock(val cooldownMinutes: Int = 30) : EnforcementAction()
}

class RuleEngine(
    private val appProfileProvider: (String) -> AppProfile?,
    private val budgetProvider: () -> DopamineBudget,
    private val cooldownChecker: (String) -> Int? // returns remaining minutes or null
) {
    fun evaluateAppLaunch(packageName: String): LaunchDecision {
        val profile = appProfileProvider(packageName) ?: return LaunchDecision.RequiresIntent

        if (profile.isBypassed) return LaunchDecision.Allow

        val cooldown = cooldownChecker(packageName)
        if (cooldown != null) return LaunchDecision.Block(cooldown)

        if (profile.category == AppCategory.EMPTY_CALORIES) {
            val budget = budgetProvider()
            val balance = budget.currentBalance()
            if (balance <= 0) return LaunchDecision.InsufficientFP(balance)
        }

        return LaunchDecision.RequiresIntent
    }

    fun evaluateTimerExpiry(profile: AppProfile): EnforcementAction {
        return when (profile.enforcementMode) {
            EnforcementMode.NUDGE -> EnforcementAction.ShowNudge
            EnforcementMode.HARD_LOCK -> EnforcementAction.HardLock(cooldownMinutes = 30)
        }
    }
}
