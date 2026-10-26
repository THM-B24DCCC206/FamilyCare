
package com.familycare.app.core.notification

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object FamilyDeviceRegistrar {
    private const val TAG = "FamilyDeviceRegistrar"
    private const val SERVER_URL = "http://127.0.0.1:3000/register-device"

    private val executor = Executors.newSingleThreadExecutor()

    fun linkFamily(
        context: Context,
        familyCodeInput: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val familyCode = familyCodeInput.trim().uppercase()

        if (familyCode.isBlank()) {
            onResult(false, "Vui lòng nhập mã gia đình.")
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    onResult(false, "Không lấy được FCM token. Hãy thử lại.")
                    return@addOnCompleteListener
                }

                val token = task.result

                executor.execute {
                    var connection: HttpURLConnection? = null

                    try {
                        connection =
                            (URL(SERVER_URL).openConnection() as HttpURLConnection).apply {
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

                        connection.outputStream.use {
                            it.write(payload.toByteArray(Charsets.UTF_8))
                        }

                        val code = connection.responseCode
                        val stream = if (code in 200..299) {
                            connection.inputStream
                        } else {
                            connection.errorStream
                        }

                        val response = stream?.bufferedReader()
                            ?.use { it.readText() }
                            .orEmpty()

                        if (code in 200..299) {
                            FamilyLinkPreferences.saveFamilyCode(
                                context.applicationContext,
                                familyCode
                            )
                            Log.d(TAG, "Đăng ký thiết bị thành công.")
                            onResult(true, "Liên kết gia đình thành công!")
                        } else {
                            val serverMessage = try {
                                JSONObject(response).optString(
                                    "message",
                                    "Máy chủ từ chối yêu cầu."
                                )
                            } catch (_: Exception) {
                                "Máy chủ từ chối yêu cầu."
                            }

                            onResult(false, serverMessage)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Lỗi đăng ký thiết bị.", e)
                        onResult(
                            false,
                            "Không kết nối được server. Hãy kiểm tra server đang chạy."
                        )
                    } finally {
                        connection?.disconnect()
                    }
                }
            }
    }
}
