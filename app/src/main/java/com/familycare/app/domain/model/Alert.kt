package com.familycare.app.domain.model

enum class AlertLevel {
    CRITICAL,
    WARNING,
    INFO
}

enum class AlertType {
    SOS_EMERGENCY,
    THRESHOLD_EXCEEDED,
    MEDICATION_MISSED,
    APPOINTMENT_REMINDER
}

data class Alert(
    val id: String = "",
    val familyId: String = "",
    val targetUserId: String = "",
    val targetUserName: String = "",
    val type: AlertType = AlertType.THRESHOLD_EXCEEDED,
    val level: AlertLevel = AlertLevel.WARNING,
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isResolved: Boolean = false,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val memberId: String get() = targetUserId
    val memberName: String get() = targetUserName
    val alertType: AlertType get() = type
    val alertLevel: AlertLevel get() = level
}
