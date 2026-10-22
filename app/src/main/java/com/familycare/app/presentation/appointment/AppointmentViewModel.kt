package com.familycare.app.presentation.appointment

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.data.repository.AppointmentRepositoryImpl
import com.familycare.app.domain.model.Appointment
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class AppointmentViewModel(application: Application) :
    AndroidViewModel(application) {

    private val appointmentDao =
        AppDatabase.getInstance(application).appointmentDao

    private val repository =
        AppointmentRepositoryImpl(appointmentDao)

    // Bộ đặt và hủy nhắc lịch khám.
    private val alarmScheduler =
        AppointmentAlarmScheduler(application)

    // ID thử nghiệm, sẽ thay bằng ID người dùng thật khi có đăng nhập.
    private val userId = "demo-user"

    val appointments = repository
        .getAppointmentsByMember(userId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Thêm lịch khám và đặt nhắc sau khi lưu thành công.
    fun addAppointment(
        hospitalName: String,
        department: String,
        doctorName: String,
        appointmentTime: Long,
        notes: String
    ) {
        if (hospitalName.isBlank() || appointmentTime <= 0L) return

        viewModelScope.launch {
            val appointment = Appointment(
                id = UUID.randomUUID().toString(),
                userId = userId,
                hospitalName = hospitalName.trim(),
                department = department.trim(),
                doctorName = doctorName.trim(),
                appointmentTime = appointmentTime,
                notes = notes.trim()
            )

            when (repository.addAppointment(appointment)) {
                is Resource.Success -> {
                    alarmScheduler.schedule(
                        appointmentId = appointment.id,
                        hospitalName = appointment.hospitalName,
                        appointmentTime = appointment.appointmentTime
                    )
                }

                is Resource.Error -> {
                    // Không đặt nhắc nếu lưu lịch thất bại.
                }

                is Resource.Loading -> Unit
            }
        }
    }

    // Cập nhật lịch và đồng bộ lại báo thức.
    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            when (repository.updateAppointment(appointment)) {
                is Resource.Success -> {
                    // Hủy lịch nhắc cũ trước khi đặt lại.
                    alarmScheduler.cancel(appointment.id)

                    if (!appointment.isCompleted) {
                        alarmScheduler.schedule(
                            appointmentId = appointment.id,
                            hospitalName = appointment.hospitalName,
                            appointmentTime = appointment.appointmentTime
                        )
                    }
                }

                is Resource.Error -> Unit

                is Resource.Loading -> Unit
            }
        }
    }

    // Xóa lịch thành công thì mới hủy báo thức.
    fun deleteAppointment(appointmentId: String) {
        if (appointmentId.isBlank()) return

        viewModelScope.launch {
            when (repository.deleteAppointment(appointmentId)) {
                is Resource.Success -> {
                    alarmScheduler.cancel(appointmentId)
                }

                is Resource.Error -> Unit

                is Resource.Loading -> Unit
            }
        }
    }

    // Đánh dấu hoàn thành cũng sẽ hủy nhắc lịch.
    fun markAsCompleted(appointment: Appointment) {
        updateAppointment(
            appointment.copy(isCompleted = true)
        )
    }
}