package com.familycare.app.presentation.schedule

import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.domain.model.Appointment
import com.familycare.app.domain.model.Medicine
import com.familycare.app.presentation.home.Member

enum class HealthScheduleTab {
    MEDICATION,
    APPOINTMENT
}

enum class MedicationViewMode {
    SESSIONS,
    CABINET
}

data class HealthScheduleUiState(
    val isLoading: Boolean = false,
    val familyMembers: List<Member> = emptyList(),
    val selectedMemberId: String = "",
    val selectedTab: HealthScheduleTab = HealthScheduleTab.MEDICATION,
    val medicationViewMode: MedicationViewMode = MedicationViewMode.SESSIONS,
    val medicines: List<Medicine> = emptyList(),
    val todayLogs: List<MedicationLogEntity> = emptyList(),
    val upcomingAppointments: List<Appointment> = emptyList(),
    val pastAppointments: List<Appointment> = emptyList(),
    val errorMessage: String? = null
)
