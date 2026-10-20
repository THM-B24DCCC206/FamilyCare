package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.Appointment
import kotlinx.coroutines.flow.Flow

interface AppointmentRepository {
    fun getAppointmentsByMember(userId: String): Flow<List<Appointment>>
    suspend fun addAppointment(appointment: Appointment): Resource<Unit>
    suspend fun updateAppointment(appointment: Appointment): Resource<Unit>
    suspend fun deleteAppointment(appointmentId: String): Resource<Unit>
}
