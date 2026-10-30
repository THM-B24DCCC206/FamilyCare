package com.familycare.app.presentation.biometrics.ppg

import androidx.compose.ui.graphics.Color

/**
 * Phân loại mức độ nhịp tim theo chuẩn y tế lâm sàng (Hiệp hội Tim mạch Hoa Kỳ - AHA & Bộ Y Tế)
 */
data class HeartRateLevel(
    val title: String,            // Tên mức độ: "Bình thường", "Nhịp tim nhanh",...
    val rangeText: String,        // Dải chỉ số: "60 - 100 BPM"
    val subtitle: String,         // Mô tả lâm sàng
    val badgeLabel: String,       // Nhãn ngắn trên thẻ lịch sử: "Bình thường", "Hơi nhanh",...
    val color: Color,             // Màu chính (Text, icon, viền)
    val containerColor: Color,    // Màu nền chip/card
    val borderColor: Color        // Màu viền chip/card
)

object HeartRateClassifier {

    /**
     * Phân loại chi tiết chỉ số nhịp tim thành 5 mức độ chuẩn
     */
    fun classify(bpm: Int): HeartRateLevel {
        return when {
            bpm <= 0 -> HeartRateLevel(
                title = "Đang đo...",
                rangeText = "--",
                subtitle = "Chưa có chỉ số nhịp tim",
                badgeLabel = "--",
                color = Color(0xFF64748B),
                containerColor = Color(0xFFF1F5F9),
                borderColor = Color(0xFFCBD5E1)
            )
            bpm < 50 -> HeartRateLevel(
                title = "Nhịp tim rất chậm",
                rangeText = "< 50 BPM",
                subtitle = "Nhịp quá chậm (Nguy cơ nhịp chậm bệnh lý, cần chú ý theo dõi)",
                badgeLabel = "Rất chậm",
                color = Color(0xFFDC2626),
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5)
            )
            bpm in 50..59 -> HeartRateLevel(
                title = "Nhịp tim chậm",
                rangeText = "50 - 59 BPM",
                subtitle = "Thấp hơn mức thông thường (Bình thường đối với người tập thể thao)",
                badgeLabel = "Nhịp chậm",
                color = Color(0xFF0288D1),
                containerColor = Color(0xFFE0F2FE),
                borderColor = Color(0xFFBAE6FD)
            )
            bpm in 60..100 -> HeartRateLevel(
                title = "Nhịp tim bình thường",
                rangeText = "60 - 100 BPM",
                subtitle = "Nhịp tim nghỉ ngơi lý tưởng và an toàn",
                badgeLabel = "Bình thường",
                color = Color(0xFF15803D),
                containerColor = Color(0xFFF0FDF4),
                borderColor = Color(0xFFBBF7D0)
            )
            bpm in 101..120 -> HeartRateLevel(
                title = "Nhịp tim hơi nhanh",
                rangeText = "101 - 120 BPM",
                subtitle = "Tăng nhẹ (Thường gặp sau khi vận động nhẹ, căng thẳng hoặc sốt nhẹ)",
                badgeLabel = "Hơi nhanh",
                color = Color(0xFFEA580C),
                containerColor = Color(0xFFFFF7ED),
                borderColor = Color(0xFFFED7AA)
            )
            else -> HeartRateLevel(
                title = "Nhịp tim rất nhanh",
                rangeText = "> 120 BPM",
                subtitle = "Cảnh báo nhịp tim đập nhanh nguy cơ cao khi nghỉ ngơi (Cần nghỉ ngơi & kiểm tra)",
                badgeLabel = "Rất nhanh",
                color = Color(0xFFDC2626),
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5)
            )
        }
    }

    /**
     * Bảng danh sách tất cả các dải nhịp tim để hiển thị hướng dẫn tham khảo trực quan
     */
    val referenceLevels: List<HeartRateLevel> = listOf(
        classify(45),  // < 50 BPM
        classify(55),  // 50 - 59 BPM
        classify(75),  // 60 - 100 BPM
        classify(110), // 101 - 120 BPM
        classify(130)  // > 120 BPM
    )
}
