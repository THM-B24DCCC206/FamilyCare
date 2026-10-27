package com.familycare.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familycare.app.R
import com.familycare.app.core.emergency.EmergencySirenController
import com.familycare.app.presentation.emergency.EmergencyAlertReceiverActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch

/**
 * Dịch vụ FCM chạy ngầm tiếp nhận thông báo khẩn cấp trên thiết bị người thân (Máy B).
 * Tự động bật còi hú âm lượng tối đa, rung khẩn cấp và kích hoạt màn hình báo động toàn màn hình vượt khóa.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "MyFirebaseMsgService"
        const val CHANNEL_EMERGENCY_SOS = "channel_emergency_sos_high_priority"
        const val NOTIFICATION_ID_SOS = 9999
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM Token mới nhận: $token")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Đã nhận tin nhắn FCM từ: ${message.from}, Data: ${message.data}")

        val type = message.data["type"] ?: ""
        if (type == "EMERGENCY_SOS" || type == "SOS_EMERGENCY") {
            handleEmergencySosMessage(message.data)
        } else {
            // Xử lý các thông báo thông thường khác
            handleGeneralNotification(message)
        }
    }

    /**
     * Xử lý gói tin Cảnh Báo Khẩn Cấp Cấp Độ Đỏ (EMERGENCY_SOS)
     */
    private fun handleEmergencySosMessage(data: Map<String, String>) {
        val senderName = data["senderName"] ?: data["userName"] ?: "Người thân"
        val senderPhone = data["senderPhone"] ?: data["phone"] ?: ""
        val latitude = data["latitude"]?.toDoubleOrNull() ?: 0.0
        val longitude = data["longitude"]?.toDoubleOrNull() ?: 0.0
        val address = data["address"] ?: "Vị trí không xác định"
        val bloodType = data["bloodType"] ?: "--"
        val medicalConditions = data["medicalConditions"] ?: "Không có"
        val allergies = data["allergies"] ?: "Không có"
        val timestamp = data["timestamp"]?.toLongOrNull() ?: System.currentTimeMillis()

        Log.w(TAG, "🚨 PHÁT HIỆN TÍN HIỆU CẤP CỨU SOS TỪ: $senderName tại $address")

        // 1. Ép kênh STREAM_ALARM lên 100% cực đại, phát còi hú lặp vô tận và rung dồn dập
        EmergencySirenController.startSiren(applicationContext)

        // 2. Tạo Intent kích hoạt EmergencyAlertReceiverActivity toàn màn hình
        val fullScreenIntent = Intent(this, EmergencyAlertReceiverActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EmergencyAlertReceiverActivity.EXTRA_SENDER_NAME, senderName)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_SENDER_PHONE, senderPhone)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_LATITUDE, latitude)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_LONGITUDE, longitude)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_ADDRESS, address)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_BLOOD_TYPE, bloodType)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_CONDITIONS, medicalConditions)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_ALLERGIES, allergies)
            putExtra(EmergencyAlertReceiverActivity.EXTRA_TIMESTAMP, timestamp)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID_SOS,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Khởi tạo NotificationChannel khẩn cấp (nếu Android 8.0 trở lên)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val channel = NotificationChannel(
                CHANNEL_EMERGENCY_SOS,
                "Báo Động Cấp Cứu SOS Khẩn Cấp",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh cảnh báo SOS ưu tiên tối đa đánh thức màn hình khi người thân gặp nạn"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 300, 800, 300, 1000)
                setSound(
                    soundUri,
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 4. Tạo Notification với cờ CATEGORY_ALARM, PRIORITY_MAX và setFullScreenIntent
        val notification = NotificationCompat.Builder(this, CHANNEL_EMERGENCY_SOS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🚨 CẢNH BÁO CẤP CỨU GIA ĐÌNH!")
            .setContentText("$senderName đang gặp nguy hiểm tại $address!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 CẢNH BÁO SOS: Người thân $senderName vừa phát tín hiệu cấp cứu khẩn cấp tại: $address. Nhấn để mở bản đồ dẫn đường và hỗ trợ ngay lập tức!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true) // ĐÁNH THỨC MÀN HÌNH KHÓA VƯỢT TRỘI
            .build()

        notificationManager.notify(NOTIFICATION_ID_SOS, notification)

        // 5. Lưu sự kiện khẩn cấp vào Room Database (Trung Tâm Cảnh Báo)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                val db = com.familycare.app.core.database.AppDatabase.getInstance(applicationContext)
                val alertEntity = com.familycare.app.core.database.AlertEntity(
                    memberId = data["userId"] ?: "user_sos",
                    memberName = senderName,
                    alertType = com.familycare.app.domain.model.AlertType.SOS_EMERGENCY,
                    alertLevel = com.familycare.app.domain.model.AlertLevel.CRITICAL,
                    title = "🚨 BÁO ĐỘNG SOS KHẨN CẤP",
                    message = "$senderName đã kích hoạt còi cứu hộ khẩn cấp tại: $address",
                    address = address,
                    latitude = latitude,
                    longitude = longitude,
                    timestamp = timestamp,
                    isRead = false,
                    isResolved = false,
                    familyId = data["familyId"] ?: ""
                )
                db.alertDao().insertAlert(alertEntity)
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi lưu cảnh báo SOS vào cơ sở dữ liệu", e)
            }
        }
    }

    private fun handleGeneralNotification(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "FamilyCare"
        val body = message.notification?.body ?: message.data["body"] ?: "Bạn có một thông báo mới từ gia đình."

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "familycare_general"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo gia đình",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
