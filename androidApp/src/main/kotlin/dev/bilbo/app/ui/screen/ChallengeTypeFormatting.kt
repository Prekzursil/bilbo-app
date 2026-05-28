package dev.bilbo.app.ui.screen

import androidx.compose.ui.graphics.Color
import dev.bilbo.social.ChallengeEngine

// ── Per-type display formatting + accent colours ──────────────────────────────
private const val ARGB_TYPE_REDUCE = 0xFFF44336
private const val ARGB_TYPE_EARN = 0xFF4CAF50
private const val ARGB_TYPE_FP = 0xFFFF9800
private const val ARGB_TYPE_STREAK = 0xFF9C27B0
private const val ARGB_TYPE_POOL = 0xFF2196F3
private const val ARGB_TYPE_ANALOG = 0xFF009688

internal fun ChallengeEngine.ChallengeType.displayName(): String =
    when (this) {
        ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES -> "Reduce Scrolling"
        ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "Earn Nutritive Time"
        ChallengeEngine.ChallengeType.REACH_FP_BALANCE -> "Reach FP Balance"
        ChallengeEngine.ChallengeType.DAILY_STREAK -> "Daily Streak"
        ChallengeEngine.ChallengeType.GROUP_FP_POOL -> "Group FP Pool"
        ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS -> "Analog Activities"
    }

internal fun ChallengeEngine.ChallengeType.description(): String =
    when (this) {
        ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES -> "Reduce empty-calorie minutes below a target"
        ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "Accumulate nutritive screen time"
        ChallengeEngine.ChallengeType.REACH_FP_BALANCE -> "Reach a Focus Points balance"
        ChallengeEngine.ChallengeType.DAILY_STREAK -> "Maintain a consecutive-day streak"
        ChallengeEngine.ChallengeType.GROUP_FP_POOL -> "Collectively earn Focus Points"
        ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS -> "Complete analog activity suggestions"
    }

internal fun ChallengeEngine.ChallengeType.unit(): String =
    when (this) {
        ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES -> "max minutes"
        ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> "minutes"
        ChallengeEngine.ChallengeType.REACH_FP_BALANCE -> "FP"
        ChallengeEngine.ChallengeType.DAILY_STREAK -> "days"
        ChallengeEngine.ChallengeType.GROUP_FP_POOL -> "FP total"
        ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS -> "activities"
    }

internal fun challengeTypeColor(type: ChallengeEngine.ChallengeType): Color =
    when (type) {
        ChallengeEngine.ChallengeType.REDUCE_EMPTY_CALORIES -> Color(ARGB_TYPE_REDUCE)
        ChallengeEngine.ChallengeType.EARN_NUTRITIVE_MINUTES -> Color(ARGB_TYPE_EARN)
        ChallengeEngine.ChallengeType.REACH_FP_BALANCE -> Color(ARGB_TYPE_FP)
        ChallengeEngine.ChallengeType.DAILY_STREAK -> Color(ARGB_TYPE_STREAK)
        ChallengeEngine.ChallengeType.GROUP_FP_POOL -> Color(ARGB_TYPE_POOL)
        ChallengeEngine.ChallengeType.ANALOG_COMPLETIONS -> Color(ARGB_TYPE_ANALOG)
    }
