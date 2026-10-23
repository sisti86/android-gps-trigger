package com.gpstrigger.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Persists zones and the active-zone state in SharedPreferences as JSON. */
object ZoneStore {
    private const val KEY_ZONES = "zones"
    private const val KEY_ACTIVE = "active"

    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var prefs: SharedPreferences

    private val _zones = MutableStateFlow<List<Zone>>(emptyList())
    val zones: StateFlow<List<Zone>> = _zones.asStateFlow()

    private val _active = MutableStateFlow(ActiveState())
    val active: StateFlow<ActiveState> = _active.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("zone_store", Context.MODE_PRIVATE)
        _zones.value = decode(KEY_ZONES) ?: emptyList()
        _active.value = decode(KEY_ACTIVE) ?: ActiveState()
    }

    fun zone(id: String): Zone? = _zones.value.firstOrNull { it.id == id }

    @Synchronized
    fun upsert(zone: Zone) {
        val list = _zones.value.toMutableList()
        val index = list.indexOfFirst { it.id == zone.id }
        if (index >= 0) list[index] = zone else list += zone
        _zones.value = list
        prefs.edit().putString(KEY_ZONES, json.encodeToString(list)).apply()
    }

    @Synchronized
    fun delete(id: String) {
        val list = _zones.value.filterNot { it.id == id }
        _zones.value = list
        prefs.edit().putString(KEY_ZONES, json.encodeToString(list)).apply()
    }

    @Synchronized
    fun setActive(state: ActiveState) {
        _active.value = state
        // commit(): this is often called from a broadcast receiver right before the process may die.
        prefs.edit().putString(KEY_ACTIVE, json.encodeToString(state)).commit()
    }

    private inline fun <reified T> decode(key: String): T? =
        prefs.getString(key, null)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }
}
