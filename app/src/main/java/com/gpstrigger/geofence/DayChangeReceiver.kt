package com.gpstrigger.geofence

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

/**
 * Re-evaluates the zones' weekly schedule right after midnight: geofence events don't
 * fire when the day changes while the device sits inside a zone.
 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        ResyncWorker.runNow(context)
        schedule(context)
    }

    companion object {
        fun schedule(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val intent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, DayChangeReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            // One minute past midnight, so the new day has surely started.
            val next = LocalDate.now().plusDays(1).atTime(0, 1)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
                alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            } else {
                alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, intent)
            }
        }
    }
}
