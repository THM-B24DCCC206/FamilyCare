package com.familycare.app.presentation.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.dao.AlertDao
import com.familycare.app.core.database.dao.HealthMetricDao
import com.familycare.app.core.database.dao.UserDao
import com.familycare.app.core.database.entity.HealthMetricEntity
import com.familycare.app.core.database.entity.UserEntity
import com.familycare.app.domain.model.*
import com.familycare.app.domain.usecase.AnalyzeThresholdUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel cho Trang chủ tổng quan (Dashboard), quản lý StateFlow
 * kết nối Room Database và thuật toán cảnh báo đỏ AnalyzeThresholdUseCase.
 */
class DashboardViewModel(
    private val userDao: UserDao,
    private val healthMetricDao: HealthMetricDao,
    private val alertDao: AlertDao,
    private val analyzeThresholdUseCase: AnalyzeThresholdUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    /**
     * Tải toàn bộ dữ liệu trang chủ từ Room Database & Phân tích cảnh báo ngưỡng
     */
    fun loadDashboardData() {
        _uiState.update { it.copy(status = DashboardUiStatus.Loading) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Tải danh sách thành viên gia đình (Tạo mẫu nếu chưa có)
                    val members = seedFamilyMembersIfEmpty()
                    val currentMember = _uiState.value.selectedMember ?: members.first()

                    // 2. Tải chỉ số huyết áp & nhịp tim mới nhất của thành viên đang chọn
                    val bpEntity = healthMetricDao.getLatestMetricByType(currentMember.id, MetricType.BLOOD_PRESSURE.name)
                        ?: seedSampleBp(currentMember.id)
                    val hrEntity = healthMetricDao.getLatestMetricByType(currentMember.id, MetricType.HEART_RATE.name)
                        ?: seedSampleHr(currentMember.id)

                    val bpDomain = bpEntity.toDomainModel()
                    val hrDomain = hrEntity.toDomainModel()

                    // 3. Lấy toàn bộ các lần đo trong 48h để chạy thuật toán AnalyzeThresholdUseCase
                    val sampleHistory = listOf(
                        bpDomain,
                        HealthMetric(
                            id = "bp_hist_1",
                            userId = currentMember.id,
                            type = MetricType.BLOOD_PRESSURE,
                            valuePrimary = 148.0,
                            valueSecondary = 95.0,
                            unit = "mmHg",
                            timestamp = System.currentTimeMillis() - 4 * 3600 * 1000L
                        ),
                        HealthMetric(
                            id = "bp_hist_2",
                            userId = currentMember.id,
                            type = MetricType.BLOOD_PRESSURE,
                            valuePrimary = 152.0,
                            valueSecondary = 96.0,
                            unit = "mmHg",
                            timestamp = System.currentTimeMillis() - 9 * 3600 * 1000L
                        )
                    )

                    // 4. Chạy thuật toán phát hiện chuỗi vượt ngưỡng 3 lần liên tiếp
                    var activeAlert: Alert? = null
                    var breachCount = 0

                    analyzeThresholdUseCase(
                        userId = currentMember.id,
                        metrics = sampleHistory,
                        userName = currentMember.fullName,
                        familyId = currentMember.familyId
                    ).collect { resource ->
                        if (resource is Resource.Success && resource.data != null) {
                            val analysis = resource.data
                            breachCount = analysis.consecutiveBreachCount
                            if (analysis.isCriticalRedAlert) {
                                activeAlert = analysis.generatedAlert ?: Alert(
                                    id = "alert_critical_sample",
                                    familyId = currentMember.familyId,
                                    targetUserId = currentMember.id,
                                    targetUserName = currentMember.fullName,
                                    type = AlertType.THRESHOLD_EXCEEDED,
                                    level = AlertLevel.CRITICAL,
                                    title = "⚠️ CẢNH BÁO ĐỎ: 3 LẦN ĐO VƯỢT NGƯỠNG LIÊN TIẾP",
                                    message = "Huyết áp trung bình ${bpDomain.valuePrimary.toInt()}/${bpDomain.valueSecondary?.toInt()} mmHg vượt ngưỡng an toàn trong 48 giờ qua.",
                                    timestamp = System.currentTimeMillis()
                                )
                            }
                        }
                    }

                    // 5. Cữ thuốc tiếp theo
                    val upcomingMed = Medicine(
                        id = "med_01",
                        userId = currentMember.id,
                        name = "Amlodipine 5mg (Thuốc hạ huyết áp)",
                        dosage = "1 viên",
                        instruction = "Uống sau bữa ăn, kèm nhiều nước ấm",
                        reminderTimes = listOf("08:00", "18:00")
                    )

                    _uiState.update { state ->
                        state.copy(
                            status = DashboardUiStatus.Success,
                            familyMembers = members,
                            selectedMember = currentMember,
                            activeRedAlert = activeAlert,
                            consecutiveBreachCount = breachCount,
                            latestBloodPressure = bpDomain,
                            latestHeartRate = hrDomain,
                            upcomingMedicine = upcomingMed,
                            complianceRatePercent = 94
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = DashboardUiStatus.Error(
                            "Không thể tải dữ liệu trang chủ: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                        )
                    )
                }
            }
        }
    }

    /**
     * Chuyển đổi góc nhìn thành viên trong gia đình (Member Switcher)
     */
    fun onSelectMember(member: User) {
        if (_uiState.value.selectedMember?.id == member.id) return
        _uiState.update { it.copy(selectedMember = member, isMedicineTakenToday = false) }
        loadDashboardData()
    }

    /**
     * Đánh dấu xác nhận đã uống cữ thuốc hiện tại
     */
    fun markMedicineTaken() {
        _uiState.update { it.copy(isMedicineTakenToday = true) }
    }

    /**
     * Thử lại sau khi gặp lỗi
     */
    fun retry() {
        loadDashboardData()
    }

    // --- DỮ LIỆU MẪU BAN ĐẦU (SEED DATA) ---
    private suspend fun seedFamilyMembersIfEmpty(): List<User> {
        val existing = userDao.getCurrentUser()
        if (existing != null) {
            return listOf(
                existing.toDomainModel(),
                User(id = "user_02", fullName = "Bác Trần Thị Lan", role = UserRole.MEMBER, birthDate = "10/08/1960"),
                User(id = "user_03", fullName = "Con Nguyễn Hoàng", role = UserRole.ADMIN, birthDate = "22/11/1988")
            )
        }

        val primaryUser = UserEntity(
            id = "user_demo_01",
            fullName = "Bác Nguyễn Văn Nam",
            email = "nam.nguyen@familycare.vn",
            familyId = "fam_123456",
            role = "MEMBER",
            birthDate = "15/04/1958",
            chronicDiseasesJson = "[\"Cao huyết áp vô căn\"]"
        )
        userDao.insertUser(primaryUser)

        return listOf(
            primaryUser.toDomainModel(),
            User(id = "user_02", fullName = "Bác Trần Thị Lan", role = UserRole.MEMBER, birthDate = "10/08/1960"),
            User(id = "user_03", fullName = "Con Nguyễn Hoàng", role = UserRole.ADMIN, birthDate = "22/11/1988")
        )
    }

    private suspend fun seedSampleBp(userId: String): HealthMetricEntity {
        val bp = HealthMetricEntity(
            id = "bp_latest",
            userId = userId,
            type = MetricType.BLOOD_PRESSURE.name,
            valuePrimary = 145.0,
            valueSecondary = 92.0,
            unit = "mmHg",
            timestamp = System.currentTimeMillis() - 30 * 60 * 1000L
        )
        healthMetricDao.insertMetric(bp)
        return bp
    }

    private suspend fun seedSampleHr(userId: String): HealthMetricEntity {
        val hr = HealthMetricEntity(
            id = "hr_latest",
            userId = userId,
            type = MetricType.HEART_RATE.name,
            valuePrimary = 86.0,
            unit = "BPM",
            timestamp = System.currentTimeMillis() - 30 * 60 * 1000L
        )
        healthMetricDao.insertMetric(hr)
        return hr
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val analyzeUseCase = AnalyzeThresholdUseCase(db.alertDao())
                return DashboardViewModel(
                    userDao = db.userDao(),
                    healthMetricDao = db.healthMetricDao(),
                    alertDao = db.alertDao(),
                    analyzeThresholdUseCase = analyzeUseCase
                ) as T
            }
        }
    }
}
