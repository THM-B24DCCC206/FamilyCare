
package com.familycare.app.presentation.appointment

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class AppointmentAlarmScheduler(
    private val context: Context
) {
    private val alarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(appointmentId: String, hospitalName: String, appointmentTime: Long) {
        // Nhắc trước giờ khám 1 tiếng.
        val reminderTime = appointmentTime - 60 * 60 * 1000L

        // Không đặt báo thức nếu thời điểm nhắc đã qua.
        if (reminderTime <= System.currentTimeMillis()) return

        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = ACTION_APPOINTMENT_REMINDER
            putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
            putExtra(EXTRA_HOSPITAL_NAME, hospitalName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        }
    }

    fun cancel(appointmentId: String) {
        val intent = Intent(context, AppointmentReminderReceiver::class.java).apply {
            action = ACTION_APPOINTMENT_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            appointmentId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    companion object {
        const val ACTION_APPOINTMENT_REMINDER =
            "com.familycare.app.APPOINTMENT_REMINDER"

        const val EXTRA_APPOINTMENT_ID = "appointment_id"
        const val EXTRA_HOSPITAL_NAME = "hospital_name"
    }
}
