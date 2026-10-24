package com.gpstrigger.engine

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import com.gpstrigger.Permissions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Gets the position from the satellite GPS only. The "network" provider is avoided on purpose:
 * on most phones it is Google's, and it sends nearby Wi-Fi and cell data to Google's servers.
 */
object GpsFix {
    /** A fix this accurate is good enough to stop the GPS right away. */
    private const val GOOD_ACCURACY_M = 50f
    /** A last known fix younger than this is reused; short, so the previous check's fix isn't. */
    private const val FRESH_MS = 30_000L

    @SuppressLint("MissingPermission")
    suspend fun obtain(context: Context, timeoutMs: Long = 60_000): Location? {
        if (!Permissions.hasFineLocation(context)) return null
        val manager = context.getSystemService(LocationManager::class.java)
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) return null

        manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?.takeIf { it.isFresh() && it.accuracy <= GOOD_ACCURACY_M }
            ?.let { return it }

        var best: Location? = null
        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (best.let { it == null || location.accuracy < it.accuracy }) best = location
                        if (location.accuracy <= GOOD_ACCURACY_M && cont.isActive) {
                            manager.removeUpdates(this)
                            cont.resume(location)
                        }
                    }

                    // Abstract before Android 11, so they must be implemented.
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                }
                manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        } ?: best
    }

    private fun Location.isFresh() =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000 < FRESH_MS
}
