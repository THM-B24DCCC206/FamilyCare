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
    private val bleManager: SmartwatchBleManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState(isLoading = true))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeBleDevice()
        loadInitialData()
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

                    // Thành viên được chọn mặc định luôn là người dùng đang đăng nhập
                    val currentUserId = sessionManager.userId.ifBlank { members.firstOrNull()?.id ?: "" }
                    val selectedId = if (_uiState.value.selectedMemberId.isNotBlank() &&
                        members.any { it.id == _uiState.value.selectedMemberId }
                    ) {
                        _uiState.value.selectedMemberId
                    } else {
                        currentUserId
                    }

                    _uiState.update {
                        it.copy(
                            familyMembers = members,
                            selectedMemberId = selectedId
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
        runCatching {
            userDao.deleteUserById("user_01")
            userDao.deleteUserById("user_demo_01")
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

        // 2. Thêm các thành viên gia đình khác (nếu có trong DB và không phải user_01 / user_demo_01)
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

    /**
     * Tải dữ liệu thực tế của thành viên:
     * - Nếu chưa đo lần nào: Để trống (0) và KHÔNG tự bịa cảnh báo đỏ!
     * - Nếu chưa có thuốc: Để danh sách rỗng!
     */
    private suspend fun fetchMemberDetails(memberId: String) = withContext(Dispatchers.IO) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

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
            isHeartRateWarning = isBpWarning,
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
        val realSchedules = mutableListOf<MedicationSchedule>()
        try {
            // Lấy danh sách thuốc của thành viên
            val medicinesFlow = medicineDao.getMedicinesByMember(memberId)
            val medicines = medicinesFlow.firstOrNull() ?: emptyList()

            for (medicine in medicines) {
                for (time in medicine.reminderTimes) {
                    val log = medicationLogDao.getLog(medicine.id, todayStr, time)
                    val isTaken = log?.isTaken ?: false
                    realSchedules.add(
                        MedicationSchedule(
                            id = log?.id ?: "${medicine.id}_${todayStr}_$time",
                            medicineId = medicine.id,
                            medicineName = medicine.name,
                            dosage = medicine.dosage,
                            instruction = medicine.instruction,
                            scheduledTime = time,
                            isTaken = isTaken
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // Giữ danh sách rỗng nếu chưa có dữ liệu thuốc
        }

        _uiState.update {
            it.copy(
                selectedMemberId = memberId,
                alertStatus = alert,
                latestVitals = vitals,
                todayMedications = realSchedules,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun onMemberSelect(memberId: String) {
        if (_uiState.value.selectedMemberId == memberId) return
        viewModelScope.launch {
            _uiState.update { it.copy(selectedMemberId = memberId, isLoading = true) }
            fetchMemberDetails(memberId)
        }
    }

    fun onTakeMedicine(scheduleId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            medicationLogDao.markAsTaken(scheduleId, System.currentTimeMillis())
            _uiState.update { state ->
                state.copy(
                    todayMedications = state.todayMedications.map {
                        if (it.id == scheduleId) it.copy(isTaken = true) else it
                    }
                )
            }
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
                    bleManager = ble
                ) as T
            }
        }
    }
}
