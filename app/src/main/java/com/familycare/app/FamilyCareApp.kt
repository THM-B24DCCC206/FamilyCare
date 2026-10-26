
package com.familycare.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class FamilyCareApp : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(NotificationManager::class.java)

            // Kênh cảnh báo khẩn cấp SOS
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Cảnh báo khẩn cấp SOS",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Kênh gửi thông báo khẩn cấp khi người thân té ngã hoặc bấm nút SOS"
                enableVibration(true)
            }

            // Kênh nhắc lịch uống thuốc
            val medicationChannel = NotificationChannel(
                CHANNEL_MEDICATION,
                "Nhắc nhở uống thuốc",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo nhắc uống thuốc đúng giờ"
                enableVibration(true)
            }

            // Kênh cảnh báo chỉ số bất thường
            val healthAlertChannel = NotificationChannel(
                CHANNEL_HEALTH_ALERT,
                "Cảnh báo chỉ số sức khỏe",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description =
                    "Thông báo khi chỉ số đo vượt ngưỡng an toàn"
            }

            // Kênh nhắc lịch khám
            val appointmentChannel = NotificationChannel(
                CHANNEL_APPOINTMENT,
                "Nhắc lịch khám",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Thông báo nhắc người dùng trước giờ khám bệnh"
                enableVibration(true)
            }

            // Đăng ký tất cả các kênh thông báo
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
        const val CHANNEL_EMERGENCY = "emergency_channel"
        const val CHANNEL_MEDICATION = "medication_channel"
        const val CHANNEL_HEALTH_ALERT = "health_alert_channel"
        const val CHANNEL_APPOINTMENT = "appointment_channel"
    }
}
