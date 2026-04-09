package dev.spark.coverage

import dev.spark.preferences.*
import dev.spark.shared.domain.model.*
import dev.spark.shared.domain.model.AppCategory as SharedAppCategory
import dev.spark.shared.util.*
import dev.spark.util.*
import kotlinx.datetime.*
import kotlinx.serialization.json.Json
import kotlin.test.*

// =============================================================================
//  NotificationPreferences serialization & coverage
// =============================================================================

class NotificationPreferencesSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun defaultValues() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.nudgeEnabled)
        assertTrue(prefs.weeklyInsightEnabled)
        assertTrue(prefs.challengeUpdateEnabled)
        assertTrue(prefs.quietHoursEnabled)
        assertEquals(22, prefs.quietStartHour)
        assertEquals(0, prefs.quietStartMinute)
        assertEquals(8, prefs.quietEndHour)
        assertEquals(0, prefs.quietEndMinute)
    }

    @Test fun serializationRoundTrip() {
        val prefs = NotificationPreferences(
            nudgeEnabled = false,
            weeklyInsightEnabled = false,
            challengeUpdateEnabled = false,
            quietHoursEnabled = false,
            quietStartHour = 20,
            quietStartMinute = 30,
            quietEndHour = 7,
            quietEndMinute = 15
        )
        val str = json.encodeToString(NotificationPreferences.serializer(), prefs)
        val decoded = json.decodeFromString(NotificationPreferences.serializer(), str)
        assertEquals(prefs, decoded)
    }

    @Test fun copyChangesFields() {
        val prefs = NotificationPreferences()
        val modified = prefs.copy(nudgeEnabled = false, quietStartHour = 21)
        assertFalse(modified.nudgeEnabled)
        assertEquals(21, modified.quietStartHour)
    }

    @Test fun equalityAndHashCode() {
        val p1 = NotificationPreferences(nudgeEnabled = true)
        val p2 = NotificationPreferences(nudgeEnabled = true)
        assertEquals(p1, p2)
        assertEquals(p1.hashCode(), p2.hashCode())
    }

    @Test fun inequalityOnDifferentField() {
        val p1 = NotificationPreferences(nudgeEnabled = true)
        val p2 = NotificationPreferences(nudgeEnabled = false)
        assertNotEquals(p1, p2)
    }

    @Test fun allFieldsAffectEquality() {
        val base = NotificationPreferences()
        assertNotEquals(base, base.copy(weeklyInsightEnabled = false))
        assertNotEquals(base, base.copy(challengeUpdateEnabled = false))
        assertNotEquals(base, base.copy(quietHoursEnabled = false))
        assertNotEquals(base, base.copy(quietStartHour = 0))
        assertNotEquals(base, base.copy(quietStartMinute = 1))
        assertNotEquals(base, base.copy(quietEndHour = 0))
        assertNotEquals(base, base.copy(quietEndMinute = 1))
    }
}

// =============================================================================
//  Shared domain model equals/hashCode branch coverage
// =============================================================================

class SharedDomainModelEqualityTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test fun appUsageSessionEquality() {
        val s1 = AppUsageSession(
            id = "s1", userId = "u1", packageName = "com.test", appName = "Test",
            startTime = Instant.fromEpochSeconds(1000), endTime = Instant.fromEpochSeconds(2000),
            durationMs = 1000000, category = SharedAppCategory.SOCIAL
        )
        val s2 = s1.copy()
        assertEquals(s1, s2)
        assertEquals(s1.hashCode(), s2.hashCode())
        assertNotEquals(s1, s1.copy(id = "s2"))
        assertFalse(s1.equals(null))
        assertFalse(s1.equals("string"))
    }

    @Test fun appUsageSummaryEquality() {
        val a1 = AppUsageSummary("com.test", "Test", 30, SharedAppCategory.ENTERTAINMENT)
        val a2 = a1.copy()
        assertEquals(a1, a2)
        assertEquals(a1.hashCode(), a2.hashCode())
        assertNotEquals(a1, a1.copy(packageName = "com.other"))
        assertFalse(a1.equals(null))
        assertFalse(a1.equals("string"))
    }

    @Test fun dailyInsightEquality() {
        val d1 = DailyInsight(
            id = "i1", userId = "u1", date = "2025-01-15", summary = "Good",
            highlights = listOf("h1"), suggestions = listOf("s1"),
            totalScreenTimeMinutes = 120,
            topApps = listOf(AppUsageSummary("com.test", "Test", 60, SharedAppCategory.SOCIAL)),
            tier = 1, mood = MoodScore(8, "happy")
        )
        val d2 = d1.copy()
        assertEquals(d1, d2)
        assertEquals(d1.hashCode(), d2.hashCode())
        assertNotEquals(d1, d1.copy(id = "i2"))
        assertFalse(d1.equals(null))
        assertFalse(d1.equals("string"))
    }

    @Test fun moodScoreEquality() {
        val m1 = MoodScore(8, "happy")
        val m2 = MoodScore(8, "happy")
        assertEquals(m1, m2)
        assertEquals(m1.hashCode(), m2.hashCode())
        assertNotEquals(m1, MoodScore(7, "happy"))
        assertNotEquals(m1, MoodScore(8, "sad"))
        assertFalse(m1.equals(null))
        assertFalse(m1.equals("string"))
    }

    @Test fun wellnessGoalEquality() {
        val g1 = WellnessGoal(
            id = "g1", userId = "u1", name = "Reduce", description = "Less",
            type = GoalType.SCREEN_TIME_LIMIT, targetApps = listOf("com.test"),
            dailyLimitMinutes = 60, isActive = true, createdAt = "2025-01-01"
        )
        val g2 = g1.copy()
        assertEquals(g1, g2)
        assertEquals(g1.hashCode(), g2.hashCode())
        assertNotEquals(g1, g1.copy(id = "g2"))
        assertFalse(g1.equals(null))
        assertFalse(g1.equals("string"))
    }
}

// =============================================================================
//  Result<T> additional branch coverage
// =============================================================================

class ResultBranchCoverageTest {

    @Test fun getOrNullOnLoading() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Loading
        assertNull(result.getOrNull())
    }

    @Test fun getOrNullOnError() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Error(RuntimeException("fail"))
        assertNull(result.getOrNull())
    }

    @Test fun getOrNullOnSuccess() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Success("ok")
        assertEquals("ok", result.getOrNull())
    }

    @Test fun getOrThrowOnLoading() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Loading
        val ex = assertFailsWith<IllegalStateException> {
            result.getOrThrow()
        }
        assertTrue(ex.message!!.contains("Loading"))
    }

    @Test fun getOrThrowOnError() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Error(RuntimeException("test error"))
        assertFailsWith<RuntimeException> {
            result.getOrThrow()
        }
    }

    @Test fun getOrThrowOnSuccess() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Success("data")
        assertEquals("data", result.getOrThrow())
    }

    @Test fun mapOnSuccess() {
        val result: dev.spark.shared.util.Result<Int> = dev.spark.shared.util.Result.Success(42)
        val mapped = result.map { it * 2 }
        assertEquals(84, (mapped as dev.spark.shared.util.Result.Success).data)
    }

    @Test fun mapOnError() {
        val ex = RuntimeException("fail")
        val result: dev.spark.shared.util.Result<Int> = dev.spark.shared.util.Result.Error(ex)
        val mapped = result.map { it * 2 }
        assertTrue(mapped is dev.spark.shared.util.Result.Error)
    }

    @Test fun mapOnLoading() {
        val result: dev.spark.shared.util.Result<Int> = dev.spark.shared.util.Result.Loading
        val mapped = result.map { it * 2 }
        assertTrue(mapped is dev.spark.shared.util.Result.Loading)
    }

    @Test fun mapOnSuccessTransformation() {
        val result: dev.spark.shared.util.Result<String> = dev.spark.shared.util.Result.Success("hello")
        val mapped = result.map { it.length }
        val success = mapped as dev.spark.shared.util.Result.Success
        assertEquals(5, success.data)
    }
}

// =============================================================================
//  ErrorHandler additional branch coverage
// =============================================================================

class ErrorHandlerBranchCoverageTest {

    @Test fun mapSerializationException() {
        val handler = DefaultErrorHandler()
        val error = handler.map(kotlinx.serialization.SerializationException("bad"))
        assertTrue(error is BilboError.DataError)
    }

    @Test fun mapBilboErrorPassesThrough() {
        val handler = DefaultErrorHandler()
        val original = BilboError.Offline()
        val mapped = handler.map(original)
        assertSame(original, mapped)
    }

    @Test fun mapNetworkException401() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(401, "Unauthorized"))
        assertTrue(error is BilboError.Unauthorized)
    }

    @Test fun mapNetworkException403() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(403, "Forbidden"))
        assertTrue(error is BilboError.Unauthorized)
    }

    @Test fun mapNetworkException404() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(404, "Not Found"))
        assertTrue(error is BilboError.NotFound)
    }

    @Test fun mapNetworkException4xx() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(422, "Unprocessable"))
        assertTrue(error is BilboError.ClientError)
        assertEquals(422, (error as BilboError.ClientError).statusCode)
    }

    @Test fun mapNetworkException5xx() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(503, "Unavailable"))
        assertTrue(error is BilboError.ServerError)
    }

    @Test fun mapNetworkExceptionOther() {
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(302, "Redirect"))
        assertTrue(error is BilboError.Unknown)
    }

    @Test fun mapUnknownException() {
        val handler = DefaultErrorHandler()
        val error = handler.map(RuntimeException("oops"))
        assertTrue(error is BilboError.Unknown)
    }

    @Test fun toUserMessageOnBilboError() {
        val error = BilboError.Offline()
        val msg = error.toUserMessage()
        assertTrue(msg.contains("offline"))
    }

    @Test fun toUserMessageOnGenericException() {
        val error = RuntimeException("oops")
        val msg = error.toUserMessage()
        assertNotNull(msg)
    }

    @Test fun toUserMessageOnOfflineException() {
        val error = OfflineException("no network")
        val msg = error.toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnNetworkException() {
        val error = NetworkException(500, "server error")
        val msg = error.toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnBilboErrorWithNullMessage() {
        val error = BilboError.Unknown(cause = null, message = "Something went wrong. Please try again.")
        val msg = error.toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun mapNetworkExceptionWithNullMessage() {
        // Test the null message branch in ClientError mapping
        val handler = DefaultErrorHandler()
        val error = handler.map(NetworkException(400, "Bad Request"))
        assertTrue(error is BilboError.ClientError)
        assertEquals("Bad Request", (error as BilboError.ClientError).message)
    }

    @Test fun toUserMessageOnBilboDataError() {
        val msg = BilboError.DataError(cause = RuntimeException("test")).toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnBilboServerError() {
        val msg = BilboError.ServerError(500).toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnBilboClientError() {
        val msg = BilboError.ClientError(400, "fail").toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnBilboUnauthorized() {
        val msg = BilboError.Unauthorized().toUserMessage()
        assertTrue(msg.isNotBlank())
    }

    @Test fun toUserMessageOnBilboNotFound() {
        val msg = BilboError.NotFound().toUserMessage()
        assertTrue(msg.isNotBlank())
    }
}
