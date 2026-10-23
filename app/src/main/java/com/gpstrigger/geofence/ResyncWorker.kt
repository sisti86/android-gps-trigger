package com.gpstrigger.geofence

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

/**
 * Re-registers the geofences. Runs after boot and periodically, because Play Services
 * drops geofences in some cases (e.g. location turned off) without telling the app.
 */
class ResyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        GeofenceManager.sync(applicationContext)
        return Result.success()
    }

    companion object {
        fun schedulePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "geofence_resync",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<ResyncWorker>(6, TimeUnit.HOURS).build(),
            )
        }

        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "geofence_resync_now",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ResyncWorker>().build(),
            )
        }
    }
}
