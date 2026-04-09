package dev.spark.intelligence.tier3

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.datetime.*

/**
 * Ktor client that sends anonymized weekly summaries to a Supabase Edge Function
 * and returns a narrative insight string.
 *
 * Rate limited to one request per user per week. Requests that fall within the
 * cool-down window are rejected locally without making a network call.
 */
class CloudInsightClient(
    private val httpClient: HttpClient,
    private val supabaseUrl: String,       // e.g. "https://xyz.supabase.co"
    private val supabaseAnonKey: String,
    private val promptBuilder: InsightPromptBuilder = InsightPromptBuilder(),
    private val clock: Clock = Clock.System
) {

    companion object {
        private const val EDGE_FUNCTION_PATH = "/functions/v1/weekly-insight"
        private const val RATE_LIMIT_DAYS = 7L

        // Returned when the call is rate-limited locally
        const val RATE_LIMITED_SENTINEL = "__RATE_LIMITED__"
    }

    sealed class InsightResult {
        data class Success(val narrative: String) : InsightResult()
        data object RateLimited : InsightResult()
        data class NetworkError(val message: String) : InsightResult()
        data class ServerError(val statusCode: Int, val body: String) : InsightResult()
    }

    // In-memory rate-limit state. In production, persist this via a DataStore/SQLDelight store.
    private var lastRequestInstant: Instant? = null

    /**
     * Returns true if the client is allowed to make a new cloud request.
     */
    fun canRequest(): Boolean {
        val last = lastRequestInstant ?: return true
        val now = clock.now()
        val daysSinceLast = now.minus(last).inWholeDays
        return daysSinceLast >= RATE_LIMIT_DAYS
    }

    /**
     * Sends [jsonPayload] to the Supabase Edge Function and returns the resulting narrative.
     *
     * @param jsonPayload The serialized JSON produced by [InsightPromptBuilder.toJson].
     * @param userId Anonymized user ID (e.g. a UUID hash) included for server-side deduplication.
     */
    suspend fun fetchNarrative(
        jsonPayload: String,
        userId: String
    ): InsightResult {
        if (!canRequest()) return InsightResult.RateLimited

        val url = "$supabaseUrl$EDGE_FUNCTION_PATH"

        return try {
            val response: HttpResponse = httpClient.post(url) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $supabaseAnonKey")
                    append(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                    append("X-User-Id", userId)
                }
                setBody(jsonPayload)
            }

            when (response.status.value) {
                200 -> {
                    val body = response.body<String>()
                    val narrative = parseNarrativeFromResponse(body)
                    lastRequestInstant = clock.now()
                    InsightResult.Success(narrative)
                }
                429 -> InsightResult.RateLimited
                else -> InsightResult.ServerError(
                    statusCode = response.status.value,
                    body = response.body()
                )
            }
        } catch (e: Exception) {
            InsightResult.NetworkError(e.message ?: "Unknown network error")
        }
    }

    /**
     * Convenience method: builds the payload and sends it in one call.
     */
    suspend fun fetchNarrativeForWeek(
        weekSummary: InsightPromptBuilder.WeeklySummaryPayload,
        userId: String
    ): InsightResult {
        val json = promptBuilder.toJson(weekSummary)
        return fetchNarrative(json, userId)
    }

    /**
     * Returns the number of days remaining until the next request is allowed,
     * or 0 if the client can request now.
     */
    fun daysUntilNextRequest(): Long {
        val last = lastRequestInstant ?: return 0L
        val elapsed = clock.now().minus(last).inWholeDays
        return maxOf(0L, RATE_LIMIT_DAYS - elapsed)
    }

    /**
     * Overrides the stored last-request timestamp (used for testing / state restoration).
     */
    fun setLastRequestInstant(instant: Instant) {
        lastRequestInstant = instant
    }

    // ---------------------------------------------------------------------------
    // Response parsing
    // ---------------------------------------------------------------------------

    /**
     * Extracts the narrative string from the Edge Function JSON response.
     * Expected format: { "narrative": "..." }
     */
    private fun parseNarrativeFromResponse(json: String): String {
        // Simple extraction without a full JSON library dependency at this layer
        val key = "\"narrative\""
        val keyIndex = json.indexOf(key)
        if (keyIndex == -1) return json.trim()

        val colonIndex = json.indexOf(':', keyIndex + key.length)
        if (colonIndex == -1) return json.trim()

        val valueStart = json.indexOf('"', colonIndex + 1)
        if (valueStart == -1) return json.trim()

        val valueEnd = findClosingQuote(json, valueStart + 1)
        if (valueEnd == -1) return json.trim()

        return json.substring(valueStart + 1, valueEnd)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun findClosingQuote(str: String, startIndex: Int): Int {
        var i = startIndex
        while (i < str.length) {
            when {
                str[i] == '\\' -> i += 2  // skip escape sequence
                str[i] == '"'  -> return i
                else           -> i++
            }
        }
        return -1
    }
}
