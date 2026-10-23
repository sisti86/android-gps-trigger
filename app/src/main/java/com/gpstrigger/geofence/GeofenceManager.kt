package com.gpstrigger.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.gpstrigger.Permissions
import com.gpstrigger.data.ZoneStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

enum class SyncResult { OK, NO_PERMISSION, LOCATION_UNAVAILABLE, ERROR }

/** Registers one Play Services geofence per enabled zone. */
object GeofenceManager {
    private const val TAG = "GeofenceManager"
    private val mutex = Mutex()

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // Play Services adds the event extras, so the intent must stay mutable.
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
    }

    /**
     * Replaces all registered geofences with the enabled zones. The initial triggers make
     * Play Services report right away whether the device is inside or outside each zone,
     * which also fixes zones still marked active after missed events.
     */
    @SuppressLint("MissingPermission")
    suspend fun sync(context: Context): SyncResult = mutex.withLock {
        if (!Permissions.hasFineLocation(context)) return SyncResult.NO_PERMISSION
        val client = LocationServices.getGeofencingClient(context)
        val geofences = ZoneStore.zones.value.filter { it.enabled }.map { zone ->
            Geofence.Builder()
                .setRequestId(zone.id)
                .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                .build()
        }
        try {
            client.removeGeofences(pendingIntent(context)).await()
            if (geofences.isNotEmpty()) {
                val request = GeofencingRequest.Builder()
                    .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_EXIT)
                    .addGeofences(geofences)
                    .build()
                client.addGeofences(request, pendingIntent(context)).await()
            }
            SyncResult.OK
        } catch (e: ApiException) {
            Log.w(TAG, "Geofence sync failed: ${GeofenceStatusCodes.getStatusCodeString(e.statusCode)}", e)
            if (e.statusCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                SyncResult.LOCATION_UNAVAILABLE
            } else {
                SyncResult.ERROR
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Geofence sync failed", e)
            SyncResult.NO_PERMISSION
        }
    }
}
