package com.familycare.app.core.network

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.familycare.app.FamilyCareApp
import com.familycare.app.MainActivity
import com.familycare.app.R

/**
 * Helper điều phối thông báo đẩy khẩn cấp độ ưu tiên cao (High-Priority Heads-up Notification)
 * Phục vụ Module đinh 3 của Thành viên 2 (FCM Simulation & Emergency Alert).
 */
class EmergencyNotificationHelper(
    private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun showEmergencySosNotification(
        userName: String,
        latitude: Double,
        longitude: Double
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FamilyCareApp.CHANNEL_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("🚨 CẢNH BÁO SOS: $userName CẦN TRỢ GIÚP!")
            .setContentText("Người thân vừa kích hoạt SOS tại tọa độ $latitude, $longitude. Nhấn để mở bản đồ ngay!")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Người thân $userName vừa kích hoạt SOS khẩn cấp tại tọa độ $latitude, $longitude. Còi hú đang phát âm lượng tối đa. Vui lòng liên hệ và hỗ trợ ngay lập tức!")
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .build()

        notificationManager.notify(NOTIFICATION_ID_SOS, notification)
    }

    fun showThresholdBreachNotification(
        userName: String,
        breachCount: Int,
        latestReading: String
    ) {
        if (!hasNotificationPermission()) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, FamilyCareApp.CHANNEL_HEALTH_ALERT)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("⚠️ CẢNH BÁO ĐỎ: $breachCount LẦN ĐO VƯỢT NGƯỠNG")
            .setContentText("$userName có $breachCount lần đo liên tiếp vượt ngưỡng. Chỉ số gần nhất: $latestReading")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_THRESHOLD, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    companion object {
        const val NOTIFICATION_ID_SOS = 9991
        const val NOTIFICATION_ID_THRESHOLD = 9992
    }
}
