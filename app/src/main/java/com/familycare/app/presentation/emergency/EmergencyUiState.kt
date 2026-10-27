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
    val isSirenPlaying: Boolean = false,
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val currentAddress: String = "Đang xác định vị trí thực tế...",
    val targetUserId: String = "",
    val targetUserName: String = "Đang tải hồ sơ...",
    val targetUserPhone: String = "",
    val bloodType: String = "--",
    val chronicDiseases: String = "",
    val drugAllergies: String = "",
    val familyId: String = "",
    val familyOwnerName: String = "Đang tải...",
    val familyOwnerRole: String = "Trưởng gia đình",
    val familyOwnerPhone: String = "",
    val isOwnerNotified: Boolean = false,
    val nearbyHospitals: List<NearbyHospital> = emptyList(),
    val emergencyContacts: List<String> = emptyList()
)
