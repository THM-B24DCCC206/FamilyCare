package com.familycare.app.presentation.home

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
    val hasMeasuredBp: Boolean get() = (bloodPressureSystolic ?: 0) > 0
    val hasMeasuredHr: Boolean get() = (heartRateBpm ?: 0) > 0
}

data class MedicationSchedule(
    val id: String,
    val medicineId: String,
    val medicineName: String,
    val dosage: String,
    val instruction: String,
    val scheduledTime: String,
    val isTaken: Boolean = false
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
    val alertStatus: HealthAlert? = null,
    val latestVitals: VitalSummary? = null,
    val todayMedications: List<MedicationSchedule> = emptyList(),
    val connectedDevice: BleDevice? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)
