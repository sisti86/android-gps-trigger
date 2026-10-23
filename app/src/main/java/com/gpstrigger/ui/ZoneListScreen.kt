package com.gpstrigger.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gpstrigger.PermissionState
import com.gpstrigger.Permissions
import com.gpstrigger.R
import com.gpstrigger.data.ALL_DAYS
import com.gpstrigger.data.RingerSetting
import com.gpstrigger.data.ThemeMode
import com.gpstrigger.data.Zone
import com.gpstrigger.data.ZoneActions
import com.gpstrigger.geofence.SyncResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneListScreen(
    zones: List<Zone>,
    activeIds: List<String>,
    syncResult: SyncResult?,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAdd: () -> Unit,
    onEdit: (Zone) -> Unit,
    onToggle: (Zone, Boolean) -> Unit,
    onPermissionsChanged: () -> Unit,
) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(Permissions.read(context)) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        permissions = Permissions.read(context)
        onPauseOrDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.zones_title)) },
                actions = {
                    IconButton(onClick = { showThemeDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.theme_title))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_zone))
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SetupCard(
                    permissions = permissions,
                    syncResult = syncResult,
                    onChanged = {
                        permissions = Permissions.read(context)
                        onPermissionsChanged()
                    },
                )
            }
            if (zones.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_zones),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 32.dp),
                    )
                }
            }
            items(zones, key = { it.id }) { zone ->
                ZoneCard(
                    zone = zone,
                    inside = zone.id in activeIds,
                    onClick = { onEdit(zone) },
                    onToggle = { onToggle(zone, it) },
                )
            }
        }
    }

    if (showThemeDialog) {
        ThemeDialog(
            selected = themeMode,
            onSelect = onThemeModeChange,
            onDismiss = { showThemeDialog = false },
        )
    }
}

@Composable
private fun ThemeDialog(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, onDismiss: () -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_title)) },
        text = {
            Column(Modifier.selectableGroup()) {
                options.forEach { (mode, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = mode == selected, role = Role.RadioButton) { onSelect(mode) }
                            .padding(vertical = 12.dp),
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Text(stringResource(label), Modifier.padding(start = 16.dp), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

@Composable
private fun ZoneCard(zone: Zone, inside: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(zone.name, style = MaterialTheme.typography.titleMedium)
                    if (inside) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text(stringResource(R.string.inside)) })
                    }
                }
                Text(
                    text = zone.address
                        ?: stringResource(R.string.coordinates, zone.latitude, zone.longitude),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
                Text(
                    text = if (zone.enabled) actionsSummary(LocalContext.current, zone.actions)
                    else stringResource(R.string.disabled),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
                if (zone.enabled) {
                    Text(
                        text = scheduleSummary(LocalContext.current, zone),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = zone.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun actionsSummary(context: Context, actions: ZoneActions): String {
    val parts = buildList {
        actions.ringVolume?.let { add(context.getString(R.string.summary_ring, it)) }
        actions.notificationVolume?.let { add(context.getString(R.string.summary_notification, it)) }
        actions.callVolume?.let { add(context.getString(R.string.summary_call, it)) }
        when (actions.ringerMode) {
            RingerSetting.NORMAL -> add(context.getString(R.string.ringer_normal))
            RingerSetting.VIBRATE -> add(context.getString(R.string.ringer_vibrate))
            RingerSetting.SILENT -> add(context.getString(R.string.ringer_silent))
            null -> Unit
        }
        when (actions.wifiEnabled) {
            true -> add(context.getString(R.string.summary_wifi_on))
            false -> add(context.getString(R.string.summary_wifi_off))
            null -> Unit
        }
    }
    return parts.joinToString(" · ").ifEmpty { context.getString(R.string.summary_none) }
}

private fun scheduleSummary(context: Context, zone: Zone): String {
    val locale = context.resources.configuration.locales[0]
    val days = if (zone.activeDays == ALL_DAYS) {
        context.getString(R.string.summary_every_day)
    } else {
        weekDays(locale).filter { it.value in zone.activeDays }.joinToString(" ") { it.shortName(locale) }
    }
    return days + " · " + context.getString(R.string.summary_check_every, zone.checkIntervalMinutes)
}

@Composable
private fun SetupCard(permissions: PermissionState, syncResult: SyncResult?, onChanged: () -> Unit) {
    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { onChanged() }
    val showSyncError = permissions.fineLocation &&
        (syncResult == SyncResult.ERROR || syncResult == SyncResult.LOCATION_UNAVAILABLE)
    if (permissions.allRequiredGranted && permissions.notifications && !showSyncError) return

    fun open(intent: Intent) = context.startActivity(intent)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Warning, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.titleMedium)
            }
            if (!permissions.fineLocation) {
                SetupItem(R.string.perm_location_title, R.string.perm_location_desc, R.string.action_grant) {
                    locationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }
            } else if (!permissions.backgroundLocation) {
                SetupItem(R.string.perm_background_title, R.string.perm_background_desc, R.string.action_open_settings) {
                    open(Permissions.appSettingsIntent(context))
                }
            }
            if (!permissions.locationEnabled) {
                SetupItem(R.string.perm_location_off_title, R.string.perm_location_off_desc, R.string.action_turn_on) {
                    open(Permissions.locationSettingsIntent())
                }
            }
            if (!permissions.dndAccess) {
                SetupItem(R.string.perm_dnd_title, R.string.perm_dnd_desc, R.string.action_grant) {
                    open(Permissions.dndSettingsIntent())
                }
            }
            if (!permissions.notifications) {
                SetupItem(R.string.perm_notifications_title, R.string.perm_notifications_desc, R.string.action_open_settings) {
                    open(Permissions.notificationSettingsIntent(context))
                }
            }
            if (showSyncError && permissions.locationEnabled) {
                Text(stringResource(R.string.sync_error), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SetupItem(title: Int, description: Int, action: Int, onClick: () -> Unit) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(description), style = MaterialTheme.typography.bodyMedium)
        FilledTonalButton(onClick = onClick, modifier = Modifier.padding(top = 8.dp)) {
            Text(stringResource(action))
        }
    }
}
