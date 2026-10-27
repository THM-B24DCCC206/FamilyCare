package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.familycare.app.domain.model.AlertType
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE familyId = :familyId ORDER BY timestamp DESC")
    fun getAlertsByFamily(familyId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE id = :alertId LIMIT 1")
    suspend fun getAlertById(alertId: Long): AlertEntity?

    suspend fun getAlertById(alertId: String): AlertEntity? = getAlertById(alertId.toLongOrNull() ?: 0L)

    @Query("SELECT * FROM alerts WHERE (memberId = :memberId OR targetUserId = :memberId) AND (alertType = :alertType OR type = :alertTypeStr) ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestAlertForMember(
        memberId: String,
        alertType: AlertType,
        alertTypeStr: String = alertType.name
    ): AlertEntity?

    @Query("SELECT * FROM alerts WHERE (memberId = :userId OR targetUserId = :userId) AND (alertType = :type OR type = :type) AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentAlertsByType(
        userId: String,
        type: String,
        sinceTimestamp: Long
    ): List<AlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity): Long

    @Query("UPDATE alerts SET isRead = :isRead WHERE id = :alertId")
    suspend fun updateReadStatus(alertId: Long, isRead: Boolean)

    @Query("UPDATE alerts SET isResolved = :isResolved WHERE id = :alertId")
    suspend fun updateResolvedStatus(alertId: Long, isResolved: Boolean)

    suspend fun markAsRead(alertId: Long) = updateReadStatus(alertId, true)

    suspend fun markAsRead(alertId: String) {
        val idLong = alertId.toLongOrNull()
        if (idLong != null) {
            updateReadStatus(idLong, true)
        }
    }

    suspend fun markAlertAsRead(alertId: String) = markAsRead(alertId)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: Long)

    suspend fun deleteAlert(alertId: String) {
        val idLong = alertId.toLongOrNull()
        if (idLong != null) {
            deleteAlert(idLong)
        }
    }
}
