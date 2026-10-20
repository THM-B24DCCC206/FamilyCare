package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.HealthMetric
import com.familycare.app.domain.model.MetricType

@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val type: String,               // BLOOD_PRESSURE, HEART_RATE, etc.
    val valuePrimary: Double,
    val valueSecondary: Double? = null,
    val unit: String,
    val timestamp: Long,
    val notes: String = "",
    val isFromCameraPpg: Boolean = false
) {
    fun toDomain(): HealthMetric {
        return HealthMetric(
            id = id,
            userId = userId,
            type = runCatching { MetricType.valueOf(type) }.getOrDefault(MetricType.HEART_RATE),
            valuePrimary = valuePrimary,
            valueSecondary = valueSecondary,
            unit = unit,
            timestamp = timestamp,
            notes = notes,
            isFromCameraPpg = isFromCameraPpg
        )
    }

    companion object {
        fun fromDomain(model: HealthMetric): HealthMetricEntity {
            return HealthMetricEntity(
                id = model.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                userId = model.userId,
                type = model.type.name,
                valuePrimary = model.valuePrimary,
                valueSecondary = model.valueSecondary,
                unit = model.unit,
                timestamp = model.timestamp,
                notes = model.notes,
                isFromCameraPpg = model.isFromCameraPpg
            )
        }
    }
}
