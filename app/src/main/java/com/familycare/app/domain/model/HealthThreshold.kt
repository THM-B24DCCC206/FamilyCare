package com.familycare.app.domain.model

/**
 * Cấu hình ngưỡng an toàn cá nhân hóa cho từng thành viên trong gia đình
 */
data class HealthThreshold(
    val userId: String = "",
    val systolicMax: Double = 140.0,    // Huyết áp tâm thu tối đa an toàn (mmHg)
    val systolicMin: Double = 90.0,     // Huyết áp tâm thu tối thiểu
    val diastolicMax: Double = 90.0,    // Huyết áp tâm trương tối đa an toàn (mmHg)
    val diastolicMin: Double = 60.0,    // Huyết áp tâm trương tối thiểu
    val heartRateMax: Double = 100.0,   // Nhịp tim tối đa khi nghỉ (BPM)
    val heartRateMin: Double = 50.0,    // Nhịp tim tối thiểu khi nghỉ (BPM)
    val bloodSugarMax: Double = 180.0   // Đường huyết sau ăn tối đa (mg/dL)
)
