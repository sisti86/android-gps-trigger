package com.gpstrigger.engine

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

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.channel_zone_events),
            NotificationManager.IMPORTANCE_LOW,
        )
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

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
