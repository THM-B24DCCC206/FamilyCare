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
    val isCurrentUserOwner: Boolean = false,
    val recipientCardTitle: String = "CHỦ GIA ĐÌNH TIẾP NHẬN SOS",
    val recipientCardTitleActive: String = "ĐÃ BÁO CHO CHỦ GIA ĐÌNH",
    val recipientDescription: String = "Khi kích hoạt SOS, hệ thống sẽ tự động gửi thông báo đỏ, rung chuông cực đại và tọa độ GPS đến người thân.",
    val recipientDescriptionActive: String = "✅ Đã gửi tín hiệu khẩn cấp ưu tiên cao nhất (FCM High Priority) kèm tọa độ GPS trực tiếp đến người thân.",
    val recipientCallButtonText: String = "Gọi Người Thân",
    val familyOwnerName: String = "Đang tải...",
    val familyOwnerRole: String = "Trưởng gia đình",
    val familyOwnerPhone: String = "",
    val isOwnerNotified: Boolean = false,
    val nearbyHospitals: List<NearbyHospital> = emptyList(),
    val emergencyContacts: List<String> = emptyList()
)
