package dev.bilbo.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Convenience wrapper to collect a [Flow] from Swift/Objective-C.
 *
 * iOS cannot directly consume Kotlin suspend functions or Flow, so this
 * helper provides a callback-based API bridge. On iOS, pass in
 * a [CoroutineScope] derived from a ViewModel or lifecycle object.
 *
 * @param scope   The [CoroutineScope] in which to collect.
 * @param onEach  Called for each emitted item.
 * @param onError Called when the flow terminates with an exception.
 * @param onComplete Called when the flow completes normally.
 */
fun <T> Flow<T>.collectAsCallback(
    scope: CoroutineScope,
    onEach: (T) -> Unit,
    onError: (Throwable) -> Unit = {},
    onComplete: () -> Unit = {},
) {
    this
        .onEach { onEach(it) }
        .catch { onError(it) }
        .launchIn(scope)
}

/**
 * A [CoroutineScope] backed by [Dispatchers.Main] that can be used
 * as a default scope for iOS bridging helpers.
 */
val MainScope: CoroutineScope get() = CoroutineScope(Dispatchers.Main)
