package com.example.flowschannels.data.location

import java.util.Timer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.schedule

data class Location(val latitude: Double, val longitude: Double, val index: Int)

/** A classic listener interface: fire-and-forget pushes with manual registration. */
interface LocationCallback {
    fun onLocationChanged(location: Location)
    fun onFailure(error: Throwable)
}

/**
 * A callback-based API that knows nothing about coroutines — on purpose.
 *
 * This is the shape of `FusedLocationProviderClient`, `SensorManager`, `BroadcastReceiver`,
 * `WebSocketListener`, Firebase listeners, and most third-party SDKs. It pushes values whenever it
 * wants and requires explicit unregistration, which is precisely the mismatch `callbackFlow`
 * exists to bridge.
 *
 * [activeListenerCount] is exposed so the callbackFlow lesson can *prove* that `awaitClose`
 * actually unregistered the listener when collection stopped.
 */
class FakeLocationService(private val intervalMs: Long = 800) {

    private val callbacks = CopyOnWriteArrayList<LocationCallback>()
    private var timer: Timer? = null
    private var tick = 0

    @Volatile
    private var failAfter: Int = -1

    val activeListenerCount: Int get() = callbacks.size

    /** Makes the service report an error after [ticks] updates, to demo `close(cause)`. */
    fun failAfter(ticks: Int) {
        failAfter = ticks
    }

    @Synchronized
    fun registerCallback(callback: LocationCallback) {
        callbacks.add(callback)
        if (timer == null) {
            tick = 0
            timer = Timer("fake-location", true).apply {
                schedule(delay = intervalMs, period = intervalMs) { publish() }
            }
        }
    }

    @Synchronized
    fun unregisterCallback(callback: LocationCallback) {
        callbacks.remove(callback)
        if (callbacks.isEmpty()) {
            timer?.cancel()
            timer = null
        }
    }

    private fun publish() {
        tick++
        if (failAfter in 1..tick) {
            failAfter = -1
            callbacks.forEach { it.onFailure(IllegalStateException("GPS signal lost (simulated)")) }
            return
        }
        val location = Location(
            latitude = 52.5200 + tick * 0.0012,
            longitude = 13.4050 + tick * 0.0009,
            index = tick,
        )
        callbacks.forEach { it.onLocationChanged(location) }
    }
}
