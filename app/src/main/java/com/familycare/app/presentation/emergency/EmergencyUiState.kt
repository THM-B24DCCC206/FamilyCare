package com.familycare.app.presentation.emergency

import com.familycare.app.core.emergency.EmergencyPayload
import com.familycare.app.domain.model.NearbyHospital

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
    val isLocating: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAddress: String = "Đang xác định vị trí GPS...",
    val targetUserName: String = "Bác Nguyễn Văn Nam",
    val familyId: String = "fam_123456",
    val familyOwnerName: String = "Bác Nguyễn Văn Nam",
    val familyOwnerRole: String = "Chủ hộ gia đình",
    val familyOwnerPhone: String = "0912.345.678",
    val isOwnerNotified: Boolean = false,
    val nearbyHospitals: List<NearbyHospital> = emptyList(),
    val emergencyContacts: List<String> = listOf(
        "Chủ hộ (Bác Nguyễn Văn Nam): 0912.345.678",
        "Bác sĩ gia đình (BS. Tuấn): 0987.654.321",
        "Con trai (Nguyễn Hoàng): 0903.112.233"
    )
)
