package com.familycare.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.ble.SmartwatchBleManager
import com.familycare.app.core.common.Resource
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.*
import com.familycare.app.domain.model.*
import com.familycare.app.domain.usecase.AnalyzeThresholdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomeViewModel(
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val healthMetricDao: HealthMetricDao,
    private val alertDao: AlertDao,
    private val medicineDao: MedicineDao,
    private val medicationLogDao: MedicationLogDao,
    private val analyzeThresholdUseCase: AnalyzeThresholdUseCase,
    private val bleManager: SmartwatchBleManager,
    private val appointmentDao: AppointmentDao? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var activeMedicationJob: kotlinx.coroutines.Job? = null
    private var currentLoadedDate: String = ""

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    init {
        observeBleDevice()
        observeNotifications()
        loadInitialData()
        startDayChangeMonitor()
    }

    private fun startDayChangeMonitor() {
        viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                kotlinx.coroutines.delay(30000L)
                val today = getTodayDateString()
                if (currentLoadedDate.isNotBlank() && today != currentLoadedDate) {
                    withContext(Dispatchers.Main) {
                        refresh()
                    }
                }
            }
        }
    }

    fun checkAndRefreshIfDayChanged() {
        val today = getTodayDateString()
        if (currentLoadedDate.isNotBlank() && today != currentLoadedDate) {
            refresh()
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            alertDao.getAllAlerts().collect { alerts ->
                val unreadCount = alerts.count { !it.isRead }
                _uiState.update { it.copy(unreadNotificationCount = unreadCount) }
            }
        }
    }

    private fun observeBleDevice() {
        viewModelScope.launch {
            combine(
                bleManager.currentDevice,
                bleManager.connectionState,
                bleManager.telemetry
            ) { device, state, telemetry ->
                if (device != null && state == SmartwatchConnectionState.CONNECTED) {
                    BleDevice(
                        name = device.name,
                        address = device.address,
                        isConnected = true,
                        batteryLevel = telemetry.batteryLevel,
                        liveHeartRate = if (telemetry.isRealTimeMeasuring) telemetry.heartRate else null
                    )
                } else null
            }.collect { ble ->
                _uiState.update { it.copy(connectedDevice = ble) }
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                withContext(Dispatchers.IO) {
                    // 1. Đồng bộ và lấy danh sách thành viên thực tế (Luôn ưu tiên tài khoản đăng nhập)
                    val members = loadRealFamilyMembers()

                    // Đồng bộ tên tổ ấm cho tài khoản thành viên (nếu DB hoặc Session chưa kịp nạp)
                    val activeFam = userDao.getActiveFamily()
                    val resolvedFamName = activeFam?.familyName?.takeIf { it.isNotBlank() }
                        ?: sessionManager.familyName.takeIf { it.isNotBlank() && it != "Gia đình" }
                        ?: "Tổ Ấm Yêu Thương"

                    if (sessionManager.familyName.isBlank() || sessionManager.familyName == "Gia đình") {
                        sessionManager.saveFamilySession(
                            familyId = activeFam?.familyId ?: sessionManager.familyId.ifBlank { "fam_default" },
                            familyName = resolvedFamName,
                            inviteCode = activeFam?.inviteCode ?: sessionManager.inviteCode.ifBlank { "FAM-8606" }
                        )
                    }

                    // Thành viên được chọn mặc định luôn là người dùng đang đăng nhập
                    val currentUserId = sessionManager.userId.ifBlank { members.firstOrNull()?.id ?: "" }
                    val selectedId = if (_uiState.value.selectedMemberId.isNotBlank() &&
                        members.any { it.id == _uiState.value.selectedMemberId }
                    ) {
                        _uiState.value.selectedMemberId
                    } else {
                        currentUserId
                    }

                    val currentInviteCode = activeFam?.inviteCode ?: sessionManager.inviteCode.ifBlank { "FAM-8606" }

                    _uiState.update {
                        it.copy(
                            familyMembers = members,
                            selectedMemberId = selectedId,
                            familyName = resolvedFamName,
                            inviteCode = currentInviteCode
                        )
                    }

                    if (selectedId.isNotBlank()) {
                        fetchMemberDetails(selectedId)
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage ?: "Lỗi tải dữ liệu gia đình"
                    )
                }
            }
        }
    }

    /**
     * Tải danh sách thành viên thực tế dựa vào tài khoản đang đăng nhập trong SessionManager.
     * Xóa sạch các tài khoản giả (user_01, user_demo_01) đã bị gán cứng trước đó.
     */
    private suspend fun loadRealFamilyMembers(): List<Member> = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.userId.ifBlank { "user_me" }
        val currentUserName = sessionManager.userName.ifBlank { "Tôi" }
        val currentUserEmail = sessionManager.userEmail
        val currentRole = sessionManager.userRole

        // Xóa sạch các ID mẫu cứng cũ nếu có
        val fakeIds = listOf("user_01", "user_demo_01", "member_bac_nam", "member_bo", "member_me", "member_anhtrai", "user_02", "member_ly_tran")
        fakeIds.forEach { id ->
            runCatching { userDao.deleteUserById(id) }
        }

        // Đảm bảo người dùng hiện tại đã có trong bảng users của SQLite
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

        // 2. Thêm các thành viên gia đình khác (CHỈ lấy thành viên CÙNG familyId với tài khoản hiện tại)
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

    fun refreshHomeData() {
        loadInitialData()
    }

    /**
     * Tải dữ liệu thực tế của thành viên:
     * - Nếu chưa đo lần nào: Để trống (0) và KHÔNG tự bịa cảnh báo đỏ!
     * - Nếu chưa có thuốc: Để danh sách rỗng!
     */
    private suspend fun fetchMemberDetails(memberId: String) = withContext(Dispatchers.IO) {
        val todayStr = getTodayDateString()
        currentLoadedDate = todayStr

        // 1. Chỉ số sinh tồn thực tế (KHÔNG seed số giả nếu chưa đo)
        val bpEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.BLOOD_PRESSURE.name)
        val hrEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.HEART_RATE.name)
        val sugarEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.BLOOD_SUGAR.name)
        val tempEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.TEMPERATURE.name)

        val systolic = bpEntity?.valuePrimary?.toInt() ?: 0
        val diastolic = bpEntity?.valueSecondary?.toInt() ?: 0
        val hr = hrEntity?.valuePrimary?.toInt() ?: 0

        val hasBp = bpEntity != null && systolic > 0
        val hasHr = hrEntity != null && hr > 0

        val isBpWarning = hasBp && (systolic >= 140 || diastolic >= 90)
        val isHrWarning = hasHr && (hr > 100 || hr < 55)

        val vitals = VitalSummary(
            heartRateBpm = hr,
            bloodPressureSystolic = systolic,
            bloodPressureDiastolic = diastolic,
            isHeartRateWarning = isHrWarning,
            isBloodPressureWarning = isBpWarning,
            bloodSugar = sugarEntity?.valuePrimary,
            temperature = tempEntity?.valuePrimary,
            lastUpdated = bpEntity?.timestamp ?: hrEntity?.timestamp ?: System.currentTimeMillis()
        )

        // 2. Cảnh báo (Chỉ tạo cảnh báo nếu THỰC SỰ có lần đo và vượt ngưỡng an toàn)
        var alert: HealthAlert? = null
        if (hasBp || hasHr) {
            val recentMetrics = listOfNotNull(
                bpEntity?.toDomainModel(),
                hrEntity?.toDomainModel()
            )

            val currentMember = _uiState.value.familyMembers.firstOrNull { it.id == memberId }
            val memberName = currentMember?.fullName ?: "Thành viên"

            analyzeThresholdUseCase(
                userId = memberId,
                metrics = recentMetrics,
                userName = memberName,
                familyId = sessionManager.familyName
            ).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val result = res.data
                    if (result.isCriticalRedAlert) {
                        alert = HealthAlert(
                            id = "alert_${memberId}_threshold",
                            title = "⚠️ CẢNH BÁO: CHỈ SỐ HUYẾT ÁP VƯỢT NGƯỠNG AN TOÀN",
                            message = "Phát hiện $systolic/$diastolic mmHg. Vui lòng nghỉ ngơi hoặc liên hệ bác sĩ theo dõi.",
                            level = "CRITICAL",
                            timestamp = System.currentTimeMillis()
                        )
                    }
                }
            }

            if (alert == null && isBpWarning) {
                alert = HealthAlert(
                    id = "alert_${memberId}_bp",
                    title = "⚠️ CẢNH BÁO: HUYẾT ÁP TĂNG CAO BẤT THƯỜNG",
                    message = "Chỉ số gần nhất $systolic/$diastolic mmHg vượt ngưỡng 140/90. Vui lòng nghỉ ngơi và theo dõi sát sao.",
                    level = "CRITICAL",
                    timestamp = System.currentTimeMillis()
                )
            }
        }

        // 3. Lịch uống thuốc thực tế (Lấy từ medicineDao của đúng thành viên này)
        val initialSchedules = mutableListOf<MedicationSchedule>()
        try {
            val medicines = medicineDao.getMedicinesByMember(memberId).firstOrNull() ?: emptyList()
            val logs = medicationLogDao.getLogsForDate(memberId, todayStr).firstOrNull() ?: emptyList()
            val logMap = logs.associateBy { "${it.medicineId}_${it.scheduledTime}" }

            for (medicine in medicines) {
                for (time in medicine.reminderTimes) {
                    val log = logMap["${medicine.id}_$time"]
                    initialSchedules.add(
                        MedicationSchedule(
                            id = log?.id ?: "${medicine.id}_${todayStr}_$time",
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            instruction = medicine.instruction,
                            scheduledTime = time,
                            isTaken = log?.isTaken ?: false,
                            isSkipped = log?.isSkipped ?: false,
                            actualTakenTime = log?.actualTakenTime
                        )
                    )
                }
            }
            initialSchedules.sortBy { it.scheduledTime }
        } catch (_: Exception) {
            // Giữ danh sách rỗng nếu chưa có dữ liệu thuốc
        }

        // Bắt đầu quan sát reactive flow từ Room DB
        observeMedicationFlow(memberId, todayStr)

        // 4. Lịch khám / Cuộc hẹn y tế của thành viên (Phân biệt Hôm nay vs Sắp tới)
        var todayApptItem: AppointmentItem? = null
        var nextUpcomingApptItem: AppointmentItem? = null
        if (appointmentDao != null) {
            try {
                val apptList = appointmentDao.getAppointmentsByMember(memberId).firstOrNull() ?: emptyList()
                val upcoming = apptList.filter { !it.isCompleted && it.appointmentTime >= System.currentTimeMillis() - 3600_000L }
                    .minByOrNull { it.appointmentTime }
                if (upcoming != null) {
                    val calNow = Calendar.getInstance()
                    val calAppt = Calendar.getInstance().apply { timeInMillis = upcoming.appointmentTime }
                    val isToday = calNow.get(Calendar.YEAR) == calAppt.get(Calendar.YEAR) &&
                            calNow.get(Calendar.DAY_OF_YEAR) == calAppt.get(Calendar.DAY_OF_YEAR)

                    val isTomorrow = run {
                        val calTomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
                        calTomorrow.get(Calendar.YEAR) == calAppt.get(Calendar.YEAR) &&
                                calTomorrow.get(Calendar.DAY_OF_YEAR) == calAppt.get(Calendar.DAY_OF_YEAR)
                    }

                    val dateFmt = when {
                        isToday -> "Hôm nay, " + SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(upcoming.appointmentTime))
                        isTomorrow -> "Ngày mai, " + SimpleDateFormat("HH:mm", Locale("vi", "VN")).format(Date(upcoming.appointmentTime))
                        else -> SimpleDateFormat("EEE, dd/MM", Locale("vi", "VN")).format(Date(upcoming.appointmentTime))
                    }
                    val docOrDept = if (upcoming.doctorName.isNotBlank()) "Bác sĩ ${upcoming.doctorName}" else upcoming.department
                    val item = AppointmentItem(
                        id = upcoming.id,
                        title = if (upcoming.hospitalName.isNotBlank()) upcoming.hospitalName else "Khám định kỳ",
                        timeFormatted = dateFmt,
                        doctorOrDepartment = docOrDept,
                        isToday = isToday
                    )

                    if (isToday) {
                        todayApptItem = item
                    } else {
                        nextUpcomingApptItem = item
                    }

                    // Tự động kiểm tra và tạo thông báo chuông (AlertDao) nhắc nhở lịch khám nếu chưa có
                    val now = System.currentTimeMillis()
                    val diffHours = (upcoming.appointmentTime - now) / (1000 * 3600)
                    if (diffHours in -1..168) { // Trong vòng 7 ngày tới
                        val currentMember = userDao.getUserById(memberId)
                        val memberName = currentMember?.fullName ?: "Thành viên"
                        val alertTitle = "🗓 Sắp đến lịch khám: ${item.title}"
                        val alertMsg = "Lịch khám tại ${item.title} ($dateFmt) của $memberName. Bác sĩ/Khoa: $docOrDept."
                        val existingAlerts = alertDao.getAllAlerts().firstOrNull() ?: emptyList()
                        val alreadyAlerted = existingAlerts.any {
                            it.memberId == memberId && it.message.contains(item.title)
                        }
                        if (!alreadyAlerted) {
                            alertDao.insertAlert(
                                AlertEntity(
                                    familyId = currentMember?.familyId ?: "FAM-8606",
                                    memberId = memberId,
                                    memberName = memberName,
                                    alertType = AlertType.APPOINTMENT_REMINDER,
                                    alertLevel = AlertLevel.WARNING,
                                    title = alertTitle,
                                    message = alertMsg,
                                    timestamp = now,
                                    isRead = false,
                                    isResolved = false
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        _uiState.update {
            it.copy(
                selectedMemberId = memberId,
                alertStatus = alert,
                latestVitals = vitals,
                todayMedications = initialSchedules,
                todayAppointment = todayApptItem,
                upcomingAppointment = nextUpcomingApptItem,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private fun observeMedicationFlow(memberId: String, todayStr: String) {
        activeMedicationJob?.cancel()
        activeMedicationJob = viewModelScope.launch(Dispatchers.IO) {
            combine(
                medicineDao.getMedicinesByMember(memberId),
                medicationLogDao.getLogsForDate(memberId, todayStr)
            ) { medicines, logs ->
                val logMap = logs.associateBy { "${it.medicineId}_${it.scheduledTime}" }
                val schedules = mutableListOf<MedicationSchedule>()
                for (med in medicines) {
                    for (time in med.reminderTimes) {
                        val log = logMap["${med.id}_$time"]
                        schedules.add(
                            MedicationSchedule(
                                id = log?.id ?: "${med.id}_${todayStr}_$time",
                                medicineId = med.id,
                                medicineName = med.name,
                                dosage = med.dosage,
                                instruction = med.instruction,
                                scheduledTime = time,
                                isTaken = log?.isTaken ?: false,
                                isSkipped = log?.isSkipped ?: false,
                                actualTakenTime = log?.actualTakenTime
                            )
                        )
                    }
                }
                schedules.sortBy { it.scheduledTime }
                schedules
            }.collect { schedules ->
                _uiState.update { it.copy(todayMedications = schedules) }
            }
        }
    }

    fun onMemberSelect(memberId: String) {
        if (_uiState.value.selectedMemberId == memberId) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMemberId = memberId, isLoading = true) }
            fetchMemberDetails(memberId)
        }
    }

    /**
     * Thêm thành viên mới trực tiếp vào gia đình từ Dashboard
     */
    fun addNewMember(fullName: String, relationship: String) {
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) return

        val newId = "member_${UUID.randomUUID().toString().take(8)}"
        val newMemberEntity = UserEntity(
            id = newId,
            fullName = trimmedName,
            email = "",
            role = relationship.ifBlank { "Thành viên" },
            familyId = sessionManager.familyId,
            bloodType = "O+"
        )

        viewModelScope.launch(Dispatchers.IO) {
            userDao.insertUser(newMemberEntity)
            val updatedMembers = loadRealFamilyMembers()
            _uiState.update { state ->
                state.copy(
                    familyMembers = updatedMembers,
                    selectedMemberId = newId
                )
            }
            fetchMemberDetails(newId)
        }
    }

    fun onTakeMedicine(medicineId: String, scheduledTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = getTodayDateString()
                val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
                val now = System.currentTimeMillis()
                val log = medicationLogDao.getLog(medicineId, todayStr, scheduledTime)
                if (log == null) {
                    val med = medicineDao.getMedicineById(medicineId)
                    val newLog = MedicationLogEntity(
                        id = UUID.randomUUID().toString(),
                        medicineId = medicineId,
                        medicineName = med?.name ?: "Thuốc",
                        userId = memberId,
                        scheduledDate = todayStr,
                        scheduledTime = scheduledTime,
                        actualTakenTime = now,
                        isTaken = true,
                        isSkipped = false
                    )
                    medicationLogDao.insertIfAbsent(newLog)
                    medicationLogDao.markAsTaken(newLog.id, now)
                } else {
                    medicationLogDao.markAsTaken(log.id, now)
                }

                _uiState.update { state ->
                    state.copy(
                        todayMedications = state.todayMedications.map {
                            if (it.medicineId == medicineId && it.scheduledTime == scheduledTime) {
                                it.copy(isTaken = true, isSkipped = false, actualTakenTime = now)
                            } else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi ghi nhận uống thuốc") }
            }
        }
    }

    fun onTakeMedicine(scheduleId: String) {
        val schedule = _uiState.value.todayMedications.firstOrNull { it.id == scheduleId }
        if (schedule != null) {
            onTakeMedicine(schedule.medicineId, schedule.scheduledTime)
        } else {
            viewModelScope.launch(Dispatchers.IO) {
                medicationLogDao.markAsTaken(scheduleId, System.currentTimeMillis())
            }
        }
    }

    /**
     * Xác nhận uống toàn bộ các thuốc trong cùng một cữ giờ (1 chạm)
     */
    fun onTakeSession(scheduledTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = getTodayDateString()
                val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
                val now = System.currentTimeMillis()

                val sessionDoses = _uiState.value.todayMedications.filter { it.scheduledTime == scheduledTime }
                for (dose in sessionDoses) {
                    var log = medicationLogDao.getLog(dose.medicineId, todayStr, scheduledTime)
                    if (log == null) {
                        val med = medicineDao.getMedicineById(dose.medicineId)
                        val newLog = MedicationLogEntity(
                            id = UUID.randomUUID().toString(),
                            medicineId = dose.medicineId,
                            medicineName = med?.name ?: dose.medicineName,
                            userId = memberId,
                            scheduledDate = todayStr,
                            scheduledTime = scheduledTime,
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

                _uiState.update { state ->
                    state.copy(
                        todayMedications = state.todayMedications.map {
                            if (it.scheduledTime == scheduledTime) {
                                it.copy(isTaken = true, isSkipped = false, actualTakenTime = now)
                            } else it
                        }
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi khi ghi nhận cả cữ thuốc") }
            }
        }
    }

    /**
     * Hoàn tác toàn bộ các thuốc trong cùng một cữ giờ
     */
    fun onUndoTakeSession(scheduledTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = getTodayDateString()
                val sessionDoses = _uiState.value.todayMedications.filter { it.scheduledTime == scheduledTime }
                for (dose in sessionDoses) {
                    val log = medicationLogDao.getLog(dose.medicineId, todayStr, scheduledTime)
                    if (log != null) {
                        medicationLogDao.markAsNotTaken(log.id)
                    }
                }

                _uiState.update { state ->
                    state.copy(
                        todayMedications = state.todayMedications.map {
                            if (it.scheduledTime == scheduledTime) {
                                it.copy(isTaken = false, actualTakenTime = null)
                            } else it
                        }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun onUndoTakeMedicine(medicineId: String, scheduledTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = getTodayDateString()
                val log = medicationLogDao.getLog(medicineId, todayStr, scheduledTime)
                if (log != null) {
                    medicationLogDao.markAsNotTaken(log.id)
                }

                _uiState.update { state ->
                    state.copy(
                        todayMedications = state.todayMedications.map {
                            if (it.medicineId == medicineId && it.scheduledTime == scheduledTime) {
                                it.copy(isTaken = false, actualTakenTime = null)
                            } else it
                        }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun onSkipMedicine(medicineId: String, scheduledTime: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = getTodayDateString()
                val memberId = _uiState.value.selectedMemberId.ifBlank { sessionManager.userId }
                val log = medicationLogDao.getLog(medicineId, todayStr, scheduledTime)
                if (log == null) {
                    val med = medicineDao.getMedicineById(medicineId)
                    val newLog = MedicationLogEntity(
                        id = UUID.randomUUID().toString(),
                        medicineId = medicineId,
                        medicineName = med?.name ?: "Thuốc",
                        userId = memberId,
                        scheduledDate = todayStr,
                        scheduledTime = scheduledTime,
                        actualTakenTime = null,
                        isTaken = false,
                        isSkipped = true
                    )
                    medicationLogDao.insertIfAbsent(newLog)
                    medicationLogDao.markAsSkipped(newLog.id)
                } else {
                    medicationLogDao.markAsSkipped(log.id)
                }

                _uiState.update { state ->
                    state.copy(
                        todayMedications = state.todayMedications.map {
                            if (it.medicineId == medicineId && it.scheduledTime == scheduledTime) {
                                it.copy(isSkipped = true, isTaken = false)
                            } else it
                        }
                    )
                }
            } catch (_: Exception) {}
        }
    }

    fun onDismissAlert() {
        _uiState.update { it.copy(alertStatus = null) }
    }

    fun refresh() {
        val currentId = _uiState.value.selectedMemberId
        if (currentId.isNotBlank()) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true) }
                fetchMemberDetails(currentId)
            }
        } else {
            loadInitialData()
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appContext = context.applicationContext
                val db = AppDatabase.getInstance(appContext)
                val ble = SmartwatchBleManager(appContext)
                val sessionManager = SessionManager(appContext)
                val alertDao = db.alertDao()
                val analyzeUseCase = AnalyzeThresholdUseCase(alertDao)

                return HomeViewModel(
                    sessionManager = sessionManager,
                    userDao = db.userDao(),
                    healthMetricDao = db.healthMetricDao(),
                    alertDao = alertDao,
                    medicineDao = db.medicineDao(),
                    medicationLogDao = db.medicationLogDao(),
                    analyzeThresholdUseCase = analyzeUseCase,
                    bleManager = ble,
                    appointmentDao = db.appointmentDao()
                ) as T
            }
        }
    }
}
