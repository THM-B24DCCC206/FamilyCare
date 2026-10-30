package com.familycare.app.presentation.biometrics.ppg

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

data class PpgUiState(
    val isFingerDetected: Boolean = false,
    val currentBpm: Int = 0,
    val progress: Float = 0f,           // 0.0f -> 1.0f (trong 15 giây)
    val waveformPoints: List<Float> = emptyList(),
    val isCompleted: Boolean = false,
    val finalBpm: Int = 0,
    val isSaved: Boolean = false,
    val statusMessage: String = "Áp ngón tay che kín cụm camera sau và đèn flash",
    val isEmulatorMode: Boolean = false,
    val hasMeasurementFailed: Boolean = false
)

class CameraPpgViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val healthMetricDao = database.healthMetricDao

    private val _uiState = MutableStateFlow(PpgUiState())
    val uiState = _uiState.asStateFlow()

    private val validBpmList = mutableListOf<Int>()
    private var measurementJob: Job? = null
    private var emulatorSimulationJob: Job? = null
    private val measurementDurationMs = 15_000L // Đo trong 15 giây
    private var elapsedMs = 0L

    init {
        val onEmulator = isRunningOnEmulator()
        _uiState.update {
            it.copy(
                isEmulatorMode = onEmulator,
                statusMessage = if (onEmulator) {
                    "Máy ảo phát hiện: Bạn có thể bật mô phỏng PPG để kiểm thử"
                } else {
                    "Áp ngón tay che kín cụm camera sau và đèn flash"
                }
            )
        }
    }

    /**
     * Nhận frame xử lý từ CameraX ImageAnalysis
     */
    fun onFrameProcessed(isFingerDetected: Boolean, intensity: Float, bpm: Int) {
        if (_uiState.value.isCompleted) return
        if (_uiState.value.isEmulatorMode && emulatorSimulationJob?.isActive == true) return

        if (!isFingerDetected) {
            // Ngón tay không che kín camera
            stopMeasurementTimer()
            _uiState.update {
                it.copy(
                    isFingerDetected = false,
                    currentBpm = 0,
                    statusMessage = "Vui lòng đặt ngón tay che kín cụm camera và đèn flash"
                )
            }
            return
        }

        // Cập nhật điểm vẽ sóng PPG (giữ tối đa 50 điểm gần nhất)
        val currentPoints = _uiState.value.waveformPoints.toMutableList()
        currentPoints.add(intensity)
        if (currentPoints.size > 50) currentPoints.removeAt(0)

        // Tích lũy các mẫu BPM hợp lệ theo chuẩn sinh lý (45 - 160 BPM)
        if (bpm in 45..160) {
            validBpmList.add(bpm)
        }

        _uiState.update {
            it.copy(
                isFingerDetected = true,
                currentBpm = if (bpm > 0) bpm else it.currentBpm,
                waveformPoints = currentPoints,
                statusMessage = "Đang nhận diện xung mạch... Vui lòng giữ yên ngón tay"
            )
        }

        // Bắt đầu đếm tiến trình đo 15 giây nếu chưa chạy
        if (measurementJob == null || measurementJob?.isActive == false) {
            startMeasurementTimer()
        }
    }

    private fun startMeasurementTimer() {
        measurementJob = viewModelScope.launch {
            val stepMs = 100L
            while (elapsedMs < measurementDurationMs) {
                delay(stepMs)
                elapsedMs += stepMs
                val prog = elapsedMs.toFloat() / measurementDurationMs
                _uiState.update { it.copy(progress = prog.coerceIn(0f, 1f)) }
            }

            // Hoàn tất chu kỳ đo 15 giây:
            // Tính trung bình các giá trị hợp lệ thu thập được trong suốt 15 giây
            val calculatedFinalBpm = if (validBpmList.size >= 5) {
                val sorted = validBpmList.sorted()
                // Cắt 15% ngoại lai cao nhất và thấp nhất để đạt độ tin cậy y tế tối đa
                val trimCount = (sorted.size * 0.15).toInt()
                val trimmed = if (trimCount > 0 && sorted.size - 2 * trimCount >= 3) {
                    sorted.subList(trimCount, sorted.size - trimCount)
                } else {
                    sorted
                }
                trimmed.average().roundToInt()
            } else if (_uiState.value.currentBpm in 45..160) {
                _uiState.value.currentBpm
            } else {
                0 // Tín hiệu không đủ để kết luận
            }

            if (calculatedFinalBpm > 0) {
                _uiState.update {
                    it.copy(
                        isCompleted = true,
                        hasMeasurementFailed = false,
                        progress = 1f,
                        finalBpm = calculatedFinalBpm,
                        statusMessage = "Đo hoàn tất!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCompleted = true,
                        hasMeasurementFailed = true,
                        progress = 1f,
                        finalBpm = 0,
                        statusMessage = "Tín hiệu chưa đủ ổn định. Vui lòng giữ yên ngón tay và thử lại!"
                    )
                }
            }
        }
    }

    private fun stopMeasurementTimer() {
        measurementJob?.cancel()
        measurementJob = null
        elapsedMs = 0L
        validBpmList.clear()
        _uiState.update { it.copy(progress = 0f) }
    }

    /**
     * Kích hoạt chế độ mô phỏng xung quang học PPG dành riêng cho Môi trường Máy ảo (Emulator)
     */
    fun startEmulatorSimulation() {
        stopMeasurementTimer()
        emulatorSimulationJob?.cancel()

        emulatorSimulationJob = viewModelScope.launch {
            // Nhịp tim cơ sở ngẫu nhiên cho phiên đo này (ví dụ 68..80 BPM)
            val baseHr = (68..80).random()
            var stepIndex = 0
            val points = mutableListOf<Float>()
            elapsedMs = 0L
            val stepMs = 50L // 20 FPS cập nhật sóng

            _uiState.update {
                it.copy(
                    isFingerDetected = true,
                    isCompleted = false,
                    hasMeasurementFailed = false,
                    statusMessage = "Đang mô phỏng xung nhịp PPG trên máy ảo..."
                )
            }

            while (elapsedMs < measurementDurationMs) {
                delay(stepMs)
                elapsedMs += stepMs
                stepIndex++

                // Tạo dao động nhịp tim tự nhiên (Heart Rate Variability - HRV ±2 BPM)
                val currentVariation = (sin(stepIndex * 0.08) * 2.2).toInt()
                val liveBpm = (baseHr + currentVariation).coerceIn(55, 120)

                // Tạo hình dạng sóng PPG sinh học: Sóng chính (Systolic) + Sóng phản xạ phụ (Dicrotic notch)
                val t = stepIndex * 0.18
                val waveSample = (sin(t) + 0.45 * sin(2 * t + 0.5) + (Math.random() * 0.05)).toFloat()

                points.add(waveSample)
                if (points.size > 50) points.removeAt(0)

                if (stepIndex % 6 == 0) {
                    validBpmList.add(liveBpm)
                }

                val prog = elapsedMs.toFloat() / measurementDurationMs
                _uiState.update {
                    it.copy(
                        progress = prog.coerceIn(0f, 1f),
                        currentBpm = liveBpm,
                        waveformPoints = points.toList()
                    )
                }
            }

            // Hoàn tất đo trên máy ảo
            val finalBpm = if (validBpmList.isNotEmpty()) {
                validBpmList.average().roundToInt()
            } else {
                baseHr
            }

            _uiState.update {
                it.copy(
                    isCompleted = true,
                    hasMeasurementFailed = false,
                    progress = 1f,
                    finalBpm = finalBpm,
                    statusMessage = "Đo hoàn tất (Mô phỏng máy ảo)"
                )
            }
        }
    }

    /**
     * Lưu kết quả đo nhịp tim vào cơ sở dữ liệu Room
     */
    fun saveResult(userId: String = "user_me", onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val bpm = _uiState.value.finalBpm
            if (bpm > 0) {
                val entity = HealthMetricEntity(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    type = "HEART_RATE",
                    valuePrimary = bpm.toDouble(),
                    unit = "BPM",
                    timestamp = System.currentTimeMillis(),
                    notes = if (_uiState.value.isEmulatorMode && emulatorSimulationJob != null) {
                        "Đo quang học qua Camera PPG (Mô phỏng máy ảo)"
                    } else {
                        "Đo quang học qua Camera & Flash (PPG)"
                    },
                    isFromCameraPpg = true
                )
                healthMetricDao.insertMetric(entity)
                _uiState.update { it.copy(isSaved = true) }
            }
            withContext(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    fun restart() {
        emulatorSimulationJob?.cancel()
        emulatorSimulationJob = null
        stopMeasurementTimer()
        _uiState.update {
            PpgUiState(
                isEmulatorMode = isRunningOnEmulator(),
                statusMessage = if (isRunningOnEmulator()) {
                    "Máy ảo phát hiện: Bạn có thể bật mô phỏng PPG để kiểm thử"
                } else {
                    "Áp ngón tay che kín cụm camera sau và đèn flash"
                }
            )
        }
    }

    private fun isRunningOnEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER
        val hardware = Build.HARDWARE
        val product = Build.PRODUCT
        return (fingerprint.startsWith("generic")
                || fingerprint.startsWith("unknown")
                || model.contains("google_sdk")
                || model.contains("Emulator")
                || model.contains("Android SDK built for x86")
                || manufacturer.contains("Genymotion")
                || hardware.contains("goldfish")
                || hardware.contains("ranchu")
                || product.contains("sdk_gphone")
                || product.contains("emulator"))
    }

    override fun onCleared() {
        super.onCleared()
        emulatorSimulationJob?.cancel()
        stopMeasurementTimer()
    }
}
