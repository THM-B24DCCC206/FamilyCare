package com.familycare.app.presentation.dashboard

import com.familycare.app.domain.model.Alert
import com.familycare.app.domain.model.HealthMetric
import com.familycare.app.domain.model.Medicine
import com.familycare.app.domain.model.User

/**
 * Các trạng thái giao diện Dashboard theo chuẩn MVI/MVVM:
 * - Loading: Đang tải dữ liệu hồ sơ, chỉ số Room DB
 * - Success: Tải thành công dữ liệu
 * - Error: Lỗi kèm thông điệp thân thiện
 */
sealed interface DashboardUiStatus {
    object Loading : DashboardUiStatus
    object Success : DashboardUiStatus
    data class Error(val errorMessage: String) : DashboardUiStatus
}

data class DashboardUiState(
    val status: DashboardUiStatus = DashboardUiStatus.Loading,
    val familyMembers: List<User> = emptyList(),
    val selectedMember: User? = null,
    val activeRedAlert: Alert? = null,                  // Cảnh báo đỏ vượt ngưỡng hoặc SOS
    val consecutiveBreachCount: Int = 0,                // Số lần đo vượt ngưỡng liên tiếp
    val latestBloodPressure: HealthMetric? = null,       // Huyết áp mới nhất
    val latestHeartRate: HealthMetric? = null,           // Nhịp tim mới nhất
    val upcomingMedicine: Medicine? = null,              // Cữ thuốc tiếp theo
    val isMedicineTakenToday: Boolean = false,          // Đã bấm xác nhận uống thuốc chưa
    val complianceRatePercent: Int = 92                 // Tỷ lệ tuân thủ điều trị (%)
)
