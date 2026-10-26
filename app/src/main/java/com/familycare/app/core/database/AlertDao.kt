package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts WHERE familyId = :familyId ORDER BY timestamp DESC")
    fun getAlertsByFamily(familyId: String): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlerts(): Flow<List<AlertEntity>>

    @Query("SELECT * FROM alerts WHERE targetUserId = :userId AND type = :type AND timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getRecentAlertsByType(
        userId: String,
        type: String,
        sinceTimestamp: Long
    ): List<AlertEntity>

    @Query("SELECT * FROM alerts WHERE id = :alertId LIMIT 1")
    suspend fun getAlertById(alertId: String): AlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: AlertEntity)

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: String)

    suspend fun markAlertAsRead(alertId: String) = markAsRead(alertId)

    @Query("DELETE FROM alerts WHERE id = :alertId")
    suspend fun deleteAlert(alertId: String)
}
