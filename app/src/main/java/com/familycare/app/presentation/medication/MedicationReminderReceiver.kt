
package com.familycare.app.presentation.medication

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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

        // Thời điểm dự kiến của liều thuốc do Scheduler truyền sang.
        val scheduledAt = intent.getLongExtra(
            MedicationAlarmScheduler.EXTRA_SCHEDULED_AT,
            0L
        )

        // Android 13 trở lên cần quyền thông báo để hiển thị thông báo.
        val hasNotificationPermission =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

        if (hasNotificationPermission) {
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
                ContextCompat.startForegroundService(
                    context,
                    serviceIntent
                )
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Không thể khởi chạy dịch vụ chuông nhắc thuốc.",
                    e
                )
            }
        } else {
            Log.w(TAG, "Chưa được cấp quyền thông báo.")
        }

        // Đặt lịch kiểm tra quá hạn 30 phút sau giờ uống dự kiến.
        // Chỉ đặt khi Scheduler đã cung cấp thời điểm hợp lệ.
        if (scheduledAt > 0L) {
            scheduleOverdueCheck(
                context = context,
                medicineId = medicineId,
                medicineName = medicineName,
                time = time,
                scheduledAt = scheduledAt
            )
        } else {
            Log.w(
                TAG,
                "Thiếu scheduledAt; không thể đặt lịch kiểm tra quá hạn."
            )
        }

        // Tiếp tục lên lịch nhắc thuốc vào ngày tiếp theo.
        MedicationAlarmScheduler(context).schedule(
            medicineId = medicineId,
            medicineName = medicineName,
            dosage = dosage,
            times = listOf(time)
        )
    }

    private fun scheduleOverdueCheck(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        scheduledAt: Long
    ) {
        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val overdueAt = scheduledAt + OVERDUE_DELAY_MILLIS

        // Nếu thời điểm quá hạn đã qua thì không tạo báo thức lỗi thời.
        if (overdueAt <= System.currentTimeMillis()) {
            Log.w(
                TAG,
                "Thời điểm kiểm tra quá hạn đã qua: $medicineName"
            )
            return
        }

        val overdueIntent = Intent(
            context,
            MedicationOverdueReceiver::class.java
        ).apply {
            action = ACTION_CHECK_OVERDUE
            putExtra(
                MedicationAlarmScheduler.EXTRA_MEDICINE_ID,
                medicineId
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_MEDICINE_NAME,
                medicineName
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_TIME,
                time
            )
            putExtra(
                MedicationAlarmScheduler.EXTRA_SCHEDULED_AT,
                scheduledAt
            )
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            "$medicineId|$scheduledAt|overdue".hashCode(),
            overdueIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                // Không có quyền báo thức chính xác: dùng báo thức không chính xác.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    overdueAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    overdueAt,
                    pendingIntent
                )
            }

            Log.d(
                TAG,
                "Đã đặt lịch kiểm tra quá hạn cho $medicineName."
            )
        } catch (e: SecurityException) {
            Log.e(
                TAG,
                "Không đặt được lịch kiểm tra quá hạn.",
                e
            )

            // Phương án dự phòng khi hệ thống từ chối báo thức chính xác.
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    overdueAt,
                    pendingIntent
                )
            } catch (fallbackError: Exception) {
                Log.e(
                    TAG,
                    "Không đặt được cả báo thức dự phòng.",
                    fallbackError
                )
            }
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Lỗi khi đặt lịch kiểm tra quá hạn.",
                e
            )
        }
    }

    companion object {
        private const val TAG = "MedicationReminder"

        private const val OVERDUE_DELAY_MILLIS =
            30 * 60 * 1000L

        const val ACTION_CHECK_OVERDUE =
            "com.familycare.app.ACTION_CHECK_MEDICATION_OVERDUE"
    }
}
