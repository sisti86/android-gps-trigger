package com.gpstrigger.engine

import android.content.Context
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.gpstrigger.data.RingerSetting
import com.gpstrigger.data.ZoneActions

/** Reads and changes the audio and Wi-Fi settings handled by the app. */
class DeviceSettings(context: Context) {
    private val audio = context.getSystemService(AudioManager::class.java)
    private val wifi = context.applicationContext.getSystemService(WifiManager::class.java)

    fun capture() = ZoneActions(
        ringVolume = audio.getStreamVolume(AudioManager.STREAM_RING),
        notificationVolume = audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION),
        callVolume = audio.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
        ringerMode = when (audio.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerSetting.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerSetting.VIBRATE
            else -> RingerSetting.NORMAL
        },
        wifiEnabled = wifi.isWifiEnabled,
    )

    fun volume(stream: Int): Int = audio.getStreamVolume(stream)

    fun range(stream: Int): IntRange {
        val min = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) audio.getStreamMinVolume(stream) else 0
        return min..audio.getStreamMaxVolume(stream)
    }

    /** Applies every non-null setting. Returns false if at least one could not be changed. */
    fun apply(actions: ZoneActions): Boolean {
        var ok = true
        fun attempt(what: String, block: () -> Boolean) {
            val applied = try {
                block()
            } catch (e: SecurityException) {
                Log.w(TAG, "Cannot change $what", e)
                false
            }
            if (!applied) ok = false
        }

        actions.wifiEnabled?.let { enabled ->
            // Only works because targetSdk is 28: newer targets get `false` back.
            @Suppress("DEPRECATION")
            attempt("wifi") { wifi.setWifiEnabled(enabled) }
        }

        // Raising the ring volume leaves vibrate/silent mode and lowering it to 0 enters it,
        // so "Sound" goes first and "Vibrate"/"Silent" last to win over the volume changes.
        val mode = actions.ringerMode
        if (mode == RingerSetting.NORMAL) setRingerMode(mode, ::attempt)
        actions.ringVolume?.let { setVolume(AudioManager.STREAM_RING, it, ::attempt) }
        actions.notificationVolume?.let { setVolume(AudioManager.STREAM_NOTIFICATION, it, ::attempt) }
        actions.callVolume?.let { setVolume(AudioManager.STREAM_VOICE_CALL, it, ::attempt) }
        if (mode != null && mode != RingerSetting.NORMAL) setRingerMode(mode, ::attempt)

        return ok
    }

    private fun setVolume(stream: Int, value: Int, attempt: (String, () -> Boolean) -> Unit) =
        attempt("volume of stream $stream") {
            audio.setStreamVolume(stream, value.coerceIn(range(stream)), 0)
            true
        }

    private fun setRingerMode(mode: RingerSetting, attempt: (String, () -> Boolean) -> Unit) =
        attempt("ringer mode") {
            audio.ringerMode = when (mode) {
                RingerSetting.NORMAL -> AudioManager.RINGER_MODE_NORMAL
                RingerSetting.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
                RingerSetting.SILENT -> AudioManager.RINGER_MODE_SILENT
            }
            true
        }

    private companion object {
        const val TAG = "DeviceSettings"
    }
}
