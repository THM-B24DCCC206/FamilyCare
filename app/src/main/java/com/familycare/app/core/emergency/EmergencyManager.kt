package com.familycare.app.core.emergency

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Trình quản lý sự cố khẩn cấp (EmergencyManager):
 * 1. Bắt tọa độ GPS độ chính xác cao bằng FusedLocationProviderClient.
 * 2. Đóng gói Payload thông báo khẩn cấp độ ưu tiên cao (FCM High-Priority).
 * 3. Bật âm thanh còi hú tối đa bằng MediaPlayer và AudioManager.
 * 4. Rung phản hồi cảm ứng khẩn cấp (Haptic Emergency Vibration).
 */
class EmergencyManager(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private var mediaPlayer: MediaPlayer? = null
    private var isSirenActive: Boolean = false

    /**
     * Lấy tọa độ GPS chính xác tức thời bằng FusedLocationProviderClient
     * (Không phụ thuộc vào extension ngoài, tương thích mọi phiên bản coroutine)
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentGpsLocation(): Pair<Double, Double> = suspendCancellableCoroutine { continuation ->
        val hasFine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            // Tọa độ dự phòng nếu người dùng chưa cấp quyền (Tọa độ trung tâm mẫu)
            continuation.resume(Pair(10.7769, 106.7009))
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()
        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                if (location != null) {
                    continuation.resume(Pair(location.latitude, location.longitude))
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc: Location? ->
                        if (lastLoc != null) {
                            continuation.resume(Pair(lastLoc.latitude, lastLoc.longitude))
                        } else {
                            continuation.resume(Pair(10.7769, 106.7009))
                        }
                    }.addOnFailureListener {
                        continuation.resume(Pair(10.7769, 106.7009))
                    }
                }
            }.addOnFailureListener {
                continuation.resume(Pair(10.7769, 106.7009))
            }
        } catch (_: Exception) {
            continuation.resume(Pair(10.7769, 106.7009))
        }
    }

    /**
     * Đóng gói Payload thông báo khẩn cấp gửi qua Firebase Cloud Messaging
     */
    fun createEmergencyPayload(
        userId: String,
        userName: String,
        familyId: String,
        latitude: Double,
        longitude: Double
    ): EmergencyPayload {
        return EmergencyPayload(
            eventId = UUID.randomUUID().toString(),
            userId = userId,
            userName = userName,
            familyId = familyId,
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Kích hoạt còi hú báo động âm lượng tối đa bằng MediaPlayer & AudioManager
     */
    fun startSirenAlarm() {
        if (isSirenActive) return

        try {
            // 1. Ép âm lượng chuông báo thức lên mức cao nhất
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            // 2. Lấy âm thanh báo động mặc định của hệ thống
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            // 3. Khởi tạo MediaPlayer phát lặp liên tục
            mediaPlayer = MediaPlayer().apply {
                setDataSource(context, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            isSirenActive = true

            // 4. Kích hoạt rung liên tục theo chu kỳ khẩn cấp
            triggerEmergencyVibration()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dừng âm thanh còi hú và giải phóng MediaPlayer
     */
    fun stopSirenAlarm() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            isSirenActive = false
            stopVibration()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isSirenPlaying(): Boolean = isSirenActive

    /**
     * Rung nhịp kép cảnh báo khẩn cấp
     */
    private fun triggerEmergencyVibration() {
        try {
            val pattern = longArrayOf(0, 800, 300, 800, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, 0) // 0: lặp lại
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(pattern, 0)
            }
        } catch (_: Exception) {}
    }

    private fun stopVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.cancel()
            }
        } catch (_: Exception) {}
    }
}
