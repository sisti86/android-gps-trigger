package com.gpstrigger.data

import kotlinx.serialization.Serializable
import java.util.UUID

const val DEFAULT_RADIUS_METERS = 150f

@Serializable
enum class RingerSetting { NORMAL, VIBRATE, SILENT }

/**
 * A set of device settings. A `null` field means "leave unchanged".
 * Used both for what a zone applies and for snapshots of the device state.
 */
@Serializable
data class ZoneActions(
    val ringVolume: Int? = null,
    val notificationVolume: Int? = null,
    val callVolume: Int? = null,
    val ringerMode: RingerSetting? = null,
    val wifiEnabled: Boolean? = null,
) {
    /** Values set in [other] win over the ones in this instance. */
    fun overlay(other: ZoneActions) = ZoneActions(
        ringVolume = other.ringVolume ?: ringVolume,
        notificationVolume = other.notificationVolume ?: notificationVolume,
        callVolume = other.callVolume ?: callVolume,
        ringerMode = other.ringerMode ?: ringerMode,
        wifiEnabled = other.wifiEnabled ?: wifiEnabled,
    )

    /** Keeps only the settings that [mask] defines. */
    fun restrictTo(mask: ZoneActions) = ZoneActions(
        ringVolume = ringVolume.takeIf { mask.ringVolume != null },
        notificationVolume = notificationVolume.takeIf { mask.notificationVolume != null },
        callVolume = callVolume.takeIf { mask.callVolume != null },
        ringerMode = ringerMode.takeIf { mask.ringerMode != null },
        wifiEnabled = wifiEnabled.takeIf { mask.wifiEnabled != null },
    )
}

@Serializable
data class Zone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = DEFAULT_RADIUS_METERS,
    val address: String? = null,
    val enabled: Boolean = true,
    val actions: ZoneActions = ZoneActions(),
) {
    val hasLocation get() = !latitude.isNaN() && !longitude.isNaN()
}

/**
 * Which zones the device is currently inside (in order of entry) and the device
 * settings captured before entering the first of them.
 */
@Serializable
data class ActiveState(
    val baseline: ZoneActions? = null,
    val activeZoneIds: List<String> = emptyList(),
)
