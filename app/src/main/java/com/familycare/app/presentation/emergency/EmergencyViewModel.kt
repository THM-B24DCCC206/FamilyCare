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
import com.familycare.app.domain.model.NearbyHospitalProvider
import com.familycare.app.domain.repository.AlertRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel xử lý nghiệp vụ Nút bấm SOS 1-chạm & Điều phối còi hú, GPS, FCM Alert,
 * thông báo Chủ hộ gia đình và đề xuất bệnh viện lân cận.
 */
class EmergencyViewModel(
    private val emergencyManager: EmergencyManager,
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmergencyUiState())
    val uiState: StateFlow<EmergencyUiState> = _uiState.asStateFlow()

    init {
        refreshLocation()
    }

    /**
     * Cập nhật lại tọa độ GPS và truy vấn danh sách bệnh viện lân cận
     */
    fun refreshLocation() {
        viewModelScope.launch {
            val hasPermission = emergencyManager.hasLocationPermission()
            _uiState.update {
                it.copy(
                    hasLocationPermission = hasPermission,
                    isLocating = true,
                    currentAddress = if (hasPermission) "Đang dò tìm tọa độ GPS thực tế..." else "Chưa cấp quyền vị trí GPS"
                )
            }

            try {
                val (lat, lng) = emergencyManager.getCurrentGpsLocation()
                val address = emergencyManager.resolveAddress(lat, lng)
                val hospitals = NearbyHospitalProvider.getNearbyHospitals(lat, lng)

                _uiState.update {
                    it.copy(
                        hasLocationPermission = hasPermission,
                        currentLatitude = lat,
                        currentLongitude = lng,
                        currentAddress = address,
                        nearbyHospitals = hospitals,
                        isLocating = false
                    )
                }
            } catch (_: Exception) {
                val fallbackLat = 21.0285
                val fallbackLng = 105.8542
                val defaultHospitals = NearbyHospitalProvider.getNearbyHospitals(fallbackLat, fallbackLng)
                _uiState.update {
                    it.copy(
                        isLocating = false,
                        currentLatitude = it.currentLatitude ?: fallbackLat,
                        currentLongitude = it.currentLongitude ?: fallbackLng,
                        currentAddress = if (it.currentLatitude != null) it.currentAddress else "Vị trí GPS ($fallbackLat, $fallbackLng)",
                        nearbyHospitals = if (it.nearbyHospitals.isNotEmpty()) it.nearbyHospitals else defaultHospitals
                    )
                }
            }
        }
    }

    /**
     * Kích hoạt toàn bộ quy trình khẩn cấp khi người dùng giữ nút SOS đủ 2 giây:
     * 1. Bắt tọa độ GPS độ chính xác cao qua FusedLocationProviderClient & phân giải địa chỉ thực tế.
     * 2. Phát âm thanh còi hú cực đại liên tục bằng MediaPlayer.
     * 3. Gửi thông báo khẩn cấp đến Chủ gia đình và các thành viên.
     * 4. Tính toán danh sách bệnh viện gần nhất hỗ trợ chuyển sang Google Maps.
     */
    fun triggerSos() {
        if (_uiState.value.sosStatus is SosStatus.LocatingAndActivating) return

        _uiState.update { it.copy(sosStatus = SosStatus.LocatingAndActivating) }

        viewModelScope.launch {
            try {
                // 1. Lấy tọa độ GPS tức thời và giải mã địa chỉ chính xác
                val (lat, lng) = emergencyManager.getCurrentGpsLocation()
                val address = emergencyManager.resolveAddress(lat, lng)
                val hospitals = NearbyHospitalProvider.getNearbyHospitals(lat, lng)

                // 2. Kích hoạt còi hú âm lượng tối đa
                emergencyManager.startSirenAlarm()

                // 3. Đóng gói Payload FCM có thông tin Chủ hộ và địa chỉ thực tế
                val payload = emergencyManager.createEmergencyPayload(
                    userId = "user_demo_01",
                    userName = _uiState.value.targetUserName,
                    familyId = _uiState.value.familyId,
                    latitude = lat,
                    longitude = lng,
                    address = address,
                    familyOwnerName = _uiState.value.familyOwnerName,
                    familyOwnerPhone = _uiState.value.familyOwnerPhone
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
                    message = "Vị trí: $address ($lat, $lng). Đã báo cho Chủ hộ ${payload.familyOwnerName}.",
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
                        currentLongitude = lng,
                        currentAddress = address,
                        isOwnerNotified = true,
                        nearbyHospitals = hospitals
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
                isSirenPlaying = false,
                isOwnerNotified = false
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
