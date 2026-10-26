
package com.familycare.app.presentation.medication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.familycare.app.core.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MedicationOverdueReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val medicineId = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_ID
        ) ?: return

        val medicineName = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_MEDICINE_NAME
        ) ?: "Thuốc"

        val time = intent.getStringExtra(
            MedicationAlarmScheduler.EXTRA_TIME
        ) ?: return

        val scheduledAt = intent.getLongExtra(
            MedicationAlarmScheduler.EXTRA_SCHEDULED_AT,
            0L
        )

        if (scheduledAt <= 0L) {
            Log.e(TAG, "Thiếu thời điểm uống thuốc dự kiến.")
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                checkOverdue(
                    context = appContext,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    time = time,
                    scheduledAt = scheduledAt
                )
            } catch (e: Exception) {
                Log.e(TAG, "Lỗi kiểm tra thuốc quá hạn.", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun checkOverdue(
        context: Context,
        medicineId: String,
        medicineName: String,
        time: String,
        scheduledAt: Long
    ) {
        val scheduledDate = SimpleDateFormat(
            "yyyy-MM-dd",
            Locale.US
        ).format(Date(scheduledAt))

        val database = AppDatabase.getInstance(context)
        val dao = database.medicationLogDao

        val log = dao.getLog(
            medicineId = medicineId,
            date = scheduledDate,
            time = time
        )

        if (log == null) {
            Log.w(TAG, "Không tìm thấy nhật ký thuốc cho $medicineId.")
            return
        }

        if (log.isTaken || log.isSkipped || log.overdueNotified) {
            Log.d(TAG, "Liều thuốc đã xử lý hoặc đã cảnh báo; bỏ qua.")
            return
        }

        val title = "Cảnh báo uống thuốc quá hạn"
        val body = "$medicineName chưa được xác nhận đã uống lúc $time."

        val sent = sendFamilyAlert(title, body)

        if (sent) {
            val updatedRows = dao.markOverdueNotified(log.id)
            Log.d(TAG, "Gửi cảnh báo thành công; cập nhật $updatedRows bản ghi.")
        } else {
            showLocalNotification(
                context = context,
                notificationId = "$medicineId|$scheduledAt".hashCode(),
                title = title,
                body = body
            )
            Log.w(TAG, "Gửi FCM thất bại; chưa đánh dấu đã cảnh báo.")
        }
    }

    private suspend fun sendFamilyAlert(
        title: String,
        body: String
    ): Boolean = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null

        try {
            connection = URL(SERVER_URL)
                .openConnection() as HttpURLConnection

            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true
            connection.setRequestProperty(
                "Content-Type",
                "application/json; charset=UTF-8"
            )

            val payload = JSONObject().apply {
                put("title", title)
                put("body", body)
            }.toString()

            connection.outputStream.use { output ->
                output.write(payload.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val errorText = connection.errorStream
                    ?.bufferedReader()
                    ?.use { it.readText() }

                Log.e(
                    TAG,
                    "Server trả về HTTP $responseCode: $errorText"
                )
            }

            responseCode in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "Không kết nối được server cảnh báo.", e)
            false
        } finally {
            connection?.disconnect()
        }
    }

    private fun showLocalNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String
    ) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Chưa có quyền hiển thị thông báo.")
            return
        }

        createNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context)
                .notify(notificationId, notification)
        } catch (e: SecurityException) {
            Log.e(TAG, "Không có quyền hiển thị cảnh báo cục bộ.", e)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Cảnh báo thuốc quá hạn",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Cảnh báo khi chưa xác nhận uống thuốc sau 30 phút."
        }

        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "MedicationOverdue"
        private const val CHANNEL_ID = "medication_overdue_alerts"

        private const val SERVER_URL =
            "http://127.0.0.1:3000/send-family-alert"
    }
}
