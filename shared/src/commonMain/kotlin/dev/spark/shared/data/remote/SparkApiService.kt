package dev.spark.shared.data.remote

import dev.spark.shared.domain.model.DailyInsight
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Wraps Supabase data-access calls for Bilbo features.
 *
 * All suspend functions must be called from a coroutine.
 */
class SparkApiService(private val client: SupabaseClient) {

    // -------------------------------------------------------------------------
    // Daily Insights
    // -------------------------------------------------------------------------

    /**
     * Fetches daily insights for the current user, ordered by date descending.
     *
     * @param limit Maximum number of records to return.
     */
    suspend fun getDailyInsights(limit: Int = 30): List<DailyInsight> =
        client.postgrest["daily_insights"]
            .select {
                order("date", Order.DESCENDING)
                this.limit(limit.toLong())
            }
            .decodeList()

    /**
     * Triggers the Tier-3 AI insight generation via the Supabase Edge Function
     * `ai-relay`, which securely proxies requests to the Anthropic API.
     *
     * @param date ISO-8601 date string (e.g. "2025-01-15") for which to generate insights.
     * @return The generated [DailyInsight].
     */
    suspend fun generateAiInsight(date: String): DailyInsight {
        val response = client.functions.invoke(
            function = "ai-relay",
            body = buildJsonObject {
                put("action", "generate_insight")
                put("date", date)
            }
        )
        val text = response.bodyAsText()
        return kotlinx.serialization.json.Json.decodeFromString(text)
    }
}
