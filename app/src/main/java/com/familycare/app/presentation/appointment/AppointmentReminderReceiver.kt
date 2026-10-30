
package com.familycare.app.presentation.appointment

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.familycare.app.FamilyCareApp
import com.familycare.app.R
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppointmentReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Android 13 trở lên cần quyền thông báo.
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val hospitalName = intent.getStringExtra(
            AppointmentAlarmScheduler.EXTRA_HOSPITAL_NAME
        ) ?: "Bệnh viện / phòng khám"

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(
                context,
                FamilyCareApp.CHANNEL_APPOINTMENT
            )
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Sắp đến giờ khám bệnh")
            .setContentText("Bạn có lịch khám tại $hospitalName sau khoảng 1 giờ.")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "Đừng quên chuẩn bị giấy tờ và đến đúng giờ khám tại $hospitalName."
                )
            )
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager

        val appointmentId = intent.getStringExtra(
            AppointmentAlarmScheduler.EXTRA_APPOINTMENT_ID
        ) ?: System.currentTimeMillis().toString()

        notificationManager.notify(
            appointmentId.hashCode(),
            notification
        )

        // Đồng bộ lưu cảnh báo vào chuông thông báo (AlertDao)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val alertDao = db.alertDao()
                alertDao.insertAlert(
                    AlertEntity(
                        familyId = "FAM-8606",
                        memberId = "user_me",
                        memberName = "Người thân",
                        alertType = AlertType.APPOINTMENT_REMINDER,
                        alertLevel = AlertLevel.WARNING,
                        title = "🗓 Sắp đến giờ khám: $hospitalName",
                        message = "Bạn có lịch khám tại $hospitalName sau khoảng 1 giờ. Vui lòng chuẩn bị giấy tờ và đến đúng giờ.",
                        timestamp = System.currentTimeMillis(),
                        isRead = false,
                        isResolved = false
                    )
                )
            } catch (_: Exception) {}
        }
    }
}
