package com.familycare.app.data.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.dao.AlertDao
import com.familycare.app.core.database.entity.AlertEntity
import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType
import com.familycare.app.domain.repository.AlertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

class AlertRepositoryImpl(
    private val alertDao: AlertDao
) : AlertRepository {

    override fun getAlertsByFamily(familyId: String): Flow<List<Alert>> {
        return alertDao.getAlertsByFamily(familyId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun createAlert(alert: Alert): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val entity = AlertEntity.fromDomainModel(
                if (alert.id.isBlank()) alert.copy(id = UUID.randomUUID().toString()) else alert
            )
            alertDao.insertAlert(entity)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không thể tạo cảnh báo")
        }
    }

    override suspend fun markAlertAsRead(alertId: String): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            alertDao.markAlertAsRead(alertId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Không thể cập nhật trạng thái đã xem")
        }
    }

    override suspend fun triggerEmergencySos(
        userId: String,
        familyId: String,
        lat: Double?,
        lng: Double?
    ): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val sosAlert = Alert(
                id = UUID.randomUUID().toString(),
                familyId = familyId,
                targetUserId = userId,
                targetUserName = "Người thân",
                type = AlertType.SOS_EMERGENCY,
                level = AlertLevel.CRITICAL,
                title = "🚨 KHẨN CẤP: BÁO ĐỘNG SOS TỪ NGƯỜI THÂN",
                message = "Đã kích hoạt cảnh báo SOS! Vị trí GPS hiện tại: ${lat ?: 0.0}, ${lng ?: 0.0}",
                timestamp = System.currentTimeMillis(),
                isRead = false,
                latitude = lat,
                longitude = lng
            )
            alertDao.insertAlert(AlertEntity.fromDomainModel(sosAlert))
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.localizedMessage ?: "Lỗi khi kích hoạt SOS")
        }
    }
}
