package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: String = "",
    val memberName: String = "",
    val alertType: AlertType = AlertType.THRESHOLD_EXCEEDED,
    val alertLevel: AlertLevel = AlertLevel.WARNING,
    val title: String = "",
    val message: String = "",
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val isResolved: Boolean = false,
    val familyId: String = ""
) {
    // Thuộc tính tương thích cho các module cũ
    val targetUserId: String get() = memberId
    val targetUserName: String get() = memberName
    val type: String get() = alertType.name
    val level: String get() = alertLevel.name

    fun toDomain(): Alert {
        return Alert(
            id = id.toString(),
            familyId = familyId,
            targetUserId = memberId,
            targetUserName = memberName,
            type = alertType,
            level = alertLevel,
            title = title,
            message = message,
            timestamp = timestamp,
            isRead = isRead,
            isResolved = isResolved,
            address = address,
            latitude = latitude,
            longitude = longitude
        )
    }

    fun toDomainModel(): Alert = toDomain()

    companion object {
        fun fromDomain(model: Alert): AlertEntity {
            val idParsed = model.id.toLongOrNull() ?: System.currentTimeMillis()
            return AlertEntity(
                id = idParsed,
                memberId = model.targetUserId.ifBlank { model.memberId },
                memberName = model.targetUserName.ifBlank { model.memberName },
                alertType = model.type,
                alertLevel = model.level,
                title = model.title,
                message = model.message,
                address = model.address,
                latitude = model.latitude,
                longitude = model.longitude,
                timestamp = if (model.timestamp > 0) model.timestamp else System.currentTimeMillis(),
                isRead = model.isRead,
                isResolved = model.isResolved,
                familyId = model.familyId
            )
        }

        fun fromDomainModel(alert: Alert): AlertEntity = fromDomain(alert)
    }
}
