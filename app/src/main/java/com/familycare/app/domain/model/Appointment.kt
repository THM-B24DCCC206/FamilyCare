package com.familycare.app.domain.model

data class Appointment(
    val id: String = "",
    val userId: String = "",
    val hospitalName: String,     // Tên bệnh viện / phòng khám
    val department: String = "",   // Chuyên khoa (Tim mạch, Nội tiết,...)
    val doctorName: String = "",   // Bác sĩ khám
    val appointmentTime: Long,    // Thời gian khám (timestamp)
    val notes: String = "",       // Dặn dò (nhịn ăn sáng, mang theo sổ khám cũ)
    val isCompleted: Boolean = false
)
