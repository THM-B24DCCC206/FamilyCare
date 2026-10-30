package com.familycare.app.presentation.home

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class Member(
    val id: String,
    val fullName: String,
    val avatarUrl: String? = null,
    val initials: String = fullName.trim().take(1).uppercase(),
    val role: String = "Thành viên"
)

data class HealthAlert(
    val id: String,
    val title: String,
    val message: String,
    val level: String = "CRITICAL",
    val timestamp: Long = System.currentTimeMillis()
)

data class VitalSummary(
    val heartRateBpm: Int? = null,
    val bloodPressureSystolic: Int? = null,
    val bloodPressureDiastolic: Int? = null,
    val isHeartRateWarning: Boolean = false,
    val isBloodPressureWarning: Boolean = false,
    val bloodSugar: Double? = null,
    val temperature: Double? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val hasMeasuredBp: Boolean get() = (bloodPressureSystolic ?: 0) > 0 && (bloodPressureDiastolic ?: 0) > 0
    val hasMeasuredHr: Boolean get() = (heartRateBpm ?: 0) > 0
    val hasMeasuredTemp: Boolean get() = (temperature ?: 0.0) > 0.0
    val hasMeasuredSugar: Boolean get() = (bloodSugar ?: 0.0) > 0.0
}

enum class DoseStatus {
    UPCOMING, // Chưa đến giờ (> 30 mins before scheduledTime)
    DUE,      // Đến giờ hoặc sắp đến (-30 mins to +60 mins)
    OVERDUE,  // Quá giờ (> 60 mins past scheduledTime and not taken/skipped)
    TAKEN,    // Đã uống
    SKIPPED   // Đã bỏ qua
}

data class MedicationSchedule(
    val id: String,
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val instruction: String,
    val scheduledTime: String,
    val isTaken: Boolean = false,
    val isSkipped: Boolean = false,
    val actualTakenTime: Long? = null
) {
    fun computeStatus(currentMinutesOfDay: Int? = null): DoseStatus {
        if (isTaken) return DoseStatus.TAKEN
        if (isSkipped) return DoseStatus.SKIPPED

        return try {
            val parts = scheduledTime.split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toInt()
                val min = parts[1].trim().toInt()
                val schedMinutes = hour * 60 + min

                val nowMinutes = currentMinutesOfDay ?: run {
                    val cal = Calendar.getInstance()
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }

                val diffMinutes = nowMinutes - schedMinutes // > 0: past scheduled time, < 0: before scheduled time

                when {
                    diffMinutes < -30 -> DoseStatus.UPCOMING
                    diffMinutes <= 60 -> DoseStatus.DUE
                    else -> DoseStatus.OVERDUE
                }
            } else {
                DoseStatus.DUE
            }
        } catch (_: Exception) {
            DoseStatus.DUE
        }
    }

    val status: DoseStatus get() = computeStatus()

    val takenTimeFormatted: String? get() {
        return actualTakenTime?.let {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
        }
    }
}

data class AppointmentItem(
    val id: String,
    val title: String,
    val timeFormatted: String,
    val doctorOrDepartment: String
)

data class BleDevice(
    val name: String,
    val address: String,
    val isConnected: Boolean,
    val batteryLevel: Int = 100,
    val liveHeartRate: Int? = null
)

data class HomeUiState(
    val familyMembers: List<Member> = emptyList(),
    val selectedMemberId: String = "",
    val familyName: String = "",
    val alertStatus: HealthAlert? = null,
    val latestVitals: VitalSummary? = null,
    val todayMedications: List<MedicationSchedule> = emptyList(),
    val upcomingAppointment: AppointmentItem? = null,
    val connectedDevice: BleDevice? = null,
    val unreadNotificationCount: Int = 0,
    val inviteCode: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
