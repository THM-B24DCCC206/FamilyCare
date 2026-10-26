package com.familycare.app.presentation.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.ble.SmartwatchBleManager
import com.familycare.app.core.common.Resource
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
                    val dbMembers = getOrSeedMembers()
                    val defaultMemberId = _uiState.value.selectedMemberId.ifBlank {
                        dbMembers.firstOrNull()?.id ?: ""
                    }

                    _uiState.update {
                        it.copy(
                            familyMembers = dbMembers,
                            selectedMemberId = defaultMemberId
                        )
                    }

                    if (defaultMemberId.isNotBlank()) {
                        fetchMemberDetails(defaultMemberId)
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

    private suspend fun getOrSeedMembers(): List<Member> = withContext(Dispatchers.IO) {
        val existingUsers = userDao.getAllUsers()
        if (existingUsers.isNotEmpty()) {
            return@withContext existingUsers.map { u ->
                Member(
                    id = u.id,
                    fullName = u.fullName,
                    avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                    initials = u.fullName.trim().take(1).uppercase(),
                    role = if (u.role == "ADMIN") "Trưởng gia đình" else "Thành viên"
                )
            }
        }

        // Seed dữ liệu người dùng thực tế vào SQLite
        val primaryUser = UserEntity(
            id = "user_01",
            fullName = "Bác Nguyễn Văn Nam",
            email = "nam.nguyen@familycare.vn",
            familyId = "fam_123",
            role = "MEMBER",
            birthDate = "15/04/1958",
            chronicDiseasesJson = "[\"Cao huyết áp vô căn\"]"
        )
        val user2 = UserEntity(
            id = "user_02",
            fullName = "Bác Trần Thị Lan",
            email = "lan.tran@familycare.vn",
            familyId = "fam_123",
            role = "MEMBER",
            birthDate = "10/08/1960",
            chronicDiseasesJson = "[\"Tiền tiểu đường\"]"
        )
        val user3 = UserEntity(
            id = "user_03",
            fullName = "Nguyễn Hoàng",
            email = "hoang.nguyen@familycare.vn",
            familyId = "fam_123",
            role = "ADMIN",
            birthDate = "22/11/1988"
        )

        userDao.insertUser(primaryUser)
        userDao.insertUser(user2)
        userDao.insertUser(user3)

        listOf(primaryUser, user2, user3).map { u ->
            Member(
                id = u.id,
                fullName = u.fullName,
                avatarUrl = null,
                initials = u.fullName.trim().take(1).uppercase(),
                role = if (u.role == "ADMIN") "Trưởng gia đình" else "Thành viên"
            )
        }
    }

    private suspend fun fetchMemberDetails(memberId: String) = withContext(Dispatchers.IO) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // 1. Chỉ số sinh tồn mới nhất
        val bpEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.BLOOD_PRESSURE.name)
            ?: seedSampleMetric(memberId, MetricType.BLOOD_PRESSURE.name, 125.0, 82.0, "mmHg")
        val hrEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.HEART_RATE.name)
            ?: seedSampleMetric(memberId, MetricType.HEART_RATE.name, 72.0, null, "BPM")
        val sugarEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.BLOOD_SUGAR.name)
        val tempEntity = healthMetricDao.getLatestMetricByType(memberId, MetricType.TEMPERATURE.name)

        val systolic = bpEntity.valuePrimary.toInt()
        val diastolic = bpEntity.valueSecondary?.toInt() ?: 80
        val hr = hrEntity.valuePrimary.toInt()

        val isBpWarning = systolic >= 140 || diastolic >= 90
        val isHrWarning = hr > 100 || hr < 55

        val vitals = VitalSummary(
            heartRateBpm = hr,
            bloodPressureSystolic = systolic,
            bloodPressureDiastolic = diastolic,
            isHeartRateWarning = isHrWarning,
            isBloodPressureWarning = isBpWarning,
            bloodSugar = sugarEntity?.valuePrimary,
            temperature = tempEntity?.valuePrimary,
            lastUpdated = bpEntity.timestamp
        )

        // 2. Phân tích cảnh báo (Threshold Analysis)
        var alert: HealthAlert? = null
        val recentMetrics = listOf(
            bpEntity.toDomainModel(),
            hrEntity.toDomainModel()
        )

        val currentMember = _uiState.value.familyMembers.firstOrNull { it.id == memberId }
        val memberName = currentMember?.fullName ?: "Thành viên"

        analyzeThresholdUseCase(
            userId = memberId,
            metrics = recentMetrics,
            userName = memberName,
            familyId = "fam_123"
        ).collect { res ->
            if (res is Resource.Success && res.data != null) {
                val result = res.data
                if (result.isCriticalRedAlert) {
                    alert = HealthAlert(
                        id = "alert_${memberId}_threshold",
                        title = "⚠️ CẢNH BÁO: CHỈ SỐ HUYẾT ÁP VƯỢT NGƯỠNG AN TOÀN",
                        message = "Phát hiện $systolic/$diastolic mmHg. Cần uống thuốc hoặc liên hệ bác sĩ theo dõi.",
                        level = "CRITICAL",
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }

        // Nếu người này là Bác Nam và có huyết áp cao sẵn, kích hoạt mẫu cảnh báo nguy cơ
        if (alert == null && isBpWarning) {
            alert = HealthAlert(
                id = "alert_${memberId}_bp",
                title = "⚠️ CẢNH BÁO: HUYẾT ÁP TĂNG CAO BẤT THƯỜNG",
                message = "Chỉ số hiện tại $systolic/$diastolic mmHg vượt ngưỡng 140/90. Vui lòng nghỉ ngơi và uống thuốc hạ áp theo chỉ định.",
                level = "CRITICAL",
                timestamp = System.currentTimeMillis()
            )
        }

        // 3. Lịch uống thuốc hôm nay
        val todaySchedules = mutableListOf<MedicationSchedule>()
        val defaultMedList = listOf(
            MedicationSchedule(
                id = "log_${memberId}_0800",
                medicineId = "med_01",
                medicineName = "Amlodipine 5mg",
                dosage = "1 viên",
                instruction = "Uống sau ăn sáng kèm nhiều nước ấm",
                scheduledTime = "08:00",
                isTaken = true
            ),
            MedicationSchedule(
                id = "log_${memberId}_1800",
                medicineId = "med_02",
                medicineName = "Glucophage 500mg",
                dosage = "1 viên",
                instruction = "Uống cùng bữa ăn tối để kiểm soát đường huyết",
                scheduledTime = "18:00",
                isTaken = false
            )
        )

        // Đồng bộ với database medication_logs nếu có
        for (item in defaultMedList) {
            val existingLog = medicationLogDao.getLog(item.medicineId, todayStr, item.scheduledTime)
            if (existingLog != null) {
                todaySchedules.add(item.copy(id = existingLog.id, isTaken = existingLog.isTaken))
            } else {
                val logEntity = MedicationLogEntity(
                    id = item.id,
                    medicineId = item.medicineId,
                    medicineName = item.medicineName,
                    userId = memberId,
                    scheduledDate = todayStr,
                    scheduledTime = item.scheduledTime,
                    isTaken = item.isTaken
                )
                medicationLogDao.insertIfAbsent(logEntity)
                todaySchedules.add(item)
            }
        }

        _uiState.update {
            it.copy(
                selectedMemberId = memberId,
                alertStatus = alert,
                latestVitals = vitals,
                todayMedications = todaySchedules,
                isLoading = false,
                errorMessage = null
            )
        }
    }

    private suspend fun seedSampleMetric(
        userId: String,
        type: String,
        val1: Double,
        val2: Double?,
        unit: String
    ): HealthMetricEntity {
        val entity = HealthMetricEntity(
            id = "metric_${userId}_${type.lowercase()}",
            userId = userId,
            type = type,
            valuePrimary = val1,
            valueSecondary = val2,
            unit = unit,
            timestamp = System.currentTimeMillis() - 15 * 60 * 1000L,
            notes = "Đo định kỳ buổi sáng",
            isFromCameraPpg = false
        )
        healthMetricDao.insertMetric(entity)
        return entity
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
                val alertDao = db.alertDao()
                val analyzeUseCase = AnalyzeThresholdUseCase(alertDao)

                return HomeViewModel(
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
