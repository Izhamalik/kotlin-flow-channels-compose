package com.example.flowschannels.data.location

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Turns a listener-based API into a cold [Flow].
 *
 * Three details that are the entire reason `callbackFlow` exists:
 *
 * 1. [kotlinx.coroutines.channels.ProducerScope.trySend] is used from the callback thread.
 *    Callbacks are not suspend functions, so `send` (which suspends) is the wrong API here.
 * 2. [awaitClose] is the unregister hook. Without it the listener leaks, and worse: the Flow
 *    never suspends, so collection completes immediately and you get nothing.
 * 3. Collection cancellation runs [awaitClose], which is how Flow cancellation maps onto
 *    "please stop the GPS / sensor / WebSocket".
 */
fun FakeLocationService.locationUpdates(): Flow<Location> = callbackFlow {
    val callback = object : LocationCallback {
        override fun onLocationChanged(location: Location) {
            // trySend: the callback is not a coroutine. If the downstream is slow the value is
            // dropped or buffered according to the channel capacity of callbackFlow (default 64).
            val result = trySend(location)
            if (result.isFailure) {
                close(result.exceptionOrNull())
            }
        }

        override fun onFailure(error: Throwable) {
            close(error)
        }
    }
    registerCallback(callback)
    awaitClose { unregisterCallback(callback) }
}
