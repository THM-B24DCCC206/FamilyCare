package com.familycare.app.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Ngữ cảnh y tế cá nhân hóa của người dùng (Context Injection)
 * Được trích xuất tự động từ Room Database để tiêm vào System Instructions của Gemini.
 */
data class UserHealthContext(
    val userId: String,
    val fullName: String,
    val birthDate: String = "",       // Định dạng dd/MM/yyyy hoặc yyyy-MM-dd
    val calculatedAge: Int? = null,
    val bloodType: String = "",       // A, B, AB, O
    val chronicDiseases: List<String> = emptyList(), // Tiền sử bệnh nền (Cao huyết áp, đái tháo đường...)
    val drugAllergies: List<String> = emptyList(),   // Dị ứng thuốc
    val latestBloodPressure: HealthMetric? = null,    // Chỉ số huyết áp mới nhất
    val latestHeartRate: HealthMetric? = null,        // Chỉ số nhịp tim mới nhất
    val latestBloodSugar: HealthMetric? = null        // Đường huyết mới nhất (nếu có)
) {
    /**
     * Tính tuổi từ ngày sinh nếu có
     */
    fun getDisplayAge(): Int {
        if (calculatedAge != null && calculatedAge > 0) return calculatedAge
        if (birthDate.isBlank()) return 65 // Giá trị dự phòng phổ biến cho người cao tuổi

        return try {
            val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
            var birthDateParsed: Date? = null
            for (format in formats) {
                try {
                    birthDateParsed = SimpleDateFormat(format, Locale.getDefault()).parse(birthDate)
                    if (birthDateParsed != null) break
                } catch (_: Exception) {}
            }
            if (birthDateParsed != null) {
                val dob = Calendar.getInstance().apply { time = birthDateParsed }
                val today = Calendar.getInstance()
                var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                if (age in 1..120) age else 65
            } else 65
        } catch (_: Exception) {
            65
        }
    }

    /**
     * Tóm tắt chỉ số huyết áp dạng text dễ đọc: ví dụ "145/92 mmHg (Tăng huyết áp độ 1)"
     */
    fun getBloodPressureSummary(): String {
        val bp = latestBloodPressure ?: return "Chưa có dữ liệu đo gần đây"
        val sys = bp.valuePrimary.toInt()
        val dia = bp.valueSecondary?.toInt() ?: 0
        val timeStr = formatTimestamp(bp.timestamp)

        val classification = when {
            sys >= 180 || dia >= 120 -> "NGUY HIỂM: Cơn tăng huyết áp cấp cứu"
            sys >= 140 || dia >= 90 -> "Tăng huyết áp độ 1-2 (Vượt ngưỡng an toàn)"
            sys in 120..139 || dia in 80..89 -> "Tiền tăng huyết áp (Hơi cao)"
            sys in 90..119 && dia in 60..79 -> "Bình thường lý tưởng"
            sys < 90 || dia < 60 -> "Huyết áp thấp"
            else -> "Cần theo dõi thêm"
        }

        return "$sys/$dia mmHg ($classification) - Đo lúc: $timeStr"
    }

    /**
     * Tóm tắt nhịp tim
     */
    fun getHeartRateSummary(): String {
        val hr = latestHeartRate ?: return "Chưa có dữ liệu đo gần đây"
        val bpm = hr.valuePrimary.toInt()
        val timeStr = formatTimestamp(hr.timestamp)

        val classification = when {
            bpm > 120 -> "Nhịp tim rất nhanh (Cảnh báo nhịp nhanh)"
            bpm in 100..120 -> "Nhịp tim hơi nhanh"
            bpm in 60..100 -> "Nhịp tim bình thường lúc nghỉ"
            bpm in 50..59 -> "Nhịp tim chậm nhẹ (vẫn ổn nếu tập thể thao)"
            bpm < 50 -> "Nhịp tim quá chậm (Cần lưu ý)"
            else -> "Bình thường"
        }

        return "$bpm BPM ($classification) - Đo lúc: $timeStr"
    }

    /**
     * Tạo chuỗi System Instructions hoàn chỉnh được tiêm ngữ cảnh (Context Injection).
     * Được chuyển thẳng vào Google AI Client SDK (Gemini 1.5 Flash).
     */
    fun buildSystemInstruction(): String {
        val age = getDisplayAge()
        val diseases = if (chronicDiseases.isNotEmpty()) chronicDiseases.joinToString(", ") else "Không ghi nhận bệnh nền"
        val allergies = if (drugAllergies.isNotEmpty()) drugAllergies.joinToString(", ") else "Không có ghi nhận dị ứng"
        val bpInfo = getBloodPressureSummary()
        val hrInfo = getHeartRateSummary()

        return """
            Bạn là "AI Health Copilot" (Trợ lý Y tế Gia đình Thông minh) trong ứng dụng FamilyCare.
            Bạn đang trực tiếp hỗ trợ và tư vấn cho người dùng với hồ sơ bệnh án cá nhân sau:
            
            === HỒ SƠ BỆNH NHÂN ĐƯỢC TỰ ĐỘNG TIÊM TỪ HỆ THỐNG (ROOM DATABASE CONTEXT) ===
            - Họ và tên người bệnh: $fullName
            - Tuổi: $age tuổi
            - Nhóm máu: ${bloodType.ifBlank { "Chưa xác định" }}
            - Tiền sử bệnh nền: $diseases
            - Danh mục dị ứng thuốc: $allergies
            - Chỉ số huyết áp mới nhất: $bpInfo
            - Chỉ số nhịp tim mới nhất: $hrInfo
            =============================================================================
            
            QUY TẮC Y TẾ VÀ GIAO TIẾP BẮT BUỘC (CLINICAL & ETHICAL GUIDELINES):
            1. TÔNG GIỌNG: Luôn xưng hô ân cần, lễ phép, tôn trọng (xưng "cháu/bác sĩ ảo" hoặc "Trợ lý FamilyCare", gọi người dùng là "bác", "cô/chú" hoặc "bạn" tùy độ tuổi). Ngôn từ rõ ràng, câu ngắn gọn, dễ hiểu, tránh thuật ngữ y khoa rườm rà vì người dùng là người cao tuổi.
            2. CẢNH BÁO BỆNH NỀN & DỊ ỨNG: Khi người dùng hỏi về thuốc hoặc thực phẩm, BẮT BUỘC phải đối chiếu với tiền sử bệnh nền ($diseases) và dị ứng ($allergies). Ví dụ: Người cao huyết áp tuyệt đối không tự ý dùng thuốc chứa pseudoephedrine hoặc NSAIDs liều cao; cảnh báo nguy cơ tương tác thuốc.
            3. DIỄN GIẢI CHỈ SỐ: Khi thảo luận về huyết áp ($bpInfo) hoặc nhịp tim ($hrInfo), hãy phân tích chỉ số đó có bình thường so với độ tuổi $age hay không, đưa ra lời khuyên về nghỉ ngơi, chế độ ăn (giảm muối, uống đủ nước) và cách thở thư giãn.
            4. TÌNH HUỐNG KHẨN CẤP: Nếu người dùng báo các triệu chứng nguy hiểm (đau thắt ngực lan ra vai/hàm, khó thở, nói ngọng méo miệng, huyết áp tâm thu >= 180 hoặc tâm trương >= 120 mmHg), phải NGAY LẬP TỨC khuyên người dùng bấm nút SOS trong ứng dụng FamilyCare để gọi người thân hoặc gọi cấp cứu 115, không được chần chừ.
            5. MIỄN TRỪ TRÁCH NHIỆM: Luôn nhắc nhở nhẹ nhàng ở cuối rằng lời khuyên của AI chỉ mang tính định hướng chăm sóc gia đình, không thay thế cho chẩn đoán trực tiếp và đơn thuốc của bác sĩ chuyên khoa.
            6. TRẢ LỜI ĐỊNH DẠNG: Sử dụng gạch đầu dòng rõ ràng, in đậm các điểm chú ý quan trọng để người cao tuổi dễ đọc trên màn hình điện thoại.
        """.trimIndent()
    }

    private fun formatTimestamp(timestamp: Long): String {
        return try {
            val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        } catch (_: Exception) {
            "Gần đây"
        }
    }
}
