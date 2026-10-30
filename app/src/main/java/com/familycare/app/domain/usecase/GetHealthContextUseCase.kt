package com.familycare.app.domain.usecase

import com.familycare.app.core.database.HealthMetricDao
import com.familycare.app.core.database.MedicationLogDao
import com.familycare.app.core.database.MedicineDao
import com.familycare.app.core.database.UserDao
import com.familycare.app.core.database.UserEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.domain.model.OtherMemberHealthSummary
import com.familycare.app.domain.model.TodayMedicineSummary
import com.familycare.app.domain.model.UserHealthContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * UseCase truy xuất 100% hồ sơ bệnh án, lịch uống thuốc và các chỉ số đo mới nhất từ Room Database
 * của thành viên được chọn và tóm tắt các thành viên khác trong gia đình để tiêm ngữ cảnh vào Gemini AI.
 */
class GetHealthContextUseCase(
    private val userDao: UserDao,
    private val healthMetricDao: HealthMetricDao,
    private val medicineDao: MedicineDao? = null,
    private val medicationLogDao: MedicationLogDao? = null
) {

    suspend operator fun invoke(targetUserId: String? = null): UserHealthContext = withContext(Dispatchers.IO) {
        // 1. Lấy thông tin người dùng được chọn từ Room Database
        val userEntity = if (!targetUserId.isNullOrBlank()) {
            userDao.getUserById(targetUserId)
        } else {
            userDao.getCurrentUser()
        } ?: userDao.getAllUsers().firstOrNull() ?: seedDefaultDemoUser()

        val userId = userEntity.id
        val userDomain = userEntity.toDomainModel()
        val calculatedAge = calculateAgeFromBirthDate(userDomain.birthDate)

        // 2. Lấy chỉ số sinh tồn mới nhất từ Room
        val bpEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.BLOOD_PRESSURE.name)
        val hrEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.HEART_RATE.name)
        val bsEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.BLOOD_SUGAR.name)

        // 3. Lấy lịch uống thuốc hôm nay & trạng thái đã uống / chưa uống
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
        val rawMedicines = runCatching {
            medicineDao?.getMedicinesByMember(userId)?.firstOrNull().orEmpty()
        }.getOrDefault(emptyList())

        val todayLogs = runCatching {
            medicationLogDao?.getLogsForDate(userId, todayDate)?.firstOrNull().orEmpty()
        }.getOrDefault(emptyList())

        val todayMedicines = rawMedicines.map { med ->
            val normalizedDosage = normalizeDosage(med.name, med.dosage)
            val isLiquid = isLiquidOrPowder(med.name, med.dosage, med.instruction)
            val unit = if (isLiquid) "gói" else "viên"

            val medLogs = todayLogs.filter { it.medicineId == med.id }
            val reminderTimes = med.reminderTimes

            val scheduleStatus = if (reminderTimes.isEmpty()) {
                "Chưa thiết lập giờ uống"
            } else {
                val takenTimes = reminderTimes.filter { time ->
                    medLogs.any { it.scheduledTime == time && it.isTaken }
                }
                val pendingTimes = reminderTimes.filter { time ->
                    !takenTimes.contains(time)
                }

                when {
                    takenTimes.size == reminderTimes.size -> "Đã uống đủ các cữ (${takenTimes.joinToString(", ")})"
                    takenTimes.isEmpty() -> "Chưa uống cữ nào (Cần uống lúc: ${pendingTimes.joinToString(", ")})"
                    else -> "Đã uống ${takenTimes.size}/${reminderTimes.size} cữ (Đã uống: ${takenTimes.joinToString(", ")} • Còn lại: ${pendingTimes.joinToString(", ")})"
                }
            }

            TodayMedicineSummary(
                medicineId = med.id,
                name = med.name,
                dosage = normalizedDosage,
                unit = unit,
                purpose = med.purpose.ifBlank { "Điều trị theo đơn" },
                reminderTimes = reminderTimes,
                scheduleStatus = scheduleStatus
            )
        }

        // 4. Lấy tóm tắt các thành viên khác trong cùng tổ ấm gia đình
        val currentFamilyId = userDomain.familyId.trim()
        val fakeIds = setOf("user_01", "user_demo_01", "member_bac_nam", "member_bo", "member_me", "member_anhtrai", "user_02", "member_ly_tran")
        val allUsers = runCatching { userDao.getAllUsers() }.getOrDefault(emptyList())
        val otherUsers = if (currentFamilyId.isNotBlank()) {
            allUsers.filter {
                it.id != userId &&
                it.id !in fakeIds &&
                it.familyId.isNotBlank() &&
                it.familyId == currentFamilyId
            }
        } else emptyList()

        val otherMembers = otherUsers.map { other ->
            val otherAge = calculateAgeFromBirthDate(other.birthDate)
            val otherBp = healthMetricDao.getLatestMetricByType(other.id, MetricType.BLOOD_PRESSURE.name)
            val otherHr = healthMetricDao.getLatestMetricByType(other.id, MetricType.HEART_RATE.name)
            val otherMeds = runCatching {
                medicineDao?.getMedicinesByMember(other.id)?.firstOrNull().orEmpty()
            }.getOrDefault(emptyList())

            val bpStr = if (otherBp != null) {
                val dia = otherBp.valueSecondary?.toInt()
                if (dia != null) "${otherBp.valuePrimary.toInt()}/$dia mmHg" else "${otherBp.valuePrimary.toInt()} mmHg"
            } else "Chưa đo"

            val hrStr = if (otherHr != null) "${otherHr.valuePrimary.toInt()} BPM" else "Chưa đo"

            val medsStr = if (otherMeds.isEmpty()) {
                "Không có đơn thuốc"
            } else {
                otherMeds.joinToString(", ") { "${it.name} (${it.dosage})" }
            }

            OtherMemberHealthSummary(
                userId = other.id,
                fullName = other.fullName,
                age = otherAge,
                role = other.role,
                bloodPressureSummary = bpStr,
                heartRateSummary = hrStr,
                chronicDiseases = other.toDomainModel().chronicDiseases,
                medicinesSummary = medsStr
            )
        }

        UserHealthContext(
            userId = userId,
            fullName = userDomain.fullName,
            birthDate = userDomain.birthDate,
            calculatedAge = calculatedAge,
            bloodType = userDomain.bloodType,
            chronicDiseases = userDomain.chronicDiseases,
            drugAllergies = userDomain.drugAllergies,
            latestBloodPressure = bpEntity?.toDomainModel(),
            latestHeartRate = hrEntity?.toDomainModel(),
            latestBloodSugar = bsEntity?.toDomainModel(),
            todayMedicines = todayMedicines,
            otherFamilyMembers = otherMembers
        )
    }

    /**
     * Chuẩn hóa liều lượng dùng rõ ràng, dễ hiểu cho người cao tuổi
     */
    private fun normalizeDosage(name: String, rawDosage: String): String {
        val trimmed = rawDosage.trim()
        val isPowder = isLiquidOrPowder(name, rawDosage, "")
        val defaultUnit = if (isPowder) "gói" else "viên"

        if (trimmed.isBlank()) return "1 $defaultUnit"
        if (trimmed.contains("viên", ignoreCase = true) ||
            trimmed.contains("gói", ignoreCase = true) ||
            trimmed.contains("ống", ignoreCase = true) ||
            trimmed.contains("ml", ignoreCase = true)
        ) {
            return trimmed
        }

        return if (trimmed.matches("^\\d+$".toRegex())) {
            "$trimmed $defaultUnit"
        } else {
            trimmed
        }
    }

    private fun isLiquidOrPowder(vararg texts: String): Boolean {
        val combined = texts.joinToString(" ").lowercase(Locale.ROOT)
        return combined.contains("gói") ||
                combined.contains("sủi") ||
                combined.contains("bột") ||
                combined.contains("cốm") ||
                combined.contains("siro") ||
                combined.contains("dung dịch") ||
                combined.contains("nước") ||
                combined.contains("ống")
    }

    private fun calculateAgeFromBirthDate(birthDate: String): Int {
        if (birthDate.isBlank()) return 65
        val formats = listOf("dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy")
        for (fmt in formats) {
            try {
                val parsed = SimpleDateFormat(fmt, Locale.getDefault()).parse(birthDate)
                if (parsed != null) {
                    val dob = Calendar.getInstance().apply { time = parsed }
                    val today = Calendar.getInstance()
                    var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                    if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                        age--
                    }
                    if (age in 1..120) return age
                }
            } catch (_: Exception) {}
        }
        return 65
    }

    private suspend fun seedDefaultDemoUser(): UserEntity {
        val defaultUser = UserEntity(
            id = "user_me",
            fullName = "Ly Trần",
            email = "lytran@familycare.vn",
            avatarUrl = "",
            familyId = "fam_01",
            role = "ADMIN",
            birthDate = "20/11/1992",
            bloodType = "A+",
            chronicDiseasesJson = "[]",
            drugAllergiesJson = "[]"
        )
        userDao.insertUser(defaultUser)
        return defaultUser
    }
}
