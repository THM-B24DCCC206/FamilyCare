package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.Alert
import kotlinx.coroutines.flow.Flow

interface AlertRepository {
    fun getAlertsByFamily(familyId: String): Flow<List<Alert>>
    suspend fun createAlert(alert: Alert): Resource<Unit>
    suspend fun markAlertAsRead(alertId: String): Resource<Unit>
    suspend fun triggerEmergencySos(userId: String, familyId: String, lat: Double?, lng: Double?): Resource<Unit>
}
