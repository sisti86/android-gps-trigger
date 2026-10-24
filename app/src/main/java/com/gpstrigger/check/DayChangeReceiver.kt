package com.gpstrigger.check

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Re-evaluates the zones' weekly schedule right after midnight: the check leaves zones
 * not scheduled for the new day and enters newly scheduled ones the device is inside.
 */
class DayChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LocationChecks.runNow(context)
        schedule(context)
    }

    companion object {
        fun schedule(context: Context) {
            val intent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, DayChangeReceiver::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            // One minute past midnight, so the new day has surely started.
            val next = LocalDate.now().plusDays(1).atTime(0, 1)
                .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            LocationChecks.setAlarm(context.getSystemService(AlarmManager::class.java), next, intent)
        }
    }
}
