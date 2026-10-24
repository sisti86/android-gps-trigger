package com.gpstrigger.check

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import com.gpstrigger.engine.GpsFix
import com.gpstrigger.engine.LocationCheck
import com.gpstrigger.engine.Notifications
import com.gpstrigger.engine.ZoneEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Takes one GPS fix, updates the zones and schedules the next check. Runs as a foreground
 * service (with a short-lived notification) because Android only gives background apps
 * a location a few times per hour.
 */
class LocationCheckService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(Notifications.CHECK_NOTIFICATION_ID, Notifications.checkingLocation(this))
        // A start while a check is running is covered by that check.
        if (!running) {
            running = true
            scope.launch {
                val wakeLock = getSystemService(PowerManager::class.java)
                    .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GpsTrigger:check")
                    .apply { acquire(WAKE_LOCK_TIMEOUT_MS) }
                try {
                    check()
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    LocationChecks.scheduleNext(this@LocationCheckService)
                    running = false
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    private suspend fun check() {
        ZoneEngine.leaveUnscheduledZones(this)
        if (!LocationCheck.isNeeded()) return
        GpsFix.obtain(this)?.let { LocationCheck.evaluate(this, it) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private companion object {
        const val WAKE_LOCK_TIMEOUT_MS = 90_000L
    }
}
