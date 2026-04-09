package dev.spark.shared.domain.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a recorded screen-time session for a single app.
 *
 * @property id Unique identifier for this session (UUID string).
 * @property userId The authenticated user's ID from Supabase Auth.
 * @property packageName Android package name or iOS bundle ID of the app.
 * @property appName Human-readable display name of the app.
 * @property startTime When the session began.
 * @property endTime When the session ended (null if still active).
 * @property durationMs Total session duration in milliseconds.
 * @property category App category (social, entertainment, productivity, etc.).
 */
@Serializable
data class AppUsageSession(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("package_name") val packageName: String,
    @SerialName("app_name") val appName: String,
    @SerialName("start_time") val startTime: Instant,
    @SerialName("end_time") val endTime: Instant?,
    @SerialName("duration_ms") val durationMs: Long,
    val category: AppCategory,
)

@Serializable
enum class AppCategory {
    @SerialName("social") SOCIAL,
    @SerialName("entertainment") ENTERTAINMENT,
    @SerialName("productivity") PRODUCTIVITY,
    @SerialName("games") GAMES,
    @SerialName("health") HEALTH,
    @SerialName("news") NEWS,
    @SerialName("other") OTHER,
}
