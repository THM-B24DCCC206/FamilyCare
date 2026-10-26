
package com.familycare.app.presentation.medication

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

class MedicationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_ID
        ) ?: return

        val medicineName = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_NAME
        ) ?: "Thuốc"

        val dosage = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_DOSAGE
        ) ?: ""

        val time = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_TIME
        ) ?: return

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

        // Khởi chạy dịch vụ chuông báo thức.
        val serviceIntent = Intent(
            context,
            MedicationAlarmService::class.java
        ).apply {
            action = MedicationAlarmService.ACTION_START
            putExtra(
                MedicationAlarmScheduler.EXTRA_MEDICINE_ID,
                medicineId
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_MEDICINE_NAME,
                medicineName
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_DOSAGE,
                dosage
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_TIME,
                time
            )
        }

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            // Không để lỗi khởi chạy service làm crash receiver.
            android.util.Log.e(
                "MedicationReminder",
                "Không thể khởi chạy dịch vụ chuông",
                e
            )
            return
        }

        // Lên lịch nhắc lại vào ngày tiếp theo.
        MedicationAlarmScheduler(context).schedule(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            times = listOf(time)
        )
    }
}
