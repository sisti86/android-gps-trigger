package com.gpstrigger.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.gpstrigger.data.AppSettings
import com.gpstrigger.data.ThemeMode
import com.gpstrigger.data.Zone
import com.gpstrigger.data.ZoneStore
import com.gpstrigger.engine.ZoneEngine
import com.gpstrigger.geofence.GeofenceManager
import com.gpstrigger.geofence.SyncResult
import kotlinx.coroutines.launch

class MainViewModel(private val app: Application) : AndroidViewModel(app) {
    val zones = ZoneStore.zones
    val active = ZoneStore.active
    val themeMode = AppSettings.themeMode

    /** Zone being edited; `null` shows the zone list. */
    var draft by mutableStateOf<Zone?>(null)
        private set
    var picking by mutableStateOf(false)
        private set
    var syncResult by mutableStateOf<SyncResult?>(null)
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
        // Leave the old version first so its settings get restored; the geofence
        // initial trigger re-enters the zone with the new settings if still inside.
        original?.let { ZoneEngine.exit(app, it, notify = false) }
        ZoneStore.upsert(zone)
        closeEditor()
        sync()
    }

    fun delete() {
        original?.let {
            ZoneEngine.exit(app, it, notify = false)
            ZoneStore.delete(it.id)
        }
        closeEditor()
        sync()
    }

    fun setEnabled(zone: Zone, enabled: Boolean) {
        if (!enabled) ZoneEngine.exit(app, zone, notify = false)
        ZoneStore.upsert(zone.copy(enabled = enabled))
        sync()
    }

    fun simulate(enter: Boolean) {
        val zone = original ?: return
        if (enter) ZoneEngine.enter(app, zone.id, ignoreSchedule = true) else ZoneEngine.exit(app, zone)
    }

    fun setThemeMode(mode: ThemeMode) {
        AppSettings.setThemeMode(app, mode)
    }

    fun sync() {
        viewModelScope.launch { syncResult = GeofenceManager.sync(app) }
    }
}
