
package com.familycare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class FamilyCareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        fetchFcmToken()
    }

    private fun fetchFcmToken() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Lấy FCM token thành công")
                    Log.d(TAG, "FCM token: ${task.result}")
                } else {
                    Log.e(TAG, "Không lấy được FCM token", task.exception)
                }
            }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(NotificationManager::class.java)

            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Cảnh báo khẩn cấp SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Kênh gửi thông báo khẩn cấp khi người thân té ngã hoặc bấm nút SOS"
                enableVibration(true)
            }

            val medicationChannel = NotificationChannel(
                CHANNEL_MEDICATION,
                "Nhắc nhở uống thuốc",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo nhắc uống thuốc đúng giờ"
                enableVibration(true)
            }

            val healthAlertChannel = NotificationChannel(
                CHANNEL_HEALTH_ALERT,
                "Cảnh báo chỉ số sức khỏe",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Thông báo khi chỉ số đo vượt ngưỡng an toàn"
            }

            val appointmentChannel = NotificationChannel(
                CHANNEL_APPOINTMENT,
                "Nhắc lịch khám",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Thông báo nhắc người dùng trước giờ khám bệnh"
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(
                    emergencyChannel,
                    medicationChannel,
                    healthAlertChannel,
                    appointmentChannel
                )
            )
        }
    }

    companion object {
        private const val TAG = "FamilyCareFCM"

        const val CHANNEL_EMERGENCY = "emergency_channel"
        const val CHANNEL_MEDICATION = "medication_channel"
        const val CHANNEL_HEALTH_ALERT = "health_alert_channel"
        const val CHANNEL_APPOINTMENT = "appointment_channel"
    }
}
