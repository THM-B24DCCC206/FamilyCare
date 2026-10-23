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
    val timestamp: Long = System.currentTimeMillis()
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
            "title" to "🚨 CẢNH BÁO SOS: $userName CẦN TRỢ GIÚP KHẨN CẤP!",
            "body" to "$userName vừa kích hoạt nút SOS khẩn cấp tại vị trí $latitude, $longitude. Nhấn để mở Google Maps ngay!"
        )
    }
}
