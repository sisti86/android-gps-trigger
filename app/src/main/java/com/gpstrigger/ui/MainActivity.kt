package com.gpstrigger.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gpstrigger.ui.theme.GpsTriggerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GpsTriggerTheme {
                GpsTriggerUi()
            }
        }
    }
}

@Composable
private fun GpsTriggerUi(vm: MainViewModel = viewModel()) {
    val zones by vm.zones.collectAsStateWithLifecycle()
    val active by vm.active.collectAsStateWithLifecycle()

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
