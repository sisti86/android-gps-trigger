package com.gpstrigger.engine

import android.content.Context
import com.gpstrigger.data.ActiveState
import com.gpstrigger.data.Zone
import com.gpstrigger.data.ZoneActions
import com.gpstrigger.data.ZoneStore

/**
 * Applies a zone's settings on entry and restores the previous ones on exit.
 *
 * The device state is captured once, when entering the first zone. With overlapping
 * zones, leaving one falls back to the most recently entered zone that is still active,
 * then to that captured state.
 */
object ZoneEngine {

    @Synchronized
    fun enter(context: Context, zoneId: String) {
        val zone = ZoneStore.zone(zoneId)?.takeIf { it.enabled } ?: return
        val state = ZoneStore.active.value
        if (zoneId in state.activeZoneIds) return

        val settings = DeviceSettings(context)
        val baseline = state.baseline.takeIf { state.activeZoneIds.isNotEmpty() } ?: settings.capture()
        ZoneStore.setActive(ActiveState(baseline, state.activeZoneIds + zoneId))

        val ok = settings.apply(zone.actions)
        Notifications.zoneEvent(context, zone, entered = true, ok)
    }

    fun exit(context: Context, zoneId: String) {
        val zone = ZoneStore.zone(zoneId)
        if (zone != null) {
            exit(context, zone)
        } else {
            forget(zoneId)
        }
    }

    /** [zone] is passed explicitly so a zone can be left right before it is edited or deleted. */
    @Synchronized
    fun exit(context: Context, zone: Zone, notify: Boolean = true) {
        val state = ZoneStore.active.value
        if (zone.id !in state.activeZoneIds) return

        val remainingIds = state.activeZoneIds - zone.id
        val restored = remainingIds
            .mapNotNull(ZoneStore::zone)
            .fold(state.baseline ?: ZoneActions()) { acc, z -> acc.overlay(z.actions) }
            .restrictTo(zone.actions)
        ZoneStore.setActive(
            if (remainingIds.isEmpty()) ActiveState() else state.copy(activeZoneIds = remainingIds)
        )

        val ok = DeviceSettings(context).apply(restored)
        if (notify) Notifications.zoneEvent(context, zone, entered = false, ok)
    }

    @Synchronized
    private fun forget(zoneId: String) {
        val state = ZoneStore.active.value
        if (zoneId !in state.activeZoneIds) return
        val remainingIds = state.activeZoneIds - zoneId
        ZoneStore.setActive(
            if (remainingIds.isEmpty()) ActiveState() else state.copy(activeZoneIds = remainingIds)
        )
    }
}
