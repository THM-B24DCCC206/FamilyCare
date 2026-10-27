package com.familycare.app.presentation.alert

import com.familycare.app.core.database.AlertEntity

enum class AlertFilter {
    ALL,
    CRITICAL,
    WARNING
}

data class AlertUiState(
    val alerts: List<AlertEntity> = emptyList(),
    val selectedFilter: AlertFilter = AlertFilter.ALL,
    val allCount: Int = 0,
    val criticalCount: Int = 0,
    val warningCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
