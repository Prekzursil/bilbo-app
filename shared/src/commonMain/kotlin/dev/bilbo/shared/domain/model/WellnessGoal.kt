package dev.bilbo.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user-defined digital wellness goal, such as limiting
 * social media to 30 minutes per day.
 */
@Serializable
data class WellnessGoal(
    val id: String,
    @SerialName("user_id") val userId: String,
    val name: String,
    val description: String,
    val type: GoalType,
    @SerialName("target_apps") val targetApps: List<String>,
    @SerialName("daily_limit_minutes") val dailyLimitMinutes: Int,
    @SerialName("is_active") val isActive: Boolean,
    @SerialName("created_at") val createdAt: String,
)

@Serializable
enum class GoalType {
    @SerialName("screen_time_limit") SCREEN_TIME_LIMIT,
    @SerialName("app_block") APP_BLOCK,
    @SerialName("focus_session") FOCUS_SESSION,
    @SerialName("bedtime") BEDTIME,
}
