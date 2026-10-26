package com.familycare.app.presentation.alert

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
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

/**
 * ViewModel quản lý danh sách cảnh báo (Alert Rules & History) của Thành viên 2
 */
class AlertViewModel(
    private val alertRepository: AlertRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertUiState())
    val uiState: StateFlow<AlertUiState> = _uiState.asStateFlow()

    init {
        observeAlerts("fam_123456")
    }

    fun observeAlerts(familyId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            alertRepository.getAlertsByFamily(familyId).collect { alertList ->
                val list = if (alertList.isEmpty()) seedDefaultAlerts() else alertList
                _uiState.update {
                    it.copy(
                        alerts = list,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    fun setFilter(level: AlertLevel?) {
        _uiState.update { it.copy(selectedFilter = level) }
    }

    fun markAsRead(alertId: String) {
        viewModelScope.launch {
            alertRepository.markAlertAsRead(alertId)
            _uiState.update { state ->
                state.copy(
                    alerts = state.alerts.map {
                        if (it.id == alertId) it.copy(isRead = true) else it
                    }
                )
            }
        }
    }

    private fun seedDefaultAlerts(): List<Alert> {
        val now = System.currentTimeMillis()
        return listOf(
            Alert(
                id = "alert_01",
                familyId = "fam_123456",
                targetUserId = "user_demo_01",
                targetUserName = "Bác Nguyễn Văn Nam",
                type = AlertType.THRESHOLD_EXCEEDED,
                level = AlertLevel.CRITICAL,
                title = "⚠️ CẢNH BÁO ĐỎ: 3 LẦN ĐO VƯỢT NGƯỠNG LIÊN TIẾP",
                message = "Huyết áp trung bình 148/95 mmHg đo trong 48h qua vượt ngưỡng an toàn. Cần kiểm tra cữ thuốc!",
                timestamp = now - 35 * 60 * 1000L,
                isRead = false
            ),
            Alert(
                id = "alert_02",
                familyId = "fam_123456",
                targetUserId = "user_demo_01",
                targetUserName = "Bác Nguyễn Văn Nam",
                type = AlertType.SOS_EMERGENCY,
                level = AlertLevel.CRITICAL,
                title = "🚨 BÁO ĐỘNG SOS KHẨN CẤP",
                message = "Người thân đã kích hoạt còi cứu hộ tại tọa độ 21.0285, 105.8542 (Hà Nội)",
                timestamp = now - 2 * 3600 * 1000L,
                isRead = true,
                latitude = 21.0285,
                longitude = 105.8542
            ),
            Alert(
                id = "alert_03",
                familyId = "fam_123456",
                targetUserId = "user_demo_01",
                targetUserName = "Bác Nguyễn Văn Nam",
                type = AlertType.MEDICATION_MISSED,
                level = AlertLevel.WARNING,
                title = "Nhắc nhở: Quên uống thuốc Amlodipine",
                message = "Cữ thuốc lúc 08:00 sáng chưa được đánh dấu đã uống.",
                timestamp = now - 6 * 3600 * 1000L,
                isRead = true
            )
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val repo = AlertRepositoryImpl(db.alertDao())
                return AlertViewModel(repo) as T
            }
        }
    }
}
