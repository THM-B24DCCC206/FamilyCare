package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthMetricDao {
    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY timestamp DESC")
    fun getMetricsByMember(userId: String): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY timestamp DESC")
    fun getMetricsByType(userId: String, type: String): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics WHERE userId = :userId AND type = :type ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestMetricByType(userId: String, type: String): HealthMetricEntity?

    @Query("SELECT * FROM health_metrics WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMetrics(userId: String, limit: Int = 10): Flow<List<HealthMetricEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetric(metric: HealthMetricEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMetrics(metrics: List<HealthMetricEntity>)

    @Query("DELETE FROM health_metrics WHERE id = :metricId")
    suspend fun deleteMetric(metricId: String)

    suspend fun deleteMetricById(metricId: String) = deleteMetric(metricId)
}
