package com.gpstrigger.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.gpstrigger.check.LocationChecks
import com.gpstrigger.data.AppSettings
import com.gpstrigger.data.ThemeMode
import com.gpstrigger.data.Zone
import com.gpstrigger.data.ZoneStore
import com.gpstrigger.engine.ZoneEngine

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    val zones = ZoneStore.zones
    val active = ZoneStore.active
    val themeMode = AppSettings.themeMode

    /** Zone being edited; `null` shows the zone list. */
    var draft by mutableStateOf<Zone?>(null)
        private set
    var picking by mutableStateOf(false)
        private set

    /** The saved version of [draft], `null` for a new zone. */
    var original by mutableStateOf<Zone?>(null)
        private set

    fun newZone() {
        original = null
        draft = Zone(name = "", latitude = Double.NaN, longitude = Double.NaN)
        picking = true
    }

    fun edit(zone: Zone) {
        original = zone
        draft = zone
    }

    fun updateDraft(zone: Zone) {
        draft = zone
    }

    fun openPicker() {
        picking = true
    }

    fun closePicker() {
        picking = false
        if (draft?.hasLocation == false) closeEditor()
    }

    fun confirmLocation(latitude: Double, longitude: Double, radius: Float, address: String?) {
        draft = draft?.copy(latitude = latitude, longitude = longitude, radiusMeters = radius, address = address)
        picking = false
    }

    fun closeEditor() {
        draft = null
        original = null
        picking = false
    }

    fun save() {
        val zone = draft ?: return
        // Leave the old version first so its settings get restored; the location
        // check right after re-enters the zone with the new settings if still inside.
        original?.let { ZoneEngine.exit(app, it, notify = false) }
        ZoneStore.upsert(zone)
        closeEditor()
        checkNow()
    }

    fun delete() {
        original?.let {
            ZoneEngine.exit(app, it, notify = false)
            ZoneStore.delete(it.id)
        }
        closeEditor()
        checkNow()
    }

    fun setEnabled(zone: Zone, enabled: Boolean) {
        if (!enabled) ZoneEngine.exit(app, zone, notify = false)
        ZoneStore.upsert(zone.copy(enabled = enabled))
        checkNow()
    }

    fun simulate(enter: Boolean) {
        val zone = original ?: return
        if (enter) ZoneEngine.enter(app, zone.id, ignoreSchedule = true) else ZoneEngine.exit(app, zone)
    }

    fun setThemeMode(mode: ThemeMode) {
        AppSettings.setThemeMode(app, mode)
    }

    /** Runs a location check now, e.g. when the app is opened or a zone changes. */
    fun checkNow() {
        if (zones.value.any { it.enabled } || active.value.activeZoneIds.isNotEmpty()) {
            LocationChecks.runNow(app)
        }
    }
}
