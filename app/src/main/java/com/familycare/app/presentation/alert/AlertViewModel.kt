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
        viewModelScope.launch {
            // Xóa sạch toàn bộ dữ liệu mẫu / giả lập cũ trong SQLite
            alertDao.deleteDummyAlerts()
        }
    }

    val uiState: StateFlow<AlertUiState> = combine(
        alertDao.getAllAlerts(),
        _selectedFilter
    ) { allAlerts, filter ->
        val filtered = when (filter) {
            AlertFilter.ALL -> allAlerts
            AlertFilter.CRITICAL -> allAlerts.filter { it.alertLevel == AlertLevel.CRITICAL }
            AlertFilter.WARNING -> allAlerts.filter { it.alertLevel == AlertLevel.WARNING }
        }

        AlertUiState(
            alerts = filtered,
            selectedFilter = filter,
            allCount = allAlerts.size,
            criticalCount = allAlerts.count { it.alertLevel == AlertLevel.CRITICAL },
            warningCount = allAlerts.count { it.alertLevel == AlertLevel.WARNING },
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
