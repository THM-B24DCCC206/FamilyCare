package com.familycare.app.presentation.smartwatch

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.ble.SmartwatchBleManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricDao
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.domain.model.SmartwatchConnectionState
import com.familycare.app.domain.model.SmartwatchDevice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel quản lý kết nối Đồng hồ thông minh qua Bluetooth Low Energy (BLE)
 * và tự động chuyển các chỉ số đo đạc thời gian thực vào cơ sở dữ liệu sức khỏe của App.
 */
class SmartwatchViewModel(
    private val bleManager: SmartwatchBleManager,
    private val healthMetricDao: HealthMetricDao,
    private val targetUserId: String = "user_me"
) : ViewModel() {

    private val _uiState = MutableStateFlow(SmartwatchUiState())
    val uiState: StateFlow<SmartwatchUiState> = _uiState.asStateFlow()

    private var autoSyncJob: Job? = null

    init {
        // Lắng nghe trạng thái từ BLE Manager
        viewModelScope.launch {
            bleManager.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
                if (state == SmartwatchConnectionState.CONNECTED) {
                    startAutoSyncLoop()
                } else {
                    stopAutoSyncLoop()
                }
            }
        }

        viewModelScope.launch {
            bleManager.discoveredDevices.collect { list ->
                _uiState.update { it.copy(discoveredDevices = list) }
            }
        }

        viewModelScope.launch {
            bleManager.currentDevice.collect { dev ->
                _uiState.update { it.copy(connectedDevice = dev) }
            }
        }

        viewModelScope.launch {
            bleManager.telemetry.collect { tele ->
                _uiState.update { it.copy(telemetry = tele) }
            }
        }

        // Tự động kích hoạt tìm kiếm ban đầu
        bleManager.startScan()
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(device: SmartwatchDevice) {
        bleManager.connectDevice(device)
    }

    fun disconnect() {
        bleManager.disconnect()
        stopAutoSyncLoop()
    }

    fun toggleAutoSync(enabled: Boolean) {
        _uiState.update { it.copy(isAutoSyncEnabled = enabled) }
        if (enabled && _uiState.value.connectionState == SmartwatchConnectionState.CONNECTED) {
            startAutoSyncLoop()
        } else {
            stopAutoSyncLoop()
        }
    }

    /**
     * Đồng bộ trực tiếp tất cả các thông số vừa nhận từ đồng hồ vào CSDL ứng dụng
     */
    fun syncNow() {
        val telemetry = _uiState.value.telemetry
        if (!telemetry.isAvailable) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }
            val now = System.currentTimeMillis()
            val deviceLabel = telemetry.deviceName.ifBlank { "Smartwatch BLE" }
            val timeString = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(now))

            val entities = mutableListOf<HealthMetricEntity>()

            // 1. Nhịp tim
            if (telemetry.heartRate > 0) {
                entities.add(
                    HealthMetricEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetUserId,
                        type = MetricType.HEART_RATE.name,
                        valuePrimary = telemetry.heartRate.toDouble(),
                        unit = "BPM",
                        timestamp = now,
                        notes = "Đồng bộ Bluetooth từ $deviceLabel"
                    )
                )
            }

            // 2. Huyết áp
            if (telemetry.systolicBp > 0) {
                entities.add(
                    HealthMetricEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetUserId,
                        type = MetricType.BLOOD_PRESSURE.name,
                        valuePrimary = telemetry.systolicBp.toDouble(),
                        valueSecondary = telemetry.diastolicBp.toDouble(),
                        unit = "mmHg",
                        timestamp = now,
                        notes = "Đồng bộ Bluetooth từ $deviceLabel"
                    )
                )
            }

            // 3. Thân nhiệt
            if (telemetry.temperature > 0.0) {
                entities.add(
                    HealthMetricEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetUserId,
                        type = MetricType.TEMPERATURE.name,
                        valuePrimary = telemetry.temperature,
                        unit = "°C",
                        timestamp = now,
                        notes = "Đồng bộ Bluetooth từ $deviceLabel"
                    )
                )
            }

            // 4. Đường huyết (nếu có cảm biến quang phổ)
            if (telemetry.bloodSugar > 0.0) {
                entities.add(
                    HealthMetricEntity(
                        id = UUID.randomUUID().toString(),
                        userId = targetUserId,
                        type = MetricType.BLOOD_SUGAR.name,
                        valuePrimary = telemetry.bloodSugar,
                        unit = "mg/dL",
                        timestamp = now,
                        notes = "Đồng bộ Bluetooth từ $deviceLabel"
                    )
                )
            }

            // Ghi batch vào SQLite CSDL
            if (entities.isNotEmpty()) {
                healthMetricDao.insertMetrics(entities)
            }

            _uiState.update {
                it.copy(
                    isSyncing = false,
                    syncCount = it.syncCount + 1,
                    lastSyncMessage = "Đã lưu ${entities.size} chỉ số lúc $timeString"
                )
            }
        }
    }

    private fun startAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(15000L) // Tự động đồng bộ mỗi 15 giây
                if (_uiState.value.isAutoSyncEnabled) {
                    syncNow()
                }
            }
        }
    }

    private fun stopAutoSyncLoop() {
        autoSyncJob?.cancel()
        autoSyncJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoSyncLoop()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val bleManager = SmartwatchBleManager(context.applicationContext)
                return SmartwatchViewModel(bleManager, db.healthMetricDao()) as T
            }
        }
    }
}
