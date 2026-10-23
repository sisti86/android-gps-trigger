package com.gpstrigger.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.gpstrigger.engine.ZoneEngine

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofence error: ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        val ids = event.triggeringGeofences?.map { it.requestId } ?: return
        when (event.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> ids.forEach { ZoneEngine.enter(context, it) }
            Geofence.GEOFENCE_TRANSITION_EXIT -> ids.forEach { ZoneEngine.exit(context, it) }
        }
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
