package com.gpstrigger.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.UUID

const val DEFAULT_RADIUS_METERS = 150f

/** ISO days of the week: 1 = Monday … 7 = Sunday. */
val ALL_DAYS: Set<Int> = (1..7).toSet()

val CHECK_INTERVALS_MINUTES = listOf(1, 10, 20, 30, 60)
const val DEFAULT_CHECK_INTERVAL_MINUTES = 1

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
    /** ISO days of the week on which the zone triggers. */
    val activeDays: Set<Int> = ALL_DAYS,
    /** Maximum delay for Play Services to report entering or leaving; longer saves battery. */
    val checkIntervalMinutes: Int = DEFAULT_CHECK_INTERVAL_MINUTES,
) {
    val hasLocation get() = !latitude.isNaN() && !longitude.isNaN()

    fun isScheduledOn(date: LocalDate = LocalDate.now()) = date.dayOfWeek.value in activeDays
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
