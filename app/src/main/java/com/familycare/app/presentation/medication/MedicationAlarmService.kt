package com.familycare.app.presentation.medication

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import com.familycare.app.R

class MedicationAlarmService : Service() {

    private var ringtone: Ringtone? = null
    private var medicineId = ""
    private var medicineName = "Thuốc"
    private var dosage = ""
    private var time = ""

    // Bộ hẹn giờ tự động tắt chuông sau 1 phút nếu người bệnh không bấm gì
    private var autoStopHandler: Handler? = null
    private var autoStopRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopAlarm()
                return START_NOT_STICKY
            }

            ACTION_SNOOZE -> {
                scheduleSnooze()
                stopAlarm()
                return START_NOT_STICKY
            }

            ACTION_START -> {
                medicineId = intent.getStringExtra(
                    MedicationAlarmScheduler.EXTRA_MEDICINE_ID
                ).orEmpty()

                medicineName = intent.getStringExtra(
                    MedicationAlarmScheduler.EXTRA_MEDICINE_NAME
                ) ?: "Thuốc"

                dosage = intent.getStringExtra(
                    MedicationAlarmScheduler.EXTRA_DOSAGE
                ).orEmpty()

                time = intent.getStringExtra(
                    MedicationAlarmScheduler.EXTRA_TIME
                ).orEmpty()

                ensureChannel()

                val notification = buildNotification()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }

                startRinging()
            }
        }

        return START_NOT_STICKY
    }

    private fun startRinging() {
        // Dừng chuông/rung cũ nếu dịch vụ nhận lệnh bắt đầu lại.
        stopRinging()

        try {
            val uri = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_ALARM
            ) ?: RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )

            if (uri != null) {
                ringtone = RingtoneManager.getRingtone(
                    applicationContext,
                    uri
                )

                ringtone?.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ringtone?.isLooping = true
                }

                ringtone?.play()
            }
        } catch (e: Exception) {
            android.util.Log.e(
                TAG,
                "Không thể phát chuông báo thuốc",
                e
            )
        }

        try {
            val pattern = longArrayOf(0, 700, 400, 700, 900)
            val effect = VibrationEffect.createWaveform(pattern, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
                    ?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(
                    Context.VIBRATOR_SERVICE
                ) as Vibrator

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Không thể rung thiết bị", e)
        }

        // Tự động ngắt chuông sau 60 giây và tự động hoãn 5 phút nếu không ai bấm gì (tránh hao pin/nóng máy)
        cancelAutoStopTimer()
        val handler = Handler(Looper.getMainLooper())
        val runnable = Runnable {
            android.util.Log.i(TAG, "Chuông reo đủ 60 giây không có tương tác -> Tự động chuyển sang chế độ nhắc lại 5 phút")
            scheduleSnooze()
            stopAlarm()
        }
        autoStopHandler = handler
        autoStopRunnable = runnable
        handler.postDelayed(runnable, AUTO_STOP_DELAY_MS)
    }

    private fun buildNotification(): Notification {
        val stopIntent = Intent(
            this,
            MedicationAlarmService::class.java
        ).setAction(ACTION_STOP)

        val snoozeIntent = Intent(
            this,
            MedicationAlarmService::class.java
        ).setAction(ACTION_SNOOZE)

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or
            PendingIntent.FLAG_IMMUTABLE

        val stopPending = PendingIntent.getService(
            this,
            4101,
            stopIntent,
            flags
        )

        val snoozePending = PendingIntent.getService(
            this,
            4102,
            snoozeIntent,
            flags
        )

        return NotificationCompat.Builder(this, CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đến giờ uống thuốc")
            .setContentText("$medicineName — $dosage")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Đã đến giờ uống $medicineName. " +
                        "Liều lượng: $dosage. Giờ đã cài: $time."
                )
            )
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(0, "Nhắc lại 5 phút", snoozePending)
            .addAction(0, "Tắt chuông", stopPending)
            .build()
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(
                NotificationManager::class.java
            )

            if (manager.getNotificationChannel(CHANNEL_ALARM) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ALARM,
                    "Chuông báo uống thuốc",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Chuông reo cho lịch uống thuốc"
                    enableVibration(true)

                    // Âm thanh do service tự phát.
                    setSound(null, null)
                }

                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun scheduleSnooze() {
        if (medicineId.isBlank() || time.isBlank()) return

        val intent = Intent(
            this,
            MedicationReminderReceiver::class.java
        ).apply {
            action = MedicationAlarmScheduler.ACTION_MEDICATION_REMINDER

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

        val pending = PendingIntent.getBroadcast(
            this,
            SNOOZE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val triggerAt =
            System.currentTimeMillis() + SNOOZE_DELAY_MS

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
            }
        } catch (e: SecurityException) {
            // Nếu không được phép đặt báo thức chính xác,
            // dùng lịch gần đúng làm phương án dự phòng.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pending
            )
        }
    }

    private fun stopRinging() {
        cancelAutoStopTimer()
        try {
            ringtone?.stop()
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Không thể dừng chuông", e)
        }
        ringtone = null

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
                    ?.cancel()
            } else {
                @Suppress("DEPRECATION")
                (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
                    .cancel()
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Không thể dừng rung", e)
        }
    }

    private fun cancelAutoStopTimer() {
        autoStopRunnable?.let { autoStopHandler?.removeCallbacks(it) }
        autoStopRunnable = null
        autoStopHandler = null
    }

    private fun stopAlarm() {
        cancelAutoStopTimer()
        stopRinging()

        getSystemService(NotificationManager::class.java)
            .cancel(NOTIFICATION_ID)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        cancelAutoStopTimer()
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START =
            "com.familycare.app.MEDICATION_ALARM_START"

        const val ACTION_STOP =
            "com.familycare.app.MEDICATION_ALARM_STOP"

        const val ACTION_SNOOZE =
            "com.familycare.app.MEDICATION_ALARM_SNOOZE"

        private const val TAG = "MedicationAlarmService"
        private const val NOTIFICATION_ID = 7315
        private const val CHANNEL_ALARM = "medication_alarm_channel"
        private const val SNOOZE_REQUEST_CODE = 92031
        private const val SNOOZE_DELAY_MS = 5 * 60 * 1000L
        private const val AUTO_STOP_DELAY_MS = 60 * 1000L // 60 giây (1 phút) tự động ngắt chuông và hoãn
    }
}
