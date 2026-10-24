
package com.familycare.app.presentation.medication

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.familycare.app.FamilyCareApp
import com.familycare.app.R

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_ID
        ) ?: return
        val name = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_NAME
        ) ?: "Thuốc"
        val dosage = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_DOSAGE
        ) ?: ""
        val time = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_TIME
        ) ?: return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notification = if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, FamilyCareApp.CHANNEL_MEDICATION)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đến giờ uống thuốc")
            .setContentText("$name — $dosage")
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "Đã đến giờ uống $name. Liều lượng: $dosage. " +
                        "Giờ đã cài: $time."
                )
            )
            .setAutoCancel(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .build()

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        manager.notify("$id-$time".hashCode(), notification)

        // Đặt lại lịch cho ngày mai.
        MedicationAlarmScheduler(context).schedule(
            medicineId = id,
            medicineName = name,
            dosage = dosage,
            times = listOf(time)
        )
    }
}
