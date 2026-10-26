package com.familycare.app.presentation.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.AppointmentEntity
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.core.database.UserEntity
import com.familycare.app.domain.model.Appointment
import com.familycare.app.domain.model.Medicine
import com.familycare.app.presentation.appointment.AppointmentAlarmScheduler
import com.familycare.app.presentation.home.Member
import com.familycare.app.presentation.medication.MedicationAlarmScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class HealthScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val userDao = db.userDao()
    private val medicineDao = db.medicineDao
    private val medicationLogDao = db.medicationLogDao
    private val appointmentDao = db.appointmentDao
    private val sessionManager = SessionManager(application)

    private val medicationAlarmScheduler = MedicationAlarmScheduler(application)
    private val appointmentAlarmScheduler = AppointmentAlarmScheduler(application)

    private val _uiState = MutableStateFlow(HealthScheduleUiState(isLoading = true))
    val uiState: StateFlow<HealthScheduleUiState> = _uiState.asStateFlow()

    private var activeMemberJob: Job? = null

    init {
        loadFamilyMembersAndInitialData()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
    }

    fun selectTab(tab: HealthScheduleTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectMember(memberId: String) {
        if (memberId.isBlank() || memberId == _uiState.value.selectedMemberId) return
        _uiState.update { it.copy(selectedMemberId = memberId) }
        observeDataForMember(memberId)
    }

    fun loadFamilyMembersAndInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val members = loadRealFamilyMembers()
                val currentUserId = sessionManager.userId.ifBlank { members.firstOrNull()?.id ?: "" }
                val targetMemberId = if (_uiState.value.selectedMemberId.isNotBlank() &&
                    members.any { it.id == _uiState.value.selectedMemberId }
                ) {
                    _uiState.value.selectedMemberId
                } else {
                    currentUserId
                }

                _uiState.update {
                    it.copy(
                        familyMembers = members,
                        selectedMemberId = targetMemberId,
                        isLoading = false
                    )
                }

                if (targetMemberId.isNotBlank()) {
                    observeDataForMember(targetMemberId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Lỗi tải danh sách thành viên"
                    )
                }
            }
        }
    }

    private suspend fun loadRealFamilyMembers(): List<Member> = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.userId.ifBlank { "user_me" }
        val currentUserName = sessionManager.userName.ifBlank { "Tôi" }
        val currentUserEmail = sessionManager.userEmail
        val currentRole = sessionManager.userRole

        // Xóa sạch các ID mẫu cũ nếu có
        runCatching {
            userDao.deleteUserById("user_01")
            userDao.deleteUserById("user_demo_01")
        }

        // Đảm bảo người dùng hiện tại có trong DB
        val existingCurrentUser = userDao.getUserById(currentUserId)
        if (existingCurrentUser == null) {
            userDao.insertUser(
                UserEntity(
                    id = currentUserId,
                    fullName = currentUserName,
                    email = currentUserEmail,
                    role = currentRole
                )
            )
        }

        val allDbUsers = userDao.getAllUsers()
        val membersList = mutableListOf<Member>()

        // 1. Luôn đưa tài khoản đang đăng nhập lên ĐẦU TIÊN
        val currentDbUser = allDbUsers.firstOrNull { it.id == currentUserId }
        val currentMember = Member(
            id = currentUserId,
            fullName = currentDbUser?.fullName ?: currentUserName,
            avatarUrl = currentDbUser?.avatarUrl?.takeIf { it.isNotBlank() },
            initials = (currentDbUser?.fullName ?: currentUserName).trim().take(1).uppercase(),
            role = if (currentRole == "ADMIN") "Trưởng gia đình" else "Thành viên"
        )
        membersList.add(currentMember)

        // 2. Thêm các thành viên khác
        allDbUsers.filter { it.id != currentUserId && it.id != "user_01" && it.id != "user_demo_01" }.forEach { u ->
            membersList.add(
                Member(
                    id = u.id,
                    fullName = u.fullName,
                    avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                    initials = u.fullName.trim().take(1).uppercase(),
                    role = if (u.role == "ADMIN") "Trưởng gia đình" else "Thành viên"
                )
            )
        }

        membersList
    }

    private fun observeDataForMember(memberId: String) {
        activeMemberJob?.cancel()
        activeMemberJob = viewModelScope.launch {
            val todayDate = getTodayDateString()

            // 1. Observe thuốc
            launch {
                medicineDao.getMedicinesByMember(memberId).collect { entities ->
                    val domainMedicines = entities.map { it.toDomain() }
                    _uiState.update { it.copy(medicines = domainMedicines) }
                    ensureTodayLogsForMedicines(domainMedicines, memberId, todayDate)
                }
            }

            // 2. Observe logs hôm nay
            launch {
                medicationLogDao.getLogsForDate(memberId, todayDate).collect { logs ->
                    _uiState.update { it.copy(todayLogs = logs) }
                }
            }

            // 3. Observe lịch khám
            launch {
                appointmentDao.getAppointmentsByMember(memberId).collect { entities ->
                    val domainAppointments = entities.map { it.toDomain() }
                    val now = System.currentTimeMillis()
                    // 24 giờ trước để tính cận ngày
                    val threshold = now - 24 * 60 * 60 * 1000L

                    val upcoming = domainAppointments.filter { !it.isCompleted && it.appointmentTime >= threshold }
                        .sortedBy { it.appointmentTime }
                    val past = domainAppointments.filter { it.isCompleted || it.appointmentTime < threshold }
                        .sortedByDescending { it.appointmentTime }

                    _uiState.update {
                        it.copy(
                            upcomingAppointments = upcoming,
                            pastAppointments = past
                        )
                    }
                }
            }
        }
    }

    private suspend fun ensureTodayLogsForMedicines(
        medicines: List<Medicine>,
        memberId: String,
        todayDate: String
    ) = withContext(Dispatchers.IO) {
        for (medicine in medicines) {
            for (time in medicine.reminderTimes) {
                val existingLog = medicationLogDao.getLog(medicine.id, todayDate, time)
                if (existingLog == null) {
                    val newLog = MedicationLogEntity(
                        id = UUID.randomUUID().toString(),
                        medicineId = medicine.id,
                        medicineName = medicine.name,
                        userId = memberId,
                        scheduledDate = todayDate,
                        scheduledTime = time,
                        isTaken = false,
                        isSkipped = false
                    )
                    medicationLogDao.insertIfAbsent(newLog)
                }
            }
        }
    }

    fun markMedicationTaken(medicine: Medicine, scheduledTime: String) {
        viewModelScope.launch {
            val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
            val todayDate = getTodayDateString()
            var log = medicationLogDao.getLog(medicine.id, todayDate, scheduledTime)

            if (log == null) {
                val newLog = MedicationLogEntity(
                    id = UUID.randomUUID().toString(),
                    medicineId = medicine.id,
                    medicineName = medicine.name,
                    userId = memberId,
                    scheduledDate = todayDate,
                    scheduledTime = scheduledTime,
                    isTaken = false,
                    isSkipped = false
                )
                medicationLogDao.insertIfAbsent(newLog)
                log = medicationLogDao.getLog(medicine.id, todayDate, scheduledTime)
            }

            if (log != null) {
                medicationLogDao.markAsTaken(log.id, System.currentTimeMillis())
            }
        }
    }

    fun addMedicine(
        name: String,
        dosage: String,
        instruction: String,
        reminderTimes: List<String>
    ) {
        if (name.isBlank() || dosage.isBlank()) return
        val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }

        viewModelScope.launch {
            val medicine = Medicine(
                id = UUID.randomUUID().toString(),
                userId = memberId,
                name = name.trim(),
                dosage = dosage.trim(),
                instruction = instruction.trim(),
                reminderTimes = reminderTimes.distinct().sorted()
            )

            medicineDao.insertMedicine(MedicineEntity.fromDomain(medicine))
            ensureTodayLogsForMedicines(listOf(medicine), memberId, getTodayDateString())

            medicationAlarmScheduler.schedule(
                medicine.id,
                medicine.name,
                medicine.dosage,
                medicine.reminderTimes
            )
        }
    }

    fun updateMedicine(
        medicine: Medicine,
        name: String,
        dosage: String,
        instruction: String,
        reminderTimes: List<String>
    ) {
        if (name.isBlank() || dosage.isBlank()) return

        viewModelScope.launch {
            val updated = medicine.copy(
                name = name.trim(),
                dosage = dosage.trim(),
                instruction = instruction.trim(),
                reminderTimes = reminderTimes.distinct().sorted()
            )

            medicationAlarmScheduler.cancel(medicine.id, medicine.reminderTimes)
            medicineDao.updateMedicine(MedicineEntity.fromDomain(updated))
            ensureTodayLogsForMedicines(listOf(updated), updated.userId, getTodayDateString())

            medicationAlarmScheduler.schedule(
                updated.id,
                updated.name,
                updated.dosage,
                updated.reminderTimes
            )
        }
    }

    fun deleteMedicine(medicineId: String) {
        viewModelScope.launch {
            val medicine = _uiState.value.medicines.firstOrNull { it.id == medicineId }
            if (medicine != null) {
                medicationAlarmScheduler.cancel(medicine.id, medicine.reminderTimes)
            }
            medicineDao.deleteMedicine(medicineId)
        }
    }

    fun addAppointment(
        hospitalName: String,
        department: String,
        doctorName: String,
        appointmentTime: Long,
        notes: String
    ) {
        if (hospitalName.isBlank() || appointmentTime <= 0L) return
        val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }

        viewModelScope.launch {
            val appointment = Appointment(
                id = UUID.randomUUID().toString(),
                userId = memberId,
                hospitalName = hospitalName.trim(),
                department = department.trim(),
                doctorName = doctorName.trim(),
                appointmentTime = appointmentTime,
                notes = notes.trim(),
                isCompleted = false
            )

            appointmentDao.insertAppointment(AppointmentEntity.fromDomain(appointment))
            appointmentAlarmScheduler.schedule(
                appointmentId = appointment.id,
                hospitalName = appointment.hospitalName,
                appointmentTime = appointment.appointmentTime
            )
        }
    }

    fun updateAppointment(appointment: Appointment) {
        viewModelScope.launch {
            appointmentAlarmScheduler.cancel(appointment.id)
            appointmentDao.insertAppointment(AppointmentEntity.fromDomain(appointment))
            if (!appointment.isCompleted && appointment.appointmentTime > System.currentTimeMillis()) {
                appointmentAlarmScheduler.schedule(
                    appointmentId = appointment.id,
                    hospitalName = appointment.hospitalName,
                    appointmentTime = appointment.appointmentTime
                )
            }
        }
    }

    fun markAppointmentCompleted(appointment: Appointment) {
        viewModelScope.launch {
            val updated = appointment.copy(isCompleted = true)
            appointmentAlarmScheduler.cancel(appointment.id)
            appointmentDao.insertAppointment(AppointmentEntity.fromDomain(updated))
        }
    }

    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            appointmentAlarmScheduler.cancel(appointmentId)
            appointmentDao.deleteAppointment(appointmentId)
        }
    }
}
