package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.HealthMetric
import com.familycare.app.domain.model.MetricType
import kotlinx.coroutines.flow.Flow

interface HealthMetricRepository {
    fun getMetricsByMember(userId: String, type: MetricType? = null): Flow<List<HealthMetric>>
    suspend fun saveMetric(metric: HealthMetric): Resource<Unit>
    suspend fun deleteMetric(metricId: String): Resource<Unit>
}
