package com.familycare.app.domain.model

enum class MetricType {
    BLOOD_PRESSURE, // Huyết áp (mmHg)
    HEART_RATE,     // Nhịp tim (BPM)
    BLOOD_SUGAR,    // Đường huyết (mg/dL)
    TEMPERATURE     // Thân nhiệt (°C)
}

data class HealthMetric(
    val id: String = "",
    val userId: String = "",
    val type: MetricType,
    val valuePrimary: Double,       // Ví dụ: Huyết áp tâm thu, hoặc Nhịp tim BPM
    val valueSecondary: Double? = null, // Ví dụ: Huyết áp tâm trương
    val unit: String,               // "BPM", "mmHg", "mg/dL", "°C"
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = "",
    val isFromCameraPpg: Boolean = false // Đánh dấu đo từ Camera PPG
)
