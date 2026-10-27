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
                    // Dọn dẹp các tài khoản giả lập nếu có trong DB
                    runCatching {
                        userDao.deleteUserById("user_01")
                        userDao.deleteUserById("user_demo_01")
                        userDao.deleteUserById("member_bac_nam")
                    }

                    // 1. Tải danh sách thành viên gia đình thực tế
                    val members = seedFamilyMembersIfEmpty()
                    val currentMember = _uiState.value.selectedMember ?: members.first()

                    // 2. Tải chỉ số huyết áp & nhịp tim mới nhất của thành viên đang chọn
                    val bpEntity = healthMetricDao.getLatestMetricByType(currentMember.id, MetricType.BLOOD_PRESSURE.name)
                        ?: seedSampleBp(currentMember.id)
                    val hrEntity = healthMetricDao.getLatestMetricByType(currentMember.id, MetricType.HEART_RATE.name)
                        ?: seedSampleHr(currentMember.id)

                    val bpDomain = bpEntity.toDomainModel()
                    val hrDomain = hrEntity.toDomainModel()

                    // 3. Lấy các lần đo gần đây để phân tích ngưỡng
                    val recentMetrics = healthMetricDao.getMetricsByMember(currentMember.id)
                    val metricsList = mutableListOf<HealthMetric>()
                    metricsList.add(bpDomain)
                    metricsList.add(hrDomain)

                    // 4. Chạy thuật toán phát hiện vượt ngưỡng
                    var activeAlert: Alert? = null
                    var breachCount = 0

                    analyzeThresholdUseCase(
                        userId = currentMember.id,
                        metrics = metricsList,
                        userName = currentMember.fullName,
                        familyId = currentMember.familyId
                    ).collect { resource ->
                        if (resource is Resource.Success && resource.data != null) {
                            val analysis = resource.data
                            breachCount = analysis.consecutiveBreachCount
                            if (analysis.isCriticalRedAlert) {
                                activeAlert = analysis.generatedAlert
                            }
                        }
                    }

                    // 5. Cữ thuốc tiếp theo
                    val upcomingMed = Medicine(
                        id = "med_01",
                        userId = currentMember.id,
                        name = "Amlodipine 5mg",
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

    // --- DỮ LIỆU THÀNH VIÊN THỰC TẾ (LY TRẦN & HẢI MINH TRẦN) ---
    private suspend fun seedFamilyMembersIfEmpty(): List<User> {
        val allDbUsers = userDao.getAllUsers().filter {
            it.id != "user_01" && it.id != "user_demo_01" && it.id != "member_bac_nam" && !it.fullName.contains("Nguyễn Văn Nam")
        }
        if (allDbUsers.isNotEmpty()) {
            return allDbUsers.map { it.toDomainModel() }
        }

        val lyTran = UserEntity(
            id = "user_me",
            fullName = "Ly Trần",
            email = "lytran@familycare.vn",
            role = "ADMIN",
            birthDate = "20/11/1992",
            bloodType = "A+"
        )
        userDao.insertUser(lyTran)

        val haiMinh = UserEntity(
            id = "user_02",
            fullName = "Hải Minh Trần",
            email = "haiminh@familycare.vn",
            role = "MEMBER",
            birthDate = "28/11/2006",
            bloodType = "O+"
        )
        userDao.insertUser(haiMinh)

        return listOf(lyTran.toDomainModel(), haiMinh.toDomainModel())
    }

    private suspend fun seedSampleBp(userId: String): HealthMetricEntity {
        val bp = HealthMetricEntity(
            id = "bp_latest",
            userId = userId,
            type = MetricType.BLOOD_PRESSURE.name,
            valuePrimary = 120.0,
            valueSecondary = 80.0,
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
            valuePrimary = 75.0,
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
