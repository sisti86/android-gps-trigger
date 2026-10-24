package com.gpstrigger.engine

import android.content.Context
import android.location.Location
import com.gpstrigger.data.ZoneStore

/** Decides from a GPS fix which zones the device has entered or left. */
object LocationCheck {
    /** Fixes less accurate than this are not trusted to enter a zone. */
    private const val MAX_ENTER_ACCURACY_M = 200f

    fun evaluate(context: Context, location: Location) {
        val activeIds = ZoneStore.active.value.activeZoneIds
        ZoneStore.zones.value.filter { it.enabled }.forEach { zone ->
            val result = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, zone.latitude, zone.longitude, result)
            val distance = result[0]
            val inside = zone.id in activeIds
            when {
                !inside && distance <= zone.radiusMeters && location.accuracy <= MAX_ENTER_ACCURACY_M ->
                    ZoneEngine.enter(context, zone.id)
                // Leave only when outside even counting the fix's error, so a jumpy fix
                // near the border doesn't toggle the zone back and forth.
                inside && distance - location.accuracy > zone.radiusMeters ->
                    ZoneEngine.exit(context, zone)
            }
        }
    }

    /** Checks run as often as the most demanding zone scheduled for today asks; null if none needs them. */
    fun intervalMinutes(): Int? =
        ZoneStore.zones.value.filter { it.enabled && it.isScheduledOn() }.minOfOrNull { it.checkIntervalMinutes }

    fun isNeeded(): Boolean =
        intervalMinutes() != null || ZoneStore.active.value.activeZoneIds.isNotEmpty()
}
