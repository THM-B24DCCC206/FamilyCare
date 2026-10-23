package com.familycare.app.presentation.emergency

import com.familycare.app.core.emergency.EmergencyPayload

sealed interface SosStatus {
    object Idle : SosStatus
    object LocatingAndActivating : SosStatus
    data class Activated(val payload: EmergencyPayload) : SosStatus
    data class Error(val message: String) : SosStatus
}

data class EmergencyUiState(
    val sosStatus: SosStatus = SosStatus.Idle,
    val isHolding: Boolean = false,
    val holdProgress: Float = 0f, // 0.0f đến 1.0f (tương đương 2 giây)
    val isSirenPlaying: Boolean = false,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val targetUserName: String = "Bác Nguyễn Văn Nam",
    val familyId: String = "fam_123456",
    val emergencyContacts: List<String> = listOf("Bác sĩ gia đình: 0987.654.321", "Con trai (Nguyễn Hoàng): 0912.345.678")
)
