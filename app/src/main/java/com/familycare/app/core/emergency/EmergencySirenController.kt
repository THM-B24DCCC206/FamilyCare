package com.familycare.app.core.emergency

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Bộ điều khiển âm thanh còi hú & rung khẩn cấp tập trung (Singleton).
 * Đồng bộ trạng thái giữa Thiết bị phát (EmergencyManager) và Thiết bị người thân tiếp nhận (FCM Service & Activity).
 */
object EmergencySirenController {
    private const val TAG = "EmergencySirenCtrl"

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying: Boolean = false
    private var originalAlarmVolume: Int? = null

    /**
     * Bật còi hú âm lượng tối đa 100% và kích hoạt rung dồn dập
     */
    @Synchronized
    fun startSiren(context: Context) {
        if (isPlaying) return

        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            // 1. Lưu lại mức âm lượng ban đầu để hoàn trả khi tắt còi
            if (originalAlarmVolume == null) {
                originalAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
            }

            // 2. Ép mức âm lượng báo động lên mức tối đa (100%)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0)

            // 3. Lấy âm thanh báo động mặc định của hệ điều hành
            var alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            if (alertUri == null) {
                alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            }

            // 4. Khởi chạy MediaPlayer phát lặp vô hạn
            mediaPlayer?.release()
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
            isPlaying = true

            // 5. Kích hoạt rung dồn dập
            startVibration(context)

            Log.d(TAG, "Đã kích hoạt còi hú SOS âm lượng tối đa & rung khẩn cấp.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi kích hoạt còi hú SOS", e)
        }
    }

    /**
     * Tắt còi hú, hủy rung và khôi phục âm lượng ban đầu của người dùng
     */
    @Synchronized
    fun stopSiren(context: Context) {
        try {
            // 1. Dừng và giải phóng MediaPlayer
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            mediaPlayer = null
            isPlaying = false

            // 2. Tắt rung khẩn cấp
            stopVibration(context)

            // 3. Khôi phục âm lượng ban đầu nếu có
            originalAlarmVolume?.let { origVol ->
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, origVol, 0)
                originalAlarmVolume = null
            }

            Log.d(TAG, "Đã tắt còi hú và khôi phục âm lượng ban đầu.")
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi dừng còi hú SOS", e)
        }
    }

    fun isSirenPlaying(): Boolean = isPlaying

    /**
     * Rung nhịp kép cấp cứu dồn dập
     */
    private fun startVibration(context: Context) {
        try {
            val pattern = longArrayOf(0, 800, 300, 800, 300, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0) // 0: lặp lại
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi kích hoạt rung", e)
        }
    }

    private fun stopVibration(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.cancel()
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi dừng rung", e)
        }
    }
}
