package com.gpstrigger.check

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CheckAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        LocationChecks.runNow(context)
    }
}
