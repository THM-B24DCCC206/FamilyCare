package com.familycare.app.domain.usecase

import com.familycare.app.core.database.dao.HealthMetricDao
import com.familycare.app.core.database.dao.UserDao
import com.familycare.app.core.database.entity.HealthMetricEntity
import com.familycare.app.core.database.entity.UserEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.domain.model.UserHealthContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * UseCase truy xuất hồ sơ bệnh án và các chỉ số đo mới nhất từ Room Database
 * để chuẩn bị cho Context Injection vào Gemini AI.
 */
class GetHealthContextUseCase(
    private val userDao: UserDao,
    private val healthMetricDao: HealthMetricDao
) {

    suspend operator fun invoke(targetUserId: String? = null): UserHealthContext = withContext(Dispatchers.IO) {
        // 1. Lấy thông tin người dùng hiện tại từ Room
        val userEntity = if (!targetUserId.isNullOrBlank()) {
            userDao.getUserById(targetUserId)
        } else {
            userDao.getCurrentUser()
        } ?: seedDefaultDemoUser() // Dự phòng mẫu bệnh nhân nếu cơ sở dữ liệu trống

        val userId = userEntity.id

        // 2. Lấy chỉ số huyết áp mới nhất từ Room
        val bpEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.BLOOD_PRESSURE.name)
            ?: seedDefaultBloodPressure(userId)

        // 3. Lấy chỉ số nhịp tim mới nhất từ Room
        val hrEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.HEART_RATE.name)
            ?: seedDefaultHeartRate(userId)

        // 4. Lấy chỉ số đường huyết nếu có
        val bsEntity = healthMetricDao.getLatestMetricByType(userId, MetricType.BLOOD_SUGAR.name)

        val userDomain = userEntity.toDomainModel()

        UserHealthContext(
            userId = userId,
            fullName = userDomain.fullName,
            birthDate = userDomain.birthDate,
            calculatedAge = 68,
            bloodType = userDomain.bloodType,
            chronicDiseases = userDomain.chronicDiseases,
            drugAllergies = userDomain.drugAllergies,
            latestBloodPressure = bpEntity.toDomainModel(),
            latestHeartRate = hrEntity.toDomainModel(),
            latestBloodSugar = bsEntity?.toDomainModel()
        )
    }

    /**
     * Dữ liệu mẫu chuẩn lâm sàng dành cho người cao tuổi trong gia đình
     */
    private suspend fun seedDefaultDemoUser(): UserEntity {
        val defaultUser = UserEntity(
            id = "user_demo_01",
            fullName = "Bác Nguyễn Văn Nam",
            email = "nam.nguyen@familycare.vn",
            avatarUrl = "",
            familyId = "fam_123456",
            role = "MEMBER",
            birthDate = "15/04/1958",
            bloodType = "O",
            chronicDiseasesJson = "[\"Cao huyết áp vô căn (10 năm)\", \"Rối loạn lipid máu\", \"Đái tháo đường type 2\"]",
            drugAllergiesJson = "[\"Dị ứng nhóm Sulfamid\", \"Dị ứng Aspirin\"]"
        )
        userDao.insertUser(defaultUser)
        return defaultUser
    }

    private suspend fun seedDefaultBloodPressure(userId: String): HealthMetricEntity {
        val bp = HealthMetricEntity(
            id = "metric_bp_01",
            userId = userId,
            type = MetricType.BLOOD_PRESSURE.name,
            valuePrimary = 145.0,  // Tâm thu 145 mmHg
            valueSecondary = 92.0, // Tâm trương 92 mmHg
            unit = "mmHg",
            timestamp = System.currentTimeMillis() - 25 * 60 * 1000L, // Đo cách đây 25 phút
            notes = "Đo lúc vừa đi bộ về, cảm thấy hơi nóng mặt",
            isFromCameraPpg = false
        )
        healthMetricDao.insertMetric(bp)
        return bp
    }

    private suspend fun seedDefaultHeartRate(userId: String): HealthMetricEntity {
        val hr = HealthMetricEntity(
            id = "metric_hr_01",
            userId = userId,
            type = MetricType.HEART_RATE.name,
            valuePrimary = 86.0,   // 86 BPM
            valueSecondary = null,
            unit = "BPM",
            timestamp = System.currentTimeMillis() - 25 * 60 * 1000L,
            notes = "Đo qua Camera PPG quang học",
            isFromCameraPpg = true
        )
        healthMetricDao.insertMetric(hr)
        return hr
    }
}
