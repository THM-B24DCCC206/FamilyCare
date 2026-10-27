package com.familycare.app.core.emergency

import java.util.UUID

/**
 * Payload chứa dữ liệu khẩn cấp gửi qua Firebase Cloud Messaging (FCM Data Message)
 * Đồng bộ giữa Thiết bị phát (Máy A) và Thiết bị tiếp nhận (Máy B)
 */
data class EmergencyPayload(
    val eventId: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val senderName: String = "",
    val senderPhone: String = "",
    val familyId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val bloodType: String = "",
    val medicalConditions: String = "",
    val allergies: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val familyOwnerName: String = "",
    val familyOwnerPhone: String = "",
    val googleMapsUrl: String = "https://maps.google.com/?q=$latitude,$longitude"
) {
    // Secondary constructor tương thích ngược với code cũ & unit tests dùng userName
    constructor(
        eventId: String = UUID.randomUUID().toString(),
        userId: String = "",
        userName: String,
        familyId: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        address: String = "Vị trí không xác định",
        familyOwnerName: String = "",
        familyOwnerPhone: String = ""
    ) : this(
        eventId = eventId,
        userId = userId,
        senderName = userName,
        senderPhone = "",
        familyId = familyId,
        latitude = latitude,
        longitude = longitude,
        address = address,
        familyOwnerName = familyOwnerName,
        familyOwnerPhone = familyOwnerPhone
    )

    // Thuộc tính tương thích ngược cho codebase cũ
    val userName: String get() = senderName

    /**
     * Chuyển đổi thành cấu trúc Map payload gửi đến Firebase Cloud Messaging (FCM Data Message)
     */
    fun toFcmDataMap(): Map<String, String> {
        return mapOf(
            "type" to "EMERGENCY_SOS",
            "senderName" to senderName,
            "userName" to senderName,
            "senderPhone" to senderPhone,
            "latitude" to latitude.toString(),
            "longitude" to longitude.toString(),
            "address" to address,
            "bloodType" to bloodType,
            "medicalConditions" to medicalConditions,
            "allergies" to allergies,
            "timestamp" to timestamp.toString(),
            "eventId" to eventId,
            "userId" to userId,
            "familyId" to familyId,
            "familyOwner" to "$familyOwnerName ($familyOwnerPhone)",
            "mapsUrl" to googleMapsUrl,
            "priority" to "high",
            "title" to "🚨 CẢNH BÁO SOS: $senderName CẦN TRỢ GIÚP KHẨN CẤP!",
            "body" to "$senderName vừa kích hoạt SOS tại $address ($latitude, $longitude). Nhấn để mở cứu hộ ngay!"
        )
    }
}
