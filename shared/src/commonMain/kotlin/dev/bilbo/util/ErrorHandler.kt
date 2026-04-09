// ErrorHandler.kt
// Bilbo — Shared Error Handler
//
// Provides:
//   • Global error handler interface
//   • User-friendly message mapping for known exceptions
//   • Offline mode detection
//   • Retry logic with exponential back-off for transient network errors

package dev.bilbo.util

import kotlinx.coroutines.delay

// MARK: - Error categories

sealed class BilboError : Exception() {

    /** No internet connection. */
    data class Offline(override val message: String = "You're offline. Check your connection.") : BilboError()

    /** Server-side error (5xx). */
    data class ServerError(val statusCode: Int, override val message: String = "Server error ($statusCode). Try again shortly.") : BilboError()

    /** Client-side error (4xx). */
    data class ClientError(val statusCode: Int, override val message: String) : BilboError()

    /** Authentication / session expired. */
    data class Unauthorized(override val message: String = "Session expired. Please sign in again.") : BilboError()

    /** Data not found. */
    data class NotFound(override val message: String = "The requested data was not found.") : BilboError()

    /** Local database or parsing error. */
    data class DataError(override val cause: Throwable? = null, override val message: String = "A data error occurred. Try reopening Bilbo.") : BilboError()

    /** Unexpected / unknown error. */
    data class Unknown(override val cause: Throwable? = null, override val message: String = "Something went wrong. Please try again.") : BilboError()
}

// MARK: - ErrorHandler interface

/**
 * Global error handler.
 * Register a listener to receive user-visible error messages.
 */
interface ErrorHandler {

    /** Map any throwable to a user-friendly [BilboError]. */
    fun map(throwable: Throwable): BilboError

    /**
     * Handle an error — maps it and notifies registered listeners.
     * Returns the mapped [BilboError] for callers that need it.
     */
    fun handle(throwable: Throwable): BilboError

    /** Register a callback to receive user-visible error messages. */
    fun addListener(listener: ErrorListener)

    /** Remove a previously registered listener. */
    fun removeListener(listener: ErrorListener)
}

fun interface ErrorListener {
    fun onError(error: BilboError)
}

// MARK: - Default implementation

class DefaultErrorHandler : ErrorHandler {

    private val listeners = mutableListOf<ErrorListener>()

    override fun map(throwable: Throwable): BilboError {
        return when (throwable) {
            is BilboError -> throwable  // Already mapped
            is OfflineException       -> BilboError.Offline()
            is NetworkException -> when {
                throwable.statusCode == 401 || throwable.statusCode == 403 -> BilboError.Unauthorized()
                throwable.statusCode == 404 -> BilboError.NotFound()
                throwable.statusCode in 400..499 -> BilboError.ClientError(
                    statusCode = throwable.statusCode,
                    message = throwable.message ?: "Request failed (${throwable.statusCode})."
                )
                throwable.statusCode in 500..599 -> BilboError.ServerError(throwable.statusCode)
                else -> BilboError.Unknown(cause = throwable)
            }
            is kotlinx.serialization.SerializationException -> BilboError.DataError(
                cause = throwable,
                message = "Failed to read data. The app may need updating."
            )
            else -> BilboError.Unknown(cause = throwable)
        }
    }

    override fun handle(throwable: Throwable): BilboError {
        val error = map(throwable)
        notifyListeners(error)
        return error
    }

    override fun addListener(listener: ErrorListener) {
        if (!listeners.contains(listener)) listeners.add(listener)
    }

    override fun removeListener(listener: ErrorListener) {
        listeners.remove(listener)
    }

    private fun notifyListeners(error: BilboError) {
        listeners.forEach { it.onError(error) }
    }
}

// MARK: - Platform exceptions (wrapped by network layer)

/** Thrown when the device has no internet connectivity. */
class OfflineException(message: String = "No internet connection") : Exception(message)

/** Thrown for HTTP errors with a status code. */
class NetworkException(val statusCode: Int, message: String) : Exception(message)

// MARK: - Offline detection helper

expect fun isNetworkAvailable(): Boolean

// MARK: - Retry logic

/**
 * Retry a suspend block up to [maxAttempts] times with exponential back-off.
 * Only retries on [BilboError.Offline] or [BilboError.ServerError].
 *
 * @param maxAttempts  Maximum total attempts (default 3).
 * @param initialDelay Initial delay in ms before first retry (default 500 ms).
 * @param factor       Multiplier applied to delay on each retry (default 2.0).
 * @param errorHandler Used to map exceptions to [BilboError].
 * @param block        The suspending operation to execute.
 */
suspend fun <T> withRetry(
    maxAttempts: Int = 3,
    initialDelay: Long = 500L,
    factor: Double = 2.0,
    errorHandler: ErrorHandler = DefaultErrorHandler(),
    block: suspend () -> T
): T {
    var currentDelay = initialDelay
    repeat(maxAttempts - 1) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            val mapped = errorHandler.map(e)
            val shouldRetry = mapped is BilboError.Offline || mapped is BilboError.ServerError
            if (!shouldRetry || attempt == maxAttempts - 2) throw mapped
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong()
    }
    // Final attempt — let exceptions propagate
    return block()
}

// MARK: - Result extensions

/**
 * Execute [block] and wrap success/failure in [Result].
 * Maps failures through [errorHandler] to typed [BilboError].
 */
suspend fun <T> safeCall(
    errorHandler: ErrorHandler = DefaultErrorHandler(),
    block: suspend () -> T
): Result<T> {
    return try {
        Result.success(block())
    } catch (e: Exception) {
        Result.failure(errorHandler.map(e))
    }
}

/** Returns a user-friendly display message for any [Throwable]. */
fun Throwable.toUserMessage(): String {
    return when (this) {
        is BilboError -> this.message ?: "An error occurred."
        else -> DefaultErrorHandler().map(this).message ?: "An error occurred."
    }
}
