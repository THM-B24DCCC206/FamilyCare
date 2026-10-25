package com.familycare.app.domain.model

/**
 * Trạng thái kết nối Bluetooth Low Energy (BLE) với Đồng hồ thông minh
 */
enum class SmartwatchConnectionState {
    DISCONNECTED,   // Chưa kết nối
    SCANNING,       // Đang quét tìm thiết bị xung quanh
    CONNECTING,     // Đang thiết lập kết nối GATT
    CONNECTED,      // Đã kết nối thành công, sẵn sàng nhận dữ liệu
    SYNCING,        // Đang đồng bộ dữ liệu vào hồ sơ sức khỏe
    ERROR           // Lỗi kết nối hoặc mất tín hiệu Bluetooth
}

/**
 * Thông tin thiết bị đồng hồ thông minh Bluetooth
 */
data class SmartwatchDevice(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val isBonded: Boolean = false,
    val deviceType: String = "Smartwatch",
    val batteryPercent: Int = 85
)

/**
 * Bộ thông số sinh tồn đo được trực tiếp từ Đồng hồ thông minh
 */
data class SmartwatchTelemetry(
    val heartRate: Int = 0,                 // Nhịp tim thời gian thực (BPM)
    val systolicBp: Int = 0,                // Huyết áp tâm thu (mmHg)
    val diastolicBp: Int = 0,               // Huyết áp tâm trương (mmHg)
    val bloodSugar: Double = 0.0,           // Đường huyết (mg/dL)
    val temperature: Double = 0.0,          // Thân nhiệt (°C)
    val spO2: Int = 0,                      // Nồng độ oxy trong máu SpO2 (%)
    val steps: Int = 0,                     // Số bước chân trong ngày
    val batteryLevel: Int = 85,             // Mức pin (%)
    val lastSyncTime: Long = System.currentTimeMillis(),
    val deviceName: String = "",
    val isRealTimeMeasuring: Boolean = false
) {
    val isAvailable: Boolean
        get() = heartRate > 0 || systolicBp > 0 || temperature > 0.0
}
