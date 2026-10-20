package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.Medicine

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val name: String,
    val dosage: String,
    val instruction: String,
    val reminderTimes: List<String>,
    val startDate: Long,
    val endDate: Long? = null,
    val scannedViaOcr: Boolean = false
) {
    fun toDomain(): Medicine {
        return Medicine(
            id = id,
            userId = userId,
            name = name,
            dosage = dosage,
            instruction = instruction,
            reminderTimes = reminderTimes,
            startDate = startDate,
            endDate = endDate,
            scannedViaOcr = scannedViaOcr
        )
    }

    companion object {
        fun fromDomain(model: Medicine): MedicineEntity {
            return MedicineEntity(
                id = model.id.ifEmpty { java.util.UUID.randomUUID().toString() },
                userId = model.userId,
                name = model.name,
                dosage = model.dosage,
                instruction = model.instruction,
                reminderTimes = model.reminderTimes,
                startDate = model.startDate,
                endDate = model.endDate,
                scannedViaOcr = model.scannedViaOcr
            )
        }
    }
}
