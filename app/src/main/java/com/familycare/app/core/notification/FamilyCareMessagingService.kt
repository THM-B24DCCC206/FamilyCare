
package com.familycare.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.familycare.app.MainActivity
import com.familycare.app.R
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class FamilyCareMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FamilyCareFCM"
        private const val CHANNEL_ID = "familycare_family_reminders"

        // Chỉ dùng khi chạy Android Emulator trên cùng máy với server.
        // Điện thoại thật cần URL HTTPS công khai.
        private const val SERVER_URL =
            "http://127.0.0.1:3000/register-device"

        private val networkExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onCreate() {
        super.onCreate()

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    registerDevice(task.result)
                } else {
                    Log.e(TAG, "Không lấy được FCM token", task.exception)
                }
            }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token đã được cập nhật")
        registerDevice(token)
    }

    private fun registerDevice(token: String) {
        val familyCode = FamilyLinkPreferences.getFamilyCode(this)

        if (familyCode.isNullOrBlank()) {
            Log.d(TAG, "Chưa liên kết gia đình; chưa đăng ký thiết bị.")
            return
        }

        networkExecutor.execute {
            var connection: HttpURLConnection? = null

            try {
                connection = (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 10000
                    readTimeout = 10000
                    doOutput = true
                    setRequestProperty(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                    )
                }

                val payload = JSONObject().apply {
                    put("familyCode", familyCode)
                    put("token", token)
                }.toString()

                connection.outputStream.use { output ->
                    output.write(payload.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val responseStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val responseText = responseStream
                    ?.bufferedReader()
                    ?.use { it.readText() }
                    .orEmpty()

                if (responseCode in 200..299) {
                    Log.d(TAG, "Đăng ký thiết bị thành công.")
                } else {
                    Log.e(
                        TAG,
                        "Đăng ký thiết bị thất bại: HTTP $responseCode, $responseText"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Không thể đăng ký thiết bị với server.", e)
            } finally {
                connection?.disconnect()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: "FamilyCare"

        val body = message.notification?.body
            ?: message.data["body"]
            ?: "Bạn có một thông báo mới từ gia đình."

        showNotification(title, body)
    }

    private fun showNotification(title: String, body: String) {
        val manager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Nhắc nhở gia đình",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)

        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
        )
    }
}
