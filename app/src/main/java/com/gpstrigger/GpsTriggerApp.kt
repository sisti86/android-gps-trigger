package com.gpstrigger

import android.app.Application
import com.gpstrigger.check.DayChangeReceiver
import com.gpstrigger.check.LocationChecks
import com.gpstrigger.data.AppSettings
import com.gpstrigger.data.ZoneStore
import com.gpstrigger.engine.Notifications
import java.io.File
import org.osmdroid.config.Configuration

class GpsTriggerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings.init(this)
        ZoneStore.init(this)
        Notifications.createChannels(this)
        LocationChecks.ensureScheduled(this)
        DayChangeReceiver.schedule(this)

        Configuration.getInstance().apply {
            load(this@GpsTriggerApp, getSharedPreferences("osmdroid", MODE_PRIVATE))
            // The OSM tile policy requires an identifying user agent.
            userAgentValue = packageName
            // Keep the tile cache in app storage so no storage permission is needed.
            osmdroidBasePath = File(filesDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }
}
