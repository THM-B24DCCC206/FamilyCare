package com.familycare.app.presentation.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.AppointmentEntity
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.core.database.UserEntity
import com.familycare.app.domain.model.AlertLevel
import com.familycare.app.domain.model.AlertType
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
    private val alertDao = db.alertDao()
    private val sessionManager = SessionManager(application)

    private val medicationAlarmScheduler = MedicationAlarmScheduler(application)
    private val appointmentAlarmScheduler = AppointmentAlarmScheduler(application)

    private val _uiState = MutableStateFlow(HealthScheduleUiState(isLoading = true))
    val uiState: StateFlow<HealthScheduleUiState> = _uiState.asStateFlow()

    private var activeMemberJob: Job? = null
    private var currentLoadedDate: String = ""

    init {
        loadFamilyMembersAndInitialData()
        startDayChangeMonitor()
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun startDayChangeMonitor() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(30000L)
                val today = getTodayDateString()
                if (currentLoadedDate.isNotBlank() && today != currentLoadedDate) {
                    val memberId = _uiState.value.selectedMemberId
                    if (memberId.isNotBlank()) {
                        withContext(Dispatchers.Main) {
                            observeDataForMember(memberId)
                        }
                    }
                }
            }
        }
    }

    fun checkAndRefreshIfDayChanged() {
        val today = getTodayDateString()
        if (currentLoadedDate.isNotBlank() && today != currentLoadedDate) {
            val memberId = _uiState.value.selectedMemberId
            if (memberId.isNotBlank()) {
                observeDataForMember(memberId)
            }
        }
    }

    fun selectTab(tab: HealthScheduleTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun selectMedicationViewMode(mode: MedicationViewMode) {
        _uiState.update { it.copy(medicationViewMode = mode) }
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

        val fakeIds = setOf("user_01", "user_demo_01", "member_bac_nam", "member_bo", "member_me", "member_anhtrai", "user_02", "member_ly_tran")
        // Xóa sạch các ID mẫu cũ nếu có
        fakeIds.forEach { id ->
            runCatching { userDao.deleteUserById(id) }
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
        val currentDbUser = allDbUsers.firstOrNull { it.id == currentUserId }
        val currentFamilyId = currentDbUser?.familyId?.trim().orEmpty().ifBlank { sessionManager.familyId.trim() }

        val membersList = mutableListOf<Member>()

        // 1. Luôn đưa tài khoản đang đăng nhập lên ĐẦU TIÊN
        val currentMember = Member(
            id = currentUserId,
            fullName = currentDbUser?.fullName ?: currentUserName,
            avatarUrl = currentDbUser?.avatarUrl?.takeIf { it.isNotBlank() },
            initials = (currentDbUser?.fullName ?: currentUserName).trim().take(1).uppercase(),
            role = if (currentRole == "ADMIN" || currentDbUser?.role?.uppercase() == "ADMIN") "Trưởng gia đình" else "Thành viên"
        )
        membersList.add(currentMember)

        // 2. Thêm các thành viên khác CÙNG familyId với tài khoản hiện tại
        if (currentFamilyId.isNotBlank()) {
            allDbUsers.filter {
                it.id != currentUserId &&
                it.id !in fakeIds &&
                it.familyId.isNotBlank() &&
                it.familyId == currentFamilyId
            }.forEach { u ->
                membersList.add(
                    Member(
                        id = u.id,
                        fullName = u.fullName,
                        avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                        initials = u.fullName.trim().take(1).uppercase(),
                        role = if (u.role.uppercase() == "ADMIN") "Trưởng gia đình" else "Thành viên"
                    )
                )
            }
        }

        membersList
    }

    private fun observeDataForMember(memberId: String) {
        activeMemberJob?.cancel()
        activeMemberJob = viewModelScope.launch {
            val todayDate = getTodayDateString()
            currentLoadedDate = todayDate

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

    fun undoMedicationTaken(medicine: Medicine, scheduledTime: String) {
        viewModelScope.launch {
            val todayDate = getTodayDateString()
            val log = medicationLogDao.getLog(medicine.id, todayDate, scheduledTime)
            if (log != null) {
                medicationLogDao.markAsNotTaken(log.id)
            }
        }
    }

    fun addMedicineSession(
        sessionTime: String,
        commonInstruction: String,
        items: List<Pair<String, String>> // (name, dosage)
    ) {
        val validItems = items.filter { it.first.isNotBlank() }
        if (validItems.isEmpty() || sessionTime.isBlank()) return
        val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
        val todayDate = getTodayDateString()

        viewModelScope.launch {
            val newMedicines = validItems.map { (name, dosage) ->
                Medicine(
                    id = UUID.randomUUID().toString(),
                    userId = memberId,
                    name = name.trim(),
                    dosage = dosage.ifBlank { "1 viên" }.trim(),
                    instruction = commonInstruction.trim(),
                    reminderTimes = listOf(sessionTime),
                    purpose = ""
                )
            }

            for (medicine in newMedicines) {
                medicineDao.insertMedicine(MedicineEntity.fromDomain(medicine))
                medicationAlarmScheduler.schedule(
                    medicine.id,
                    medicine.name,
                    medicine.dosage,
                    medicine.reminderTimes
                )
            }
            ensureTodayLogsForMedicines(newMedicines, memberId, todayDate)
        }
    }

    fun markSessionTaken(sessionTime: String, medicines: List<Medicine>) {
        viewModelScope.launch {
            val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
            val todayDate = getTodayDateString()
            val now = System.currentTimeMillis()

            for (medicine in medicines) {
                var log = medicationLogDao.getLog(medicine.id, todayDate, sessionTime)
                if (log == null) {
                    val newLog = MedicationLogEntity(
                        id = UUID.randomUUID().toString(),
                        medicineId = medicine.id,
                        medicineName = medicine.name,
                        userId = memberId,
                        scheduledDate = todayDate,
                        scheduledTime = sessionTime,
                        actualTakenTime = now,
                        isTaken = true,
                        isSkipped = false
                    )
                    medicationLogDao.insertIfAbsent(newLog)
                    medicationLogDao.markAsTaken(newLog.id, now)
                } else {
                    medicationLogDao.markAsTaken(log.id, now)
                }
            }
        }
    }

    fun undoSessionTaken(sessionTime: String, medicines: List<Medicine>) {
        viewModelScope.launch {
            val todayDate = getTodayDateString()
            for (medicine in medicines) {
                val log = medicationLogDao.getLog(medicine.id, todayDate, sessionTime)
                if (log != null) {
                    medicationLogDao.markAsNotTaken(log.id)
                }
            }
        }
    }

    fun addMedicine(
        name: String,
        dosage: String,
        purpose: String = "",
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
                reminderTimes = reminderTimes.distinct().sorted(),
                purpose = purpose.trim()
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
        purpose: String = "",
        instruction: String,
        reminderTimes: List<String>
    ) {
        if (name.isBlank() || dosage.isBlank()) return

        viewModelScope.launch {
            val updated = medicine.copy(
                name = name.trim(),
                dosage = dosage.trim(),
                instruction = instruction.trim(),
                reminderTimes = reminderTimes.distinct().sorted(),
                purpose = purpose.trim()
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

            // Thêm thông báo chuông (AlertDao) nhắc nhở lịch khám
            val currentMember = _uiState.value.familyMembers.find { it.id == memberId }
            val memberName = currentMember?.fullName ?: "Thành viên"
            val dateFmt = SimpleDateFormat("HH:mm - EEE, dd/MM", Locale("vi", "VN")).format(Date(appointmentTime))
            val docOrDept = if (doctorName.isNotBlank()) "Bác sĩ $doctorName" else department
            alertDao.insertAlert(
                AlertEntity(
                    familyId = sessionManager.familyId.ifBlank { "FAM-8606" },
                    memberId = memberId,
                    memberName = memberName,
                    alertType = AlertType.APPOINTMENT_REMINDER,
                    alertLevel = AlertLevel.WARNING,
                    title = "🗓 Sắp đến lịch khám: ${appointment.hospitalName}",
                    message = "Lịch khám tại ${appointment.hospitalName} ($dateFmt) của $memberName. Bác sĩ/Khoa: $docOrDept.",
                    timestamp = System.currentTimeMillis(),
                    isRead = false,
                    isResolved = false
                )
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
