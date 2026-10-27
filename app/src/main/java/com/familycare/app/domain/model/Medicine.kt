package com.familycare.app.domain.model

data class Medicine(
    val id: String = "",
    val userId: String = "",
    val name: String,                    // Tên thuốc
    val dosage: String,                  // Liều lượng: ví dụ "1 gói", "1 viên", "500mg"
    val instruction: String = "",        // "Uống sau khi ăn 30 phút"
    val reminderTimes: List<String>,     // ["08:00", "20:00"]
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long? = null,
    val scannedViaOcr: Boolean = false,  // Đánh dấu được quét từ ML Kit OCR
    val purpose: String = ""             // Công dụng ngắn gọn: "Mát gan, thanh nhiệt", "Hạ huyết áp"...
)

data class MedicationLog(
    val id: String = "",
    val medicineId: String,
    val medicineName: String,
    val userId: String,
    val scheduledTime: String,
    val actualTakenTime: Long? = null,
    val isTaken: Boolean = false,
    val isSkipped: Boolean = false
)
