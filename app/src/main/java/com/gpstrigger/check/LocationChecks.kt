package com.gpstrigger.check

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.gpstrigger.engine.LocationCheck

/** Schedules the periodic location checks, run by [LocationCheckService]. */
object LocationChecks {

    fun runNow(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, LocationCheckService::class.java))
    }

    /** Schedules the next check, unless one is already pending (e.g. on every process start). */
    fun ensureScheduled(context: Context) {
        if (alarmIntent(context, PendingIntent.FLAG_NO_CREATE) == null) scheduleNext(context)
    }

    fun scheduleNext(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java)
        val minutes = LocationCheck.intervalMinutes()
        if (minutes == null) {
            // No zone needs checks today: the day-change alarm will start them again.
            alarmIntent(context, PendingIntent.FLAG_NO_CREATE)?.let {
                alarms.cancel(it)
                it.cancel()
            }
            return
        }
        val intent = alarmIntent(context, PendingIntent.FLAG_UPDATE_CURRENT)!!
        val at = System.currentTimeMillis() + minutes * 60_000L
        setAlarm(alarms, at, intent)
    }

    /** Exact when allowed (always, with targetSdk 28), so the chosen frequency is respected. */
    fun setAlarm(alarms: AlarmManager, at: Long, intent: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
        }
    }

    private fun alarmIntent(context: Context, flag: Int): PendingIntent? = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, CheckAlarmReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or flag,
    )
}
