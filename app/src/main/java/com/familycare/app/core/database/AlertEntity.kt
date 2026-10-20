package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey
    val id: String,
    val familyId: String,
    val targetUserId: String,
    val targetUserName: String,
    val type: String,
    val level: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    fun toDomain(): Alert {
        return Alert(
            id = id,
            familyId = familyId,
            targetUserId = targetUserId,
            targetUserName = targetUserName,
            type = runCatching { AlertType.valueOf(type) }.getOrDefault(AlertType.THRESHOLD_EXCEEDED),
            level = runCatching { AlertLevel.valueOf(level) }.getOrDefault(AlertLevel.WARNING),
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            latitude = latitude,
            longitude = longitude
        )
    }

    companion object {
        fun fromDomain(model: Alert): AlertEntity {
            return AlertEntity(
                id = model.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                familyId = model.familyId,
                targetUserId = model.targetUserId,
                targetUserName = model.targetUserName,
                type = model.type.name,
                level = model.level.name,
                title = model.title,
                message = model.message,
                timestamp = model.timestamp,
                isRead = model.isRead,
                latitude = model.latitude,
                longitude = model.longitude
            )
        }
    }
}
