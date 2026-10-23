package com.gpstrigger.ui

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.gpstrigger.Permissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import java.io.IOException
import kotlin.coroutines.resume

/** Address search and current position, using only built-in Android / Play Services APIs (no API key). */
object LocationLookup {

    val geocoderAvailable: Boolean get() = Geocoder.isPresent()

    @Suppress("DEPRECATION")
    suspend fun search(context: Context, query: String): List<Address> = geocode(
        context,
        modern = { geocoder, listener -> geocoder.getFromLocationName(query, 5, listener) },
        legacy = { geocoder -> geocoder.getFromLocationName(query, 5) },
    )

    @Suppress("DEPRECATION")
    suspend fun reverse(context: Context, point: GeoPoint): String? = geocode(
        context,
        modern = { geocoder, listener -> geocoder.getFromLocation(point.latitude, point.longitude, 1, listener) },
        legacy = { geocoder -> geocoder.getFromLocation(point.latitude, point.longitude, 1) },
    ).firstOrNull()?.let(::format)

    fun format(address: Address): String =
        (0..address.maxAddressLineIndex).joinToString(", ") { address.getAddressLine(it) }

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(context: Context): GeoPoint? {
        if (!Permissions.hasFineLocation(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        return try {
            val location = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
                ?: client.lastLocation.await()
            location?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: Exception) {
            null
        }
    }

    /** Uses the async Geocoder API on Android 13+ and the blocking one on a background thread before. */
    private suspend fun geocode(
        context: Context,
        modern: (Geocoder, Geocoder.GeocodeListener) -> Unit,
        legacy: (Geocoder) -> List<Address>?,
    ): List<Address> {
        if (!geocoderAvailable) return emptyList()
        val geocoder = Geocoder(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return suspendCancellableCoroutine { cont ->
                modern(geocoder, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) = cont.resume(emptyList())
                })
            }
        }
        return withContext(Dispatchers.IO) {
            try {
                legacy(geocoder).orEmpty()
            } catch (_: IOException) {
                emptyList()
            }
        }
    }
}
