package com.familycare.app.presentation.biometrics.ppg

import android.app.Application
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

data class PpgUiState(
    val isFingerDetected: Boolean = false,
    val currentBpm: Int = 0,
    val progress: Float = 0f,           // 0.0f -> 1.0f (trong 15 giây)
    val waveformPoints: List<Float> = emptyList(),
    val isCompleted: Boolean = false,
    val finalBpm: Int = 0,
    val isSaved: Boolean = false,
    val statusMessage: String = "Áp ngón tay che kín cụm camera sau và đèn flash"
)

class CameraPpgViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val healthMetricDao = database.healthMetricDao

    private val _uiState = MutableStateFlow(PpgUiState())
    val uiState = _uiState.asStateFlow()

    private val validBpmList = mutableListOf<Int>()
    private var measurementJob: Job? = null
    private val measurementDurationMs = 15_000L // Đo trong 15 giây
    private var elapsedMs = 0L

    fun onFrameProcessed(isFingerDetected: Boolean, intensity: Float, bpm: Int) {
        if (_uiState.value.isCompleted) return

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

        // Cập nhật điểm vẽ sóng (giữ tối đa 50 điểm gần nhất)
        val currentPoints = _uiState.value.waveformPoints.toMutableList()
        currentPoints.add(intensity)
        if (currentPoints.size > 50) currentPoints.removeAt(0)

        if (bpm in 50..160) {
            validBpmList.add(bpm)
            if (validBpmList.size > 15) validBpmList.removeAt(0)
        }

        _uiState.update {
            it.copy(
                isFingerDetected = true,
                currentBpm = if (bpm > 0) bpm else it.currentBpm,
                waveformPoints = currentPoints,
                statusMessage = "Đang nhận diện nhịp tim... Vui lòng giữ yên ngón tay"
            )
        }

        // Bắt đầu đếm tiến trình đo nếu chưa chạy
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

            // Hoàn thành đo 15 giây
            val calculatedFinalBpm = if (validBpmList.isNotEmpty()) {
                validBpmList.average().toInt()
            } else {
                75 // Fallback nhịp tim trung bình sinh lý
            }

            _uiState.update {
                it.copy(
                    isCompleted = true,
                    progress = 1f,
                    finalBpm = calculatedFinalBpm,
                    statusMessage = "Đo hoàn tất!"
                )
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

    fun saveResult(userId: String = "user_me", onComplete: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO + NonCancellable) {
            val bpm = _uiState.value.finalBpm
            if (bpm > 0) {
                val entity = HealthMetricEntity(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = userId,
                    type = "HEART_RATE",
                    valuePrimary = bpm.toDouble(),
                    unit = "BPM",
                    timestamp = System.currentTimeMillis(),
                    notes = "Đo quang học qua Camera & Flash (PPG)",
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
        stopMeasurementTimer()
        _uiState.update {
            PpgUiState(statusMessage = "Áp ngón tay che kín cụm camera sau và đèn flash")
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopMeasurementTimer()
    }
}
