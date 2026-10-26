
package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "medication_logs",
    indices = [
        Index(value = ["medicineId", "scheduledDate", "scheduledTime"], unique = true),
        Index(value = ["userId", "scheduledDate"])
    ]
)
data class MedicationLogEntity(
    @PrimaryKey
    val id: String,

    val medicineId: String,
    val medicineName: String,
    val userId: String,

    // Ngày uống theo định dạng yyyy-MM-dd
    val scheduledDate: String,

    // Giờ uống theo định dạng HH:mm
    val scheduledTime: String,

    // Thời điểm xác nhận đã uống, tính bằng milliseconds
    val actualTakenTime: Long? = null,

    val isTaken: Boolean = false,
    val isSkipped: Boolean = false
)
