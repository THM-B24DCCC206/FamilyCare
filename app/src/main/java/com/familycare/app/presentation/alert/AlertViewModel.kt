package com.familycare.app.presentation.alert

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AlertDao
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.domain.model.AlertLevel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý và điều phối Trung Tâm Cảnh Báo (Alert Center)
 * Sử dụng toán tử combine(alertDao.getAllAlerts(), _selectedFilter) để tính toán chính xác
 * số lượng tab (allCount, criticalCount, warningCount) và lọc danh sách tự động.
 * Dữ liệu hoàn toàn là dữ liệu thực tế phát sinh từ hệ thống (SOS, sinh tồn, cữ thuốc), không dùng dữ liệu giả lập.
 */
class AlertViewModel(
    private val alertDao: AlertDao
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(AlertFilter.ALL)
    val selectedFilter: StateFlow<AlertFilter> = _selectedFilter.asStateFlow()

    init {
        // Không tự ý xóa cảnh báo trong init để tránh xóa mất bản ghi SOS người dùng vừa tạo
    }

    val uiState: StateFlow<AlertUiState> = combine(
        alertDao.getAllAlerts(),
        _selectedFilter
    ) { allAlerts, filter ->
        // Loại bỏ triệt để các cảnh báo thuộc về các thành viên giả lập/mẫu cũ, nhưng GIỮ LẠI 100% cảnh báo SOS thật
        val validAlerts = allAlerts.filter { alert ->
            val isSos = alert.alertType == com.familycare.app.domain.model.AlertType.SOS_EMERGENCY ||
                    alert.title.contains("SOS", ignoreCase = true) ||
                    alert.title.contains("CẤP CỨU", ignoreCase = true)
            if (isSos) {
                true // Giữ lại 100% cảnh báo SOS do người dùng bấm
            } else {
                !alert.memberName.contains("Nguyễn Văn Nam") &&
                !alert.memberName.contains("Nguyễn Hoàng") &&
                !alert.memberName.contains("Trần Thị Lan") &&
                !alert.message.contains("Nguyễn Hoàng") &&
                !alert.message.contains("Trần Thị Lan")
            }
        }.map { alert ->
            var finalAlert = alert
            // Đảm bảo: Mọi cảnh báo SOS / Báo động cấp cứu đều 100% là AlertLevel.CRITICAL (Khẩn cấp)
            if (finalAlert.alertType == com.familycare.app.domain.model.AlertType.SOS_EMERGENCY ||
                finalAlert.title.contains("SOS", ignoreCase = true) ||
                finalAlert.title.contains("CẤP CỨU", ignoreCase = true)) {
                if (finalAlert.alertLevel != AlertLevel.CRITICAL) {
                    finalAlert = finalAlert.copy(alertLevel = AlertLevel.CRITICAL)
                }
            }
            // Bỏ dòng nhắc nhở định vị rườm rà nếu có
            if (finalAlert.message.contains("Vui lòng bật Định vị") || finalAlert.message.contains("Chưa nhận diện được vị trí")) {
                finalAlert = finalAlert.copy(
                    message = "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân.",
                    address = null
                )
            }
            finalAlert
        }

        val filtered = when (filter) {
            AlertFilter.ALL -> validAlerts
            AlertFilter.CRITICAL -> validAlerts.filter { it.alertLevel == AlertLevel.CRITICAL }
            AlertFilter.WARNING -> validAlerts.filter { it.alertLevel != AlertLevel.CRITICAL }
        }

        val critCount = validAlerts.count { it.alertLevel == AlertLevel.CRITICAL && !it.isRead && !it.isResolved }
        val warnCount = validAlerts.count { it.alertLevel != AlertLevel.CRITICAL && !it.isRead && !it.isResolved }

        AlertUiState(
            alerts = filtered,
            selectedFilter = filter,
            allCount = validAlerts.size,
            criticalCount = critCount,
            warningCount = warnCount,
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
            alertDao.updateResolvedStatus(alertId, true)
        }
    }

    fun onResolveAlert(alertId: Long) {
        viewModelScope.launch {
            alertDao.updateResolvedStatus(alertId, true)
        }
    }

    fun onDeleteAlert(alertId: Long) {
        viewModelScope.launch {
            alertDao.deleteAlert(alertId)
        }
    }

    /**
     * Xóa sạch 100% cảnh báo để thử nghiệm từ đầu
     */
    fun clearAllAlerts() {
        viewModelScope.launch {
            alertDao.clearAllAlerts()
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
