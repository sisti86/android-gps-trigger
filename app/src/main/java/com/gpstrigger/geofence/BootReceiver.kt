package com.gpstrigger.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Geofences are dropped on reboot and on app update, so register them again. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            ResyncWorker.runNow(context)
        }
    }
}
