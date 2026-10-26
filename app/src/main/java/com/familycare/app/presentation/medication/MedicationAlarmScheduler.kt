
package com.familycare.app.presentation.medication

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class MedicationAlarmScheduler(private val context: Context) {

    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(
        medicineId: String,
        medicineName: String,
        dosage: String,
        times: List<String>
    ) {
        times.distinct().forEach { time ->
            val parts = time.split(":")
            if (parts.size != 2) return@forEach

            val hour = parts[0].toIntOrNull() ?: return@forEach
            val minute = parts[1].toIntOrNull() ?: return@forEach
            if (hour !in 0..23 || minute !in 0..59) return@forEach

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val pendingIntent = createPendingIntent(
                medicineId = medicineId,
                name = medicineName,
                dosage = dosage,
                time = time,
                scheduledAt = calendar.timeInMillis,
                flag = PendingIntent.FLAG_UPDATE_CURRENT
            ) ?: return@forEach

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !alarmManager.canScheduleExactAlarms()
                    ) {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            calendar.timeInMillis,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                android.util.Log.e(
                    "MedicationScheduler",
                    "Không đặt được báo thức chính xác: $time",
                    e
                )

                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }

    fun cancel(medicineId: String, times: List<String>) {
        times.distinct().forEach { time ->
            val intent = Intent(
                context,
                MedicationReminderReceiver::class.java
            ).apply {
                action = ACTION_MEDICATION_REMINDER
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode(medicineId, time),
                intent,
                PendingIntent.FLAG_NO_CREATE or
                    PendingIntent.FLAG_IMMUTABLE
            )

            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun createPendingIntent(
        medicineId: String,
        name: String,
        dosage: String,
        time: String,
        scheduledAt: Long,
        flag: Int
    ): PendingIntent? {
        val intent = Intent(
            context,
            MedicationReminderReceiver::class.java
        ).apply {
            action = ACTION_MEDICATION_REMINDER
            putExtra(EXTRA_MEDICINE_ID, medicineId)
            putExtra(EXTRA_MEDICINE_NAME, name)
            putExtra(EXTRA_DOSAGE, dosage)
            putExtra(EXTRA_TIME, time)
            putExtra(EXTRA_SCHEDULED_AT, scheduledAt)
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode(medicineId, time),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_MEDICATION_REMINDER =
            "com.familycare.app.MEDICATION_REMINDER"

        const val EXTRA_MEDICINE_ID = "medicine_id"
        const val EXTRA_MEDICINE_NAME = "medicine_name"
        const val EXTRA_DOSAGE = "medicine_dosage"
        const val EXTRA_TIME = "medicine_time"
        const val EXTRA_SCHEDULED_AT = "scheduled_at"

        private fun requestCode(id: String, time: String) =
            "$id|$time".hashCode()
    }
}
