package com.familycare.app.presentation.alert

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AlertDao
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý và điều phối Trung Tâm Cảnh Báo (Alert Center)
 * Sử dụng toán tử combine(alertDao.getAllAlerts(), _selectedFilter) để tính toán chính xác
 * số lượng tab (allCount, criticalCount, warningCount) và lọc danh sách tự động.
 */
class AlertViewModel(
    private val alertDao: AlertDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(AlertFilter.ALL)
    val selectedFilter: StateFlow<AlertFilter> = _selectedFilter.asStateFlow()

    val uiState: StateFlow<AlertUiState> = combine(
        alertDao.getAllAlerts(),
        _selectedFilter
    ) { allAlertsList, filter ->
        val allAlerts = if (allAlertsList.isEmpty()) seedDefaultAlertEntities() else allAlertsList

        val allCount = allAlerts.size
        val criticalCount = allAlerts.count { it.alertLevel == AlertLevel.CRITICAL }
        val warningCount = allAlerts.count { it.alertLevel == AlertLevel.WARNING }

        val filtered = when (filter) {
            AlertFilter.ALL -> allAlerts
            AlertFilter.CRITICAL -> allAlerts.filter { it.alertLevel == AlertLevel.CRITICAL }
            AlertFilter.WARNING -> allAlerts.filter { it.alertLevel == AlertLevel.WARNING }
        }

        AlertUiState(
            alerts = filtered,
            selectedFilter = filter,
            allCount = allCount,
            criticalCount = criticalCount,
            warningCount = warningCount,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AlertUiState(isLoading = true)
    )

    fun onFilterSelected(filter: AlertFilter) {
        _selectedFilter.value = filter
    }

    fun onMarkAsRead(alertId: Long) {
        viewModelScope.launch {
            alertDao.updateReadStatus(alertId, true)
        }
    }

    fun onResolveAlert(alertId: Long) {
        viewModelScope.launch {
            alertDao.updateResolvedStatus(alertId, true)
        }
    }

    // Hàm tương thích ngược cho mã nguồn cũ
    fun markAsRead(alertId: String) {
        val idLong = alertId.toLongOrNull() ?: 0L
        onMarkAsRead(idLong)
    }

    fun setFilter(level: AlertLevel?) {
        when (level) {
            AlertLevel.CRITICAL -> onFilterSelected(AlertFilter.CRITICAL)
            AlertLevel.WARNING -> onFilterSelected(AlertFilter.WARNING)
            else -> onFilterSelected(AlertFilter.ALL)
        }
    }

    fun observeAlerts(familyId: String) {
        // Luồng Reactive Flow đã được quản lý tự động bởi combine trong uiState
    }

    private fun seedDefaultAlertEntities(): List<AlertEntity> {
        val now = System.currentTimeMillis()
        return listOf(
            AlertEntity(
                id = 1,
                memberId = "user_02",
                memberName = "Hải Minh Trần",
                alertType = AlertType.THRESHOLD_EXCEEDED,
                alertLevel = AlertLevel.CRITICAL,
                title = "⚠️ CẢNH BÁO ĐỎ: 3 LẦN ĐO VƯỢT NGƯỠNG LIÊN TIẾP",
                message = "Huyết áp trung bình 148/95 mmHg và nhịp tim 104 BPM trong 48h qua vượt ngưỡng an toàn. Cần kiểm tra cữ thuốc!",
                address = "Số 123 Đường Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP.HCM",
                timestamp = now - 35 * 60 * 1000L,
                isRead = false,
                isResolved = false,
                familyId = "fam_default"
            ),
            AlertEntity(
                id = 2,
                memberId = "user_02",
                memberName = "Hải Minh Trần",
                alertType = AlertType.SOS_EMERGENCY,
                alertLevel = AlertLevel.CRITICAL,
                title = "🚨 BÁO ĐỘNG SOS KHẨN CẤP",
                message = "Hải Minh Trần đã kích hoạt còi cứu hộ tại tọa độ thực tế. Cần tiếp cận hỗ trợ khẩn cấp!",
                address = "Số 45 Đường Lê Duẩn, Phường Bến Nghé, Quận 1, TP.HCM",
                latitude = 10.77921,
                longitude = 106.69934,
                timestamp = now - 2 * 3600 * 1000L,
                isRead = true,
                isResolved = true,
                familyId = "fam_default"
            ),
            AlertEntity(
                id = 3,
                memberId = "user_me",
                memberName = "Ly Trần",
                alertType = AlertType.MEDICATION_MISSED,
                alertLevel = AlertLevel.WARNING,
                title = "💊 BỎ LỠ CỮ THUỐC: QUÁ 30 PHÚT",
                message = "Chưa uống thuốc Amlodipine 5mg theo lịch hẹn buổi sáng (08:00).",
                address = null,
                timestamp = now - 5 * 3600 * 1000L,
                isRead = true,
                isResolved = false,
                familyId = "fam_default"
            )
        )
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                return AlertViewModel(alertDao = db.alertDao()) as T
            }
        }
    }
}
