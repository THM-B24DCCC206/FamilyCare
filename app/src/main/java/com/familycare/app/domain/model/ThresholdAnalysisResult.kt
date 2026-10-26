package com.familycare.app.domain.model

/**
 * Kết quả phân tích chuỗi đo chỉ số y tế trả về cho Dashboard
 */
data class ThresholdAnalysisResult(
    val userId: String,
    val userName: String = "",
    val isCriticalRedAlert: Boolean = false,       // Trạng thái cảnh báo đỏ (>= 3 lần liên tiếp vượt ngưỡng trong 48h)
    val consecutiveBreachCount: Int = 0,          // Số lần đo liên tiếp vượt ngưỡng tính từ lần đo gần nhất
    val totalBreachesInWindow: Int = 0,           // Tổng số lần vượt ngưỡng trong 48 giờ
    val breachedMetrics: List<HealthMetric> = emptyList(), // Danh sách các lần đo vượt ngưỡng
    val latestMetric: HealthMetric? = null,        // Lần đo gần nhất
    val generatedAlert: Alert? = null,             // Bản ghi cảnh báo được sinh ra (nếu chưa bị debounce)
    val isDebounced: Boolean = false,              // Đã có cảnh báo trước đó trong khoảng thời gian debounce (tránh spam)
    val summaryMessage: String = "",
    val recommendations: List<String> = emptyList()
)
