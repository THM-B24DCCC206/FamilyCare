package com.familycare.app.presentation.emergency

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.emergency.EmergencyManager
import com.familycare.app.data.repository.AlertRepositoryImpl
import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType
import com.familycare.app.domain.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel xử lý nghiệp vụ Nút bấm SOS 1-chạm & Điều phối còi hú, GPS và FCM Alert
 */
class EmergencyViewModel(
    private val emergencyManager: EmergencyManager,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    /**
     * Kích hoạt toàn bộ quy trình khẩn cấp khi người dùng giữ nút SOS đủ 2 giây:
     * 1. Bắt tọa độ GPS độ chính xác cao qua FusedLocationProviderClient.
     * 2. Phát âm thanh còi hú cực đại liên tục bằng MediaPlayer.
     * 3. Đóng gói Payload thông báo FCM và lưu vào CSDL AlertRepository.
     */
    fun triggerSos() {
        if (_uiState.value.sosStatus is SosStatus.LocatingAndActivating) return

        _uiState.update { it.copy(sosStatus = SosStatus.LocatingAndActivating) }

        viewModelScope.launch {
            try {
                // 1. Lấy tọa độ GPS tức thời
                val (lat, lng) = emergencyManager.getCurrentGpsLocation()

                // 2. Kích hoạt còi hú âm lượng tối đa
                emergencyManager.startSirenAlarm()

                // 3. Đóng gói Payload FCM
                val payload = emergencyManager.createEmergencyPayload(
                    userId = "user_demo_01",
                    userName = _uiState.value.targetUserName,
                    familyId = _uiState.value.familyId,
                    latitude = lat,
                    longitude = lng
                )

                // 4. Lưu bản ghi cảnh báo đỏ vào CSDL cục bộ và phân phối tới gia đình
                val alert = Alert(
                    id = payload.eventId,
                    familyId = payload.familyId,
                    targetUserId = payload.userId,
                    targetUserName = payload.userName,
                    type = AlertType.SOS_EMERGENCY,
                    level = AlertLevel.CRITICAL,
                    title = "🚨 KHẨN CẤP: BÁO ĐỘNG SOS TỪ ${payload.userName}!",
                    message = "Đã kích hoạt còi cứu hộ và gửi định vị vị trí: $lat, $lng",
                    timestamp = payload.timestamp,
                    isRead = false,
                    latitude = lat,
                    longitude = lng
                )
                alertRepository.createAlert(alert)

                // 5. Cập nhật trạng thái UI hoàn tất
                _uiState.update {
                    it.copy(
                        sosStatus = SosStatus.Activated(payload),
                        isSirenPlaying = true,
                        currentLatitude = lat,
                        currentLongitude = lng
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(sosStatus = SosStatus.Error(e.localizedMessage ?: "Lỗi khi kích hoạt SOS"))
                }
            }
        }
    }

    /**
     * Dừng còi hú cứu hộ
     */
    fun stopSiren() {
        emergencyManager.stopSirenAlarm()
        _uiState.update { it.copy(isSirenPlaying = false) }
    }

    /**
     * Hủy trạng thái báo động, đưa màn hình về trạng thái chờ
     */
    fun resetSos() {
        stopSiren()
        _uiState.update {
            it.copy(
                sosStatus = SosStatus.Idle,
                isSirenPlaying = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        emergencyManager.stopSirenAlarm()
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val alertRepo = AlertRepositoryImpl(db.alertDao())
                val emergencyMgr = EmergencyManager(context.applicationContext)

                return EmergencyViewModel(emergencyMgr, alertRepo) as T
            }
        }
    }
}
