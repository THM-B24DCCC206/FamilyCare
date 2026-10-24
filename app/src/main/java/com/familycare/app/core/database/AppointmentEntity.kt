
package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.Appointment
import java.util.UUID

@Entity(tableName = "appointments")
data class AppointmentEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val hospitalName: String,
    val department: String,
    val doctorName: String,
    val appointmentTime: Long,
    val notes: String,
    val isCompleted: Boolean
) {
    fun toDomain(): Appointment {
        return Appointment(
            id = id,
            userId = userId,
            hospitalName = hospitalName,
            department = department,
            doctorName = doctorName,
            appointmentTime = appointmentTime,
            notes = notes,
            isCompleted = isCompleted
        )
    }

    companion object {
        fun fromDomain(appointment: Appointment): AppointmentEntity {
            return AppointmentEntity(
                id = appointment.id.ifBlank { UUID.randomUUID().toString() },
                userId = appointment.userId,
                hospitalName = appointment.hospitalName,
                department = appointment.department,
                doctorName = appointment.doctorName,
                appointmentTime = appointment.appointmentTime,
                notes = appointment.notes,
                isCompleted = appointment.isCompleted
            )
        }
    }
}
