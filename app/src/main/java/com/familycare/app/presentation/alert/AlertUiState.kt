package com.familycare.app.presentation.alert

import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.AlertLevel

data class AlertUiState(
    val alerts: List<Alert> = emptyList(),
    val isLoading: Boolean = false,
    val selectedFilter: AlertLevel? = null, // null: Tất cả
    val errorMessage: String? = null
)
