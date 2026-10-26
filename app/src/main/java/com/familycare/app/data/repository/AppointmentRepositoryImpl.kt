
package com.familycare.app.data.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.AppointmentDao
import com.familycare.app.core.database.AppointmentEntity
import com.familycare.app.domain.model.Appointment
import com.familycare.app.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppointmentRepositoryImpl(
    private val appointmentDao: AppointmentDao
) : AppointmentRepository {

    override fun getAppointmentsByMember(
        userId: String
    ): Flow<List<Appointment>> {
        return appointmentDao.getAppointmentsByMember(userId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun addAppointment(
        appointment: Appointment
    ): Resource<Unit> {
        return try {
            appointmentDao.insertAppointment(
                AppointmentEntity.fromDomain(appointment)
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể thêm lịch khám")
        }
    }

    override suspend fun updateAppointment(
        appointment: Appointment
    ): Resource<Unit> {
        return try {
            appointmentDao.insertAppointment(
                AppointmentEntity.fromDomain(appointment)
            )
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể cập nhật lịch khám")
        }
    }

    override suspend fun deleteAppointment(
        appointmentId: String
    ): Resource<Unit> {
        return try {
            appointmentDao.deleteAppointment(appointmentId)
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Không thể xóa lịch khám")
        }
    }
}
