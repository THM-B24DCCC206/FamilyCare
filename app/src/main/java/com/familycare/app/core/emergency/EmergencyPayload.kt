package com.familycare.app.core.emergency

/**
 * Payload chứa dữ liệu khẩn cấp gửi qua Firebase Cloud Messaging (FCM)
 */
data class EmergencyPayload(
    val eventId: String,
    val userId: String,
    val userName: String,
    val familyId: String,
    val latitude: Double,
    val longitude: Double,
    val googleMapsUrl: String = "https://maps.google.com/?q=$latitude,$longitude",
    val timestamp: Long = System.currentTimeMillis(),
    val address: String = "Tọa độ GPS ($latitude, $longitude)",
    val accuracyMeters: Float = 5.0f,
    val familyOwnerName: String = "Bác Nguyễn Văn Nam (Chủ hộ)",
    val familyOwnerPhone: String = "0912.345.678"
) {
    /**
     * Chuyển đổi thành cấu trúc Map payload gửi đến Firebase Cloud Messaging (FCM Data Message)
     */
    fun toFcmDataMap(): Map<String, String> {
        return mapOf(
            "type" to "SOS_EMERGENCY",
            "priority" to "high",
            "eventId" to eventId,
            "userId" to userId,
            "userName" to userName,
            "familyId" to familyId,
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "mapsUrl" to googleMapsUrl,
            "timestamp" to timestamp.toString(),
            "address" to address,
            "accuracy" to "±${accuracyMeters.toInt()}m",
            "familyOwner" to "$familyOwnerName ($familyOwnerPhone)",
            "title" to "🚨 CẢNH BÁO SOS: $userName CẦN TRỢ GIÚP KHẨN CẤP!",
            "body" to "$userName vừa kích hoạt SOS tại $address ($latitude, $longitude). Đã thông báo cho Chủ hộ $familyOwnerName. Nhấn để mở Google Maps ngay!"
        )
    }
}
