package com.gpstrigger.ui

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gpstrigger.data.AppSettings
import com.gpstrigger.ui.theme.GpsTriggerTheme
import com.gpstrigger.ui.theme.isDark

// Same scrims enableEdgeToEdge() uses by default for 3-button navigation.
private val LightScrim = Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkScrim = Color.argb(0x80, 0x1B, 0x1B, 0x1B)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by AppSettings.themeMode.collectAsStateWithLifecycle()
            val dark = themeMode.isDark()
            // Keep the system bar icons readable when the app theme differs from the system one.
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LightScrim, DarkScrim) { dark },
                )
                onDispose { }
            }
            GpsTriggerTheme(dark) {
                GpsTriggerUi()
            }
        }
    }
}

@Composable
private fun GpsTriggerUi(vm: MainViewModel = viewModel()) {
    val zones by vm.zones.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()
    val themeMode by vm.themeMode.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        vm.sync()
        onPauseOrDispose { }
    }

    val draft = vm.draft
    when {
        draft == null -> ZoneListScreen(
            zones = zones,
            activeIds = active.activeZoneIds,
            syncResult = vm.syncResult,
            themeMode = themeMode,
            onThemeModeChange = vm::setThemeMode,
            onAdd = vm::newZone,
            onEdit = vm::edit,
            onToggle = vm::setEnabled,
            onPermissionsChanged = vm::sync,
        )

        vm.picking -> {
            BackHandler(onBack = vm::closePicker)
            MapPickerScreen(
                initial = draft,
                onConfirm = vm::confirmLocation,
                onBack = vm::closePicker,
            )
        }

        else -> {
            BackHandler(onBack = vm::closeEditor)
            ZoneEditScreen(
                draft = draft,
                isNew = vm.original == null,
                isActive = draft.id in active.activeZoneIds,
                onChange = vm::updateDraft,
                onPickLocation = vm::openPicker,
                onSave = vm::save,
                onDelete = vm::delete,
                onSimulate = vm::simulate,
                onBack = vm::closeEditor,
            )
        }
    }
}
