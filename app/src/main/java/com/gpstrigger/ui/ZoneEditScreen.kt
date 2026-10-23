package com.gpstrigger.ui

import android.media.AudioManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.gpstrigger.R
import com.gpstrigger.data.CHECK_INTERVALS_MINUTES
import com.gpstrigger.data.DEFAULT_CHECK_INTERVAL_MINUTES
import com.gpstrigger.data.RingerSetting
import com.gpstrigger.data.Zone
import com.gpstrigger.engine.DeviceSettings
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditScreen(
    draft: Zone,
    isNew: Boolean,
    isActive: Boolean,
    onChange: (Zone) -> Unit,
    onPickLocation: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onSimulate: (enter: Boolean) -> Unit,
    onBack: () -> Unit,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val actions = draft.actions
    val canSave = draft.name.isNotBlank() && draft.hasLocation && draft.activeDays.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (isNew) R.string.new_zone else R.string.edit_zone)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (!isNew) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                    TextButton(onClick = onSave, enabled = canSave) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = draft.name,
                onValueChange = { onChange(draft.copy(name = it)) },
                label = { Text(stringResource(R.string.zone_name)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            Card(onClick = onPickLocation, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null)
                    Column(Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(stringResource(R.string.location), style = MaterialTheme.typography.titleSmall)
                        if (draft.hasLocation) {
                            Text(
                                draft.address ?: stringResource(R.string.coordinates, draft.latitude, draft.longitude),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                stringResource(R.string.radius_value, draft.radiusMeters.roundToInt()),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            stringResource(R.string.change_location),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.zone_enabled), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                Switch(checked = draft.enabled, onCheckedChange = { onChange(draft.copy(enabled = it)) })
            }

            HorizontalDivider()
            Text(stringResource(R.string.schedule_title), style = MaterialTheme.typography.titleMedium)

            DaysSetting(draft.activeDays) { onChange(draft.copy(activeDays = it)) }
            Column {
                ChoiceSetting(
                    label = stringResource(R.string.check_interval),
                    options = CHECK_INTERVALS_MINUTES.map { it to stringResource(R.string.minutes_value, it) },
                    selected = draft.checkIntervalMinutes,
                    onSelect = { onChange(draft.copy(checkIntervalMinutes = it ?: DEFAULT_CHECK_INTERVAL_MINUTES)) },
                )
                Text(
                    stringResource(R.string.check_interval_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.actions_title), style = MaterialTheme.typography.titleMedium)

            VolumeSetting(R.string.ring_volume, AudioManager.STREAM_RING, actions.ringVolume) {
                onChange(draft.copy(actions = actions.copy(ringVolume = it)))
            }
            VolumeSetting(R.string.notification_volume, AudioManager.STREAM_NOTIFICATION, actions.notificationVolume) {
                onChange(draft.copy(actions = actions.copy(notificationVolume = it)))
            }
            VolumeSetting(R.string.call_volume, AudioManager.STREAM_VOICE_CALL, actions.callVolume) {
                onChange(draft.copy(actions = actions.copy(callVolume = it)))
            }
            ChoiceSetting(
                label = stringResource(R.string.ringer_mode),
                options = listOf(
                    null to stringResource(R.string.no_change),
                    RingerSetting.NORMAL to stringResource(R.string.ringer_normal),
                    RingerSetting.VIBRATE to stringResource(R.string.ringer_vibrate),
                    RingerSetting.SILENT to stringResource(R.string.ringer_silent),
                ),
                selected = actions.ringerMode,
                onSelect = { onChange(draft.copy(actions = actions.copy(ringerMode = it))) },
            )
            ChoiceSetting(
                label = stringResource(R.string.wifi),
                options = listOf(
                    null to stringResource(R.string.no_change),
                    true to stringResource(R.string.wifi_on),
                    false to stringResource(R.string.wifi_off),
                ),
                selected = actions.wifiEnabled,
                onSelect = { onChange(draft.copy(actions = actions.copy(wifiEnabled = it))) },
            )
            Text(
                stringResource(R.string.restore_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Button(onClick = onSave, enabled = canSave, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }

            if (!isNew) {
                HorizontalDivider()
                Text(stringResource(R.string.test_title), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onSimulate(true) }, enabled = !isActive, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.simulate_enter))
                    }
                    OutlinedButton(onClick = { onSimulate(false) }, enabled = isActive, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.simulate_exit))
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_confirm_title)) },
            text = { Text(stringResource(R.string.delete_confirm_text, draft.name)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** A switch to enable the setting plus a slider over the stream's real volume steps. */
@Composable
private fun VolumeSetting(label: Int, stream: Int, value: Int?, onChange: (Int?) -> Unit) {
    val context = LocalContext.current
    val device = remember { DeviceSettings(context) }
    val range = remember(stream) { device.range(stream) }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Switch(
                checked = value != null,
                onCheckedChange = { checked -> onChange(if (checked) device.volume(stream).coerceIn(range) else null) },
            )
        }
        if (value != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onChange(it.roundToInt()) },
                    valueRange = range.first.toFloat()..range.last.toFloat(),
                    steps = (range.last - range.first - 1).coerceAtLeast(0),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    stringResource(R.string.volume_value, value, range.last),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DaysSetting(selected: Set<Int>, onChange: (Set<Int>) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val days = remember(locale) { weekDays(locale) }

    Column {
        Text(stringResource(R.string.active_days), style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                val iso = day.value
                FilterChip(
                    selected = iso in selected,
                    onClick = { onChange(if (iso in selected) selected - iso else selected + iso) },
                    label = { Text(day.shortName(locale)) },
                )
            }
        }
        if (selected.isEmpty()) {
            Text(
                stringResource(R.string.no_days_selected),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceSetting(label: String, options: List<Pair<T?, String>>, selected: T?, onSelect: (T?) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (option, text) ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(text) })
            }
        }
    }
}
