package com.familycare.app.domain.model

enum class AlertLevel {
    INFO,
    WARNING,
    CRITICAL // Cảnh báo đỏ: 3 lần vượt ngưỡng hoặc SOS
}

enum class AlertType {
    THRESHOLD_EXCEEDED,  // Vượt ngưỡng liên tiếp
    SOS_EMERGENCY,       // Nút SOS khẩn cấp
    MEDICATION_MISSED    // Bỏ lỡ uống thuốc
}

data class Alert(
    val id: String = "",
    val familyId: String = "",
    val targetUserId: String = "",
    val targetUserName: String = "",
    val type: AlertType,
    val level: AlertLevel,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val latitude: Double? = null,  // Tọa độ GPS nếu là SOS
    val longitude: Double? = null
)
