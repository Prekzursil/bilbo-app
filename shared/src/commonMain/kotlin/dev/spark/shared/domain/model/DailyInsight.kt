package dev.spark.shared.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An AI-generated or heuristic insight summarising the user's
 * digital wellness activity for a given day.
 *
 * @property tier Which AI tier produced this insight (1 = local heuristic,
 *   2 = on-device ML, 3 = Anthropic API via Edge Function).
 */
@Serializable
data class DailyInsight(
    val id: String,
    @SerialName("user_id") val userId: String,
    val date: String,
    val summary: String,
    val highlights: List<String>,
    val suggestions: List<String>,
    @SerialName("total_screen_time_minutes") val totalScreenTimeMinutes: Int,
    @SerialName("top_apps") val topApps: List<AppUsageSummary>,
    val tier: Int,
    val mood: MoodScore?,
)

@Serializable
data class AppUsageSummary(
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    val category: AppCategory,
)

@Serializable
data class MoodScore(
    val score: Int,  // 1-10
    val label: String,
)
