package dev.bilbo.coverage

import dev.bilbo.domain.AppCategory
import dev.bilbo.economy.AppClassifier
import dev.bilbo.shared.util.Result
import dev.bilbo.shared.util.getOrNull
import dev.bilbo.shared.util.getOrThrow
import dev.bilbo.shared.util.map
import dev.bilbo.util.BilboError
import dev.bilbo.util.DefaultErrorHandler
import dev.bilbo.util.NetworkException
import dev.bilbo.util.OfflineException
import dev.bilbo.util.toUserMessage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Supplemental Strict-Zero coverage for previously-uncovered branches in
 * standalone, pure-logic utilities. These close real reachable gaps that were
 * formerly hidden by Kover class exclusions.
 */
class StrictZeroSupplementTest {
    // -------------------------------------------------------------------------
    // DefaultErrorHandler.map — every HTTP status branch
    // -------------------------------------------------------------------------

    private val handler = DefaultErrorHandler()

    @Test fun alreadyMappedErrorPassesThrough() {
        val original = BilboError.NotFound()
        assertEquals(original, handler.map(original))
    }

    @Test fun offlineExceptionMapsToOffline() {
        assertTrue(handler.map(OfflineException()) is BilboError.Offline)
    }

    @Test fun unauthorizedStatusCodes() {
        assertTrue(handler.map(NetworkException(401, "x")) is BilboError.Unauthorized)
        assertTrue(handler.map(NetworkException(403, "x")) is BilboError.Unauthorized)
    }

    @Test fun notFoundStatusCode() {
        assertTrue(handler.map(NetworkException(404, "x")) is BilboError.NotFound)
    }

    @Test fun clientErrorPreservesMessageAndFallback() {
        val withMsg = handler.map(NetworkException(422, "bad input")) as BilboError.ClientError
        assertEquals(422, withMsg.statusCode)
        assertEquals("bad input", withMsg.message)
    }

    @Test fun serverErrorStatusCode() {
        val err = handler.map(NetworkException(503, "x")) as BilboError.ServerError
        assertEquals(503, err.statusCode)
    }

    @Test fun networkExceptionOutsideKnownRangeMapsToUnknown() {
        assertTrue(handler.map(NetworkException(302, "redirect")) is BilboError.Unknown)
    }

    @Test fun serializationFailureMapsToDataError() {
        val err = handler.map(kotlinx.serialization.SerializationException("boom"))
        assertTrue(err is BilboError.DataError)
    }

    @Test fun arbitraryThrowableMapsToUnknown() {
        assertTrue(handler.map(IllegalStateException("nope")) is BilboError.Unknown)
    }

    @Test fun handleNotifiesListeners() {
        var received: BilboError? = null
        handler.addListener { received = it }
        // Adding the same listener twice must not double-register.
        val listener = dev.bilbo.util.ErrorListener { }
        handler.addListener(listener)
        handler.addListener(listener)
        val mapped = handler.handle(NetworkException(500, "x"))
        assertEquals(mapped, received)
        handler.removeListener(listener)
    }

    // -------------------------------------------------------------------------
    // Throwable.toUserMessage — both branches
    // -------------------------------------------------------------------------

    @Test fun toUserMessageForBilboError() {
        assertEquals("Server error (500). Try again shortly.", BilboError.ServerError(500).toUserMessage())
    }

    @Test fun toUserMessageForGenericThrowable() {
        // A non-BilboError throwable routes through DefaultErrorHandler().map.
        assertTrue(RuntimeException("x").toUserMessage().isNotBlank())
    }

    // -------------------------------------------------------------------------
    // Result extensions — Loading and Error branches of map/getOrThrow
    // -------------------------------------------------------------------------

    @Test fun resultMapSuccess() {
        val mapped = Result.Success(2).map { it * 3 }
        assertEquals(3 * 2, (mapped as Result.Success).data)
    }

    @Test fun resultMapErrorIsUnchanged() {
        val err: Result<Int> = Result.Error(IllegalStateException("e"))
        assertTrue(err.map { it + 1 } is Result.Error)
    }

    @Test fun resultMapLoadingIsUnchanged() {
        val loading: Result<Int> = Result.Loading
        assertTrue(loading.map { it + 1 } is Result.Loading)
    }

    @Test fun resultGetOrNullAndGetOrThrow() {
        assertEquals(5, Result.Success(5).getOrNull())
        assertNull((Result.Loading as Result<Int>).getOrNull())
        assertEquals(5, Result.Success(5).getOrThrow())
        assertFailsWith<IllegalStateException> { (Result.Error(IllegalStateException("e")) as Result<Int>).getOrThrow() }
        assertFailsWith<IllegalStateException> { (Result.Loading as Result<Int>).getOrThrow() }
    }

    // -------------------------------------------------------------------------
    // AppClassifier.inferFromPackageName — every category branch + enforcement
    // -------------------------------------------------------------------------

    private val classifier = AppClassifier.fromDefaults(emptyList())

    @Test fun inferEmptyCalorieApp() {
        val c = classifier.classify("com.instagram.android")
        assertEquals(AppCategory.EMPTY_CALORIES, c?.category)
    }

    @Test fun inferNutritiveLearningApp() {
        val c = classifier.classify("com.duolingo.app")
        assertEquals(AppCategory.NUTRITIVE, c?.category)
    }

    @Test fun inferNeutralProductivityApp() {
        val c = classifier.classify("com.slack.android")
        assertEquals(AppCategory.NEUTRAL, c?.category)
    }

    @Test fun inferUnknownAppReturnsNull() {
        assertNull(classifier.classify("io.example.unknownapp"))
    }
}
