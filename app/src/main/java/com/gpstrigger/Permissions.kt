package com.gpstrigger

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

data class PermissionState(
    val fineLocation: Boolean,
    val backgroundLocation: Boolean,
    val locationEnabled: Boolean,
    val dndAccess: Boolean,
    val notifications: Boolean,
) {
    val allRequiredGranted get() = fineLocation && backgroundLocation && locationEnabled && dndAccess
}

object Permissions {
    fun read(context: Context) = PermissionState(
        fineLocation = hasFineLocation(context),
        backgroundLocation = hasBackgroundLocation(context),
        locationEnabled = LocationManagerCompat.isLocationEnabled(context.getSystemService(LocationManager::class.java)),
        dndAccess = context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted,
        notifications = NotificationManagerCompat.from(context).areNotificationsEnabled(),
    )

    fun hasFineLocation(context: Context) = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    private fun hasBackgroundLocation(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun appSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun locationSettingsIntent() = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)

    fun dndSettingsIntent() = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)

    fun notificationSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
}
