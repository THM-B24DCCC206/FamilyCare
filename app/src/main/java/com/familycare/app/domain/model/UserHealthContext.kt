package com.familycare.app.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Tóm tắt chi tiết một loại thuốc đang sử dụng hôm nay
 */
data class TodayMedicineSummary(
    val medicineId: String,
    val name: String,
    val dosage: String,
    val unit: String = "viên",
    val purpose: String = "",
    val reminderTimes: List<String> = emptyList(),
    val scheduleStatus: String = "" // Ví dụ: "Đã uống cữ 08:00, Chưa uống cữ 20:00"
)

/**
 * Tóm tắt y tế ngắn gọn của thành viên khác trong cùng tổ ấm gia đình
 */
data class OtherMemberHealthSummary(
    val userId: String,
    val fullName: String,
    val age: Int,
    val role: String = "MEMBER",
    val bloodPressureSummary: String = "Chưa có dữ liệu",
    val heartRateSummary: String = "Chưa có dữ liệu",
    val chronicDiseases: List<String> = emptyList(),
    val medicinesSummary: String = "Chưa có đơn thuốc"
)

/**
 * Ngữ cảnh y tế cá nhân hóa của người dùng (Context Injection)
 * Được trích xuất tự động 100% từ Room Database để tiêm vào System Instructions của Gemini.
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
    val latestBloodSugar: HealthMetric? = null,       // Đường huyết mới nhất (nếu có)
    val todayMedicines: List<TodayMedicineSummary> = emptyList(), // Lịch uống thuốc hôm nay
    val otherFamilyMembers: List<OtherMemberHealthSummary> = emptyList() // Thành viên khác trong tổ ấm
) {
    /**
     * Tính tuổi chính xác từ ngày sinh nếu có
     */
    fun getDisplayAge(): Int {
        if (calculatedAge != null && calculatedAge > 0) return calculatedAge
        if (birthDate.isBlank()) return 65

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
     * Tóm tắt chỉ số huyết áp dạng text dễ đọc: ví dụ "145/92 mmHg (Tăng huyết áp) - Đo lúc: 08:30 - 27/10/2026"
     */
    fun getBloodPressureSummary(): String {
        val bp = latestBloodPressure ?: return "Chưa có dữ liệu đo gần đây"
        val sys = bp.valuePrimary.toInt()
        val dia = bp.valueSecondary?.toInt() ?: 0
        val timeStr = formatTimestamp(bp.timestamp)

        val classification = when {
            sys >= 180 || dia >= 120 -> "NGUY HIỂM: Cơn tăng huyết áp cấp cứu"
            sys >= 140 || dia >= 90 -> "Tăng huyết áp (Vượt ngưỡng an toàn)"
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
            bpm > 120 -> "Nhịp tim rất nhanh"
            bpm in 100..120 -> "Nhịp tim hơi nhanh"
            bpm in 60..100 -> "Nhịp tim bình thường"
            bpm in 50..59 -> "Nhịp tim chậm nhẹ"
            bpm < 50 -> "Nhịp tim quá chậm"
            else -> "Bình thường"
        }

        return "$bpm BPM ($classification) - Đo lúc: $timeStr"
    }

    /**
     * Tạo chuỗi System Instructions hoàn chỉnh được tiêm ngữ cảnh (Context Injection)
     * tuân theo cấu trúc bối cảnh y tế gia đình FamilyCare chuẩn xác.
     */
    fun buildSystemInstruction(): String {
        val age = getDisplayAge()
        val diseases = if (chronicDiseases.isNotEmpty()) chronicDiseases.joinToString(", ") else "Không có"
        val allergies = if (drugAllergies.isNotEmpty()) drugAllergies.joinToString(", ") else "Không có"
        val bpInfo = getBloodPressureSummary()
        val hrInfo = getHeartRateSummary()

        val medicinesText = if (todayMedicines.isEmpty()) {
            "Không có lịch uống thuốc được ghi nhận hôm nay"
        } else {
            todayMedicines.joinToString("\n     + ") { med ->
                val purposeText = if (med.purpose.isNotBlank()) ", Công dụng: ${med.purpose}" else ""
                val timesText = if (med.reminderTimes.isNotEmpty()) ", Cữ uống: ${med.reminderTimes.joinToString(", ")}" else ""
                val statusText = if (med.scheduleStatus.isNotBlank()) " [${med.scheduleStatus}]" else ""
                "${med.name} (Liều: ${med.dosage})$purposeText$timesText$statusText"
            }
        }

        val otherMembersText = if (otherFamilyMembers.isEmpty()) {
            "   - Không có thành viên nào khác được ghi nhận trong tổ ấm."
        } else {
            otherFamilyMembers.joinToString("\n") { m ->
                val diseasesStr = if (m.chronicDiseases.isEmpty()) "Không có" else m.chronicDiseases.joinToString(", ")
                "   - ${m.fullName} (${m.age} tuổi): Huyết áp ${m.bloodPressureSummary}, Tim ${m.heartRateSummary}, Bệnh nền: $diseasesStr, Thuốc: ${m.medicinesSummary}"
            }
        }

        return """
=== BỐI CẢNH Y TẾ GIA ĐÌNH FAMILYCARE (DỮ LIỆU THẬT TỪ THIẾT BỊ) ===
1. THÀNH VIÊN ĐANG ĐƯỢC CHỌN TRỌNG TÂM:
   - Họ tên: $fullName ($age tuổi)
   - Tiền sử bệnh nền: $diseases
   - Dị ứng: $allergies
   - Huyết áp đo gần nhất: $bpInfo
   - Nhịp tim đo gần nhất: $hrInfo
   - Thuốc đang dùng hôm nay:
     + $medicinesText

2. TÓM TẮT CÁC THÀNH VIÊN KHÁC TRONG TỔ ẤM:
$otherMembersText

3. QUY TẮC PHẢN HỒI LÂM SÀNG (CLINICAL & ETHICAL GUIDELINES):
   - Ưu tiên số 1: Trả lời trọng tâm theo dữ liệu của THÀNH VIÊN ĐANG ĐƯỢC CHỌN TRỌNG TÂM ($fullName).
   - Nhận diện thành viên khác: Nếu người dùng chủ động hỏi về người khác bằng tên hoặc danh xưng (ví dụ: "vợ tôi", "cháu Ly", "Bố Nam"), hãy đối chiếu ngay sang mục tóm tắt của đúng người đó để trả lời chính xác số liệu thật.
   - Đảm bảo an toàn: Không bịa số liệu, cảnh báo tương tác thuốc/bệnh nền, xưng hô lễ phép "cháu" - "bác", gắn cảnh báo SOS nếu có dấu hiệu cấp cứu (huyết áp ≥ 180, đau ngực dữ dội).
====================================================================
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
