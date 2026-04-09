package dev.bilbo.shared.util

/**
 * A sealed class representing the outcome of an async operation,
 * used to propagate loading, success, and error states through
 * Kotlin flows across both Android and iOS targets.
 */
sealed class Result<out T> {
    data object Loading : Result<Nothing>()
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
}

/** Returns the [Success] data or null if this is not a success state. */
fun <T> Result<T>.getOrNull(): T? = (this as? Result.Success)?.data

/** Throws the contained exception if this is an [Error], otherwise returns data. */
fun <T> Result<T>.getOrThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Error -> throw exception
    is Result.Loading -> error("Result is still Loading")
}

/** Transforms a [Success] result with [transform], leaving other states unchanged. */
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> = when (this) {
    is Result.Success -> Result.Success(transform(data))
    is Result.Error -> this
    is Result.Loading -> this
}
