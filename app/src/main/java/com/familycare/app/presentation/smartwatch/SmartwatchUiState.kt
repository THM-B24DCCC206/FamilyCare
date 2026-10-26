package com.familycare.app.presentation.smartwatch

import com.familycare.app.domain.model.SmartwatchConnectionState
import com.familycare.app.domain.model.SmartwatchDevice
import com.familycare.app.domain.model.SmartwatchTelemetry

data class SmartwatchUiState(
    val connectionState: SmartwatchConnectionState = SmartwatchConnectionState.DISCONNECTED,
    val connectedDevice: SmartwatchDevice? = null,
    val discoveredDevices: List<SmartwatchDevice> = emptyList(),
    val telemetry: SmartwatchTelemetry = SmartwatchTelemetry(),
    val isAutoSyncEnabled: Boolean = true,
    val syncIntervalSeconds: Int = 10,
    val lastSyncMessage: String = "Chưa đồng bộ",
    val syncCount: Int = 0,
    val isSyncing: Boolean = false,
    val isBluetoothEnabled: Boolean = true
)
