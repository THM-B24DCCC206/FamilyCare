package com.familycare.app.presentation.biometrics.ppg

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer

class CameraPpgAnalyzer(
    private val onFrameProcessed: (isFingerDetected: Boolean, redIntensity: Float, currentBpm: Int) -> Unit
) : ImageAnalysis.Analyzer {

    private val signalProcessor = PpgSignalProcessor()

    override fun analyze(image: ImageProxy) {
        val timestamp = System.currentTimeMillis()

        try {
            val planes = image.planes
            if (planes.size < 3) {
                image.close()
                return
            }

            // Y plane (độ sáng) & V plane (kênh đỏ Cr)
            val yBuffer: ByteBuffer = planes[0].buffer
            val vBuffer: ByteBuffer = planes[2].buffer

            val width = image.width
            val height = image.height

            // Lấy mẫu vùng trung tâm (vùng ngón tay áp trực tiếp lên cảm biến)
            val cropSize = 80
            val startX = (width - cropSize) / 2
            val startY = (height - cropSize) / 2

            var sumY = 0L
            var sumV = 0L
            var count = 0

            val yRowStride = planes[0].rowStride
            val yPixelStride = planes[0].pixelStride
            val vRowStride = planes[2].rowStride
            val vPixelStride = planes[2].pixelStride

            for (row in startY until startY + cropSize step 2) {
                for (col in startX until startX + cropSize step 2) {
                    val yIndex = row * yRowStride + col * yPixelStride
                    val vIndex = (row / 2) * vRowStride + (col / 2) * vPixelStride

                    val yLimit = yBuffer.limit()
                    val vLimit = vBuffer.limit()
                    if (yIndex < yLimit && vIndex < vLimit) {
                        val y = yBuffer.get(yIndex).toInt() and 0xFF
                        val v = vBuffer.get(vIndex).toInt() and 0xFF

                        sumY += y
                        sumV += v
                        count++
                    }
                }
            }

            if (count > 0) {
                val avgY = sumY.toDouble() / count
                val avgV = sumV.toDouble() / count

                // Ước tính cường độ màu đỏ: R = Y + 1.402 * (V - 128)
                val redIntensity = avgY + 1.402 * (avgV - 128)

                // Kiểm tra xem ngón tay có đang che kín camera & flash hay không
                // Khi có ngón tay và flash bật: Kênh đỏ V rất cao (> 130) và độ sáng Y đủ lớn (> 20)
                val isFingerDetected = avgV > 130 && avgY > 20

                if (isFingerDetected) {
                    val bpm = signalProcessor.addSample(timestamp, redIntensity)
                    onFrameProcessed(true, redIntensity.toFloat(), bpm)
                } else {
                    signalProcessor.reset()
                    onFrameProcessed(false, 0f, 0)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            image.close() // Bắt buộc phải đóng ImageProxy để CameraX tiếp tục gửi frame tiếp theo
        }
    }

    fun reset() {
        signalProcessor.reset()
    }
}
