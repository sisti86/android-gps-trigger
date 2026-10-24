package com.gpstrigger.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gpstrigger.R
import com.gpstrigger.data.Zone
import com.gpstrigger.ui.MainActivity

object Notifications {
    private const val CHANNEL_ID = "zone_events"
    private const val CHECK_CHANNEL_ID = "location_checks"
    const val CHECK_NOTIFICATION_ID = 1

    fun createChannels(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannels(
            listOf(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.channel_zone_events),
                    NotificationManager.IMPORTANCE_LOW,
                ),
                NotificationChannel(
                    CHECK_CHANNEL_ID,
                    context.getString(R.string.channel_location_checks),
                    NotificationManager.IMPORTANCE_MIN,
                ),
            )
        )
    }

    /** Shown by the foreground service for the few seconds a location check takes. */
    fun checkingLocation(context: Context): Notification =
        NotificationCompat.Builder(context, CHECK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zone)
            .setContentTitle(context.getString(R.string.notif_checking_location))
            .setOngoing(true)
            .setSilent(true)
            .build()

    fun zoneEvent(context: Context, zone: Zone, entered: Boolean, ok: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val title = context.getString(if (entered) R.string.notif_entered else R.string.notif_exited, zone.name)
        val text = context.getString(
            when {
                !ok -> R.string.notif_partial
                entered -> R.string.notif_applied
                else -> R.string.notif_restored
            }
        )
        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_zone)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(zone.id.hashCode(), notification)
        } catch (_: SecurityException) {
            // Notification permission revoked in the meantime.
        }
    }
}
