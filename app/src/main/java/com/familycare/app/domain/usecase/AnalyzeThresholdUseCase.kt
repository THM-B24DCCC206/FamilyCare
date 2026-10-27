package com.familycare.app.domain.usecase

import com.familycare.app.core.common.Resource
import com.familycare.app.core.database.AlertDao
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * UseCase phân tích ngưỡng sức khỏe & Điều phối 3 nguồn cảnh báo (Clean Architecture - Domain Layer).
 * 
 * Nghiệp vụ chính:
 * 1. Nguồn 1: Cấp cứu SOS (SOS_EMERGENCY) -> AlertLevel.CRITICAL, không debounce.
 * 2. Nguồn 2: Vượt ngưỡng sinh tồn (THRESHOLD_EXCEEDED) -> Quét 48h, >= 3 lần liên tiếp -> Debounce 6h -> AlertLevel.CRITICAL.
 * 3. Nguồn 3: Nhắc cữ thuốc (MEDICATION_MISSED) -> Quá 30 phút -> AlertLevel.WARNING.
 */
class AnalyzeThresholdUseCase(
    private val alertDao: AlertDao? = null
) {

    /**
     * Nguồn 1: Cấp cứu SOS (SOS_EMERGENCY)
     * Nhận payload từ thiết bị phát SOS hoặc FCM (chứa tên người bị nạn thật, tọa độ thật và địa chỉ số nhà tiếng Việt thật).
     * Lưu ngay bản ghi AlertLevel.CRITICAL, không áp dụng debounce.
     */
    suspend fun recordSosEmergency(
        memberId: String,
        memberName: String,
        address: String,
        latitude: Double? = null,
        longitude: Double? = null,
        familyId: String = ""
    ): AlertEntity {
        val alert = AlertEntity(
            memberId = memberId,
            memberName = memberName,
            alertType = AlertType.SOS_EMERGENCY,
            alertLevel = AlertLevel.CRITICAL,
            title = "🚨 BÁO ĐỘNG SOS KHẨN CẤP",
            message = "$memberName đã kích hoạt còi cứu hộ khẩn cấp tại: $address",
            address = address,
            latitude = latitude,
            longitude = longitude,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isResolved = false,
            familyId = familyId
        )
        alertDao?.insertAlert(alert)
        return alert
    }

    /**
     * Nguồn 3: Nhắc cữ thuốc (MEDICATION_MISSED)
     * Khi người thân quên uống thuốc quá 30 phút so với giờ hẹn, sinh bản ghi AlertLevel.WARNING.
     */
    suspend fun recordMedicationMissed(
        memberId: String,
        memberName: String,
        medicineName: String,
        scheduledTime: String,
        familyId: String = ""
    ): AlertEntity {
        val alert = AlertEntity(
            memberId = memberId,
            memberName = memberName,
            alertType = AlertType.MEDICATION_MISSED,
            alertLevel = AlertLevel.WARNING,
            title = "💊 BỎ LỠ CỮ THUỐC: QUÁ 30 PHÚT",
            message = "$memberName chưa uống thuốc $medicineName theo lịch hẹn $scheduledTime.",
            address = null,
            timestamp = System.currentTimeMillis(),
            isRead = false,
            isResolved = false,
            familyId = familyId
        )
        alertDao?.insertAlert(alert)
        return alert
    }

    /**
     * Nguồn 2: Vượt ngưỡng sinh tồn (THRESHOLD_EXCEEDED)
     * Quét lịch sử đo trong 48h, phát hiện >= 3 lần đo liên tiếp vượt ngưỡng.
     * Áp dụng Debounce Window 6 giờ để chống quá tải thông báo (Alert Fatigue).
     */
    operator fun invoke(
        userId: String,
        metrics: List<HealthMetric>,
        threshold: HealthThreshold = HealthThreshold(userId = userId),
        userName: String = "Người thân",
        familyId: String = "",
        currentTime: Long = System.currentTimeMillis(),
        debounceWindowMs: Long = DEFAULT_DEBOUNCE_WINDOW_MS
    ): Flow<Resource<ThresholdAnalysisResult>> = flow {
        emit(Resource.Loading())

        if (metrics.isEmpty()) {
            val emptyResult = ThresholdAnalysisResult(
                userId = userId,
                userName = userName,
                isCriticalRedAlert = false,
                consecutiveBreachCount = 0,
                summaryMessage = "Chưa có dữ liệu đo trong 48 giờ qua."
            )
            emit(Resource.Success(emptyResult))
            return@flow
        }

        try {
            // 1. Lọc các lần đo trong cửa sổ 48 giờ gần nhất
            val window48HoursAgo = currentTime - TIME_WINDOW_48_HOURS_MS
            val metricsIn48h = metrics
                .filter { it.userId == userId && it.timestamp >= window48HoursAgo }
                .sortedByDescending { it.timestamp } // Mới nhất lên đầu

            if (metricsIn48h.isEmpty()) {
                emit(Resource.Success(
                    ThresholdAnalysisResult(
                        userId = userId,
                        userName = userName,
                        isCriticalRedAlert = false,
                        consecutiveBreachCount = 0,
                        summaryMessage = "Không có lần đo nào trong 48 giờ gần nhất."
                    )
                ))
                return@flow
            }

            // 2. Thuật toán phát hiện chuỗi liên tiếp (Consecutive Breach Algorithm)
            val consecutiveBreachList = mutableListOf<HealthMetric>()
            var consecutiveCount = 0

            for (metric in metricsIn48h) {
                if (isMetricExceedingThreshold(metric, threshold)) {
                    consecutiveCount++
                    consecutiveBreachList.add(metric)
                } else {
                    // Ngay khi gặp 1 lần đo an toàn -> Chuỗi liên tiếp từ hiện tại bị ngắt
                    break
                }
            }

            // Đếm tổng số lần đo vượt ngưỡng trong cả 48h để hiển thị thống kê
            val allBreachesIn48h = metricsIn48h.filter { isMetricExceedingThreshold(it, threshold) }

            // 3. Đánh giá trạng thái cảnh báo đỏ (>= 3 lần đo liên tiếp vượt ngưỡng)
            val isCritical = consecutiveCount >= CONSECUTIVE_BREACH_TRIGGER

            // 4. Cơ chế chống lặp cảnh báo (Deduplication / Debounce)
            var isDebounced = false
            var generatedAlert: Alert? = null

            if (isCritical) {
                // Áp dụng Debounce Window 6 giờ: Kiểm tra bản ghi gần nhất của memberId với loại THRESHOLD_EXCEEDED
                val latestAlert = alertDao?.getLatestAlertForMember(userId, AlertType.THRESHOLD_EXCEEDED)
                val isWithinDebounce = latestAlert != null && (currentTime - latestAlert.timestamp) < debounceWindowMs

                if (isWithinDebounce) {
                    // Đã có cảnh báo trong 6h qua -> Bỏ qua không tạo bản ghi mới để chống Alert Fatigue
                    isDebounced = true
                } else {
                    // Chưa cảnh báo trong 6h qua -> Sinh một cảnh báo đỏ mới và lưu vào Room DB
                    val latest = metricsIn48h.first()
                    val alertMessage = buildBreachAlertMessage(userName, consecutiveCount, latest)
                    val newAlert = AlertEntity(
                        memberId = userId,
                        memberName = userName,
                        alertType = AlertType.THRESHOLD_EXCEEDED,
                        alertLevel = AlertLevel.CRITICAL,
                        title = "⚠️ CẢNH BÁO ĐỎ: $consecutiveCount LẦN ĐO VƯỢT NGƯỠNG LIÊN TIẾP",
                        message = alertMessage,
                        timestamp = currentTime,
                        isRead = false,
                        isResolved = false,
                        familyId = familyId
                    )

                    alertDao?.insertAlert(newAlert)
                    generatedAlert = newAlert.toDomain()
                }
            }

            // 5. Tổng hợp thông điệp & khuyến nghị lâm sàng cho người thân
            val summary = when {
                isCritical -> "Cảnh báo khẩn cấp: Ghi nhận $consecutiveCount lần đo liên tiếp vượt ngưỡng an toàn trong 48 giờ qua."
                consecutiveCount > 0 -> "Ghi nhận $consecutiveCount lần đo vượt ngưỡng gần nhất. Vui lòng theo dõi thêm."
                else -> "Tất cả các chỉ số sinh tồn trong 48 giờ qua đều nằm trong vùng an toàn."
            }

            val recommendations = generateClinicalRecommendations(consecutiveCount, metricsIn48h.firstOrNull())

            val finalResult = ThresholdAnalysisResult(
                userId = userId,
                userName = userName,
                isCriticalRedAlert = isCritical,
                consecutiveBreachCount = consecutiveCount,
                totalBreachesInWindow = allBreachesIn48h.size,
                breachedMetrics = consecutiveBreachList,
                latestMetric = metricsIn48h.firstOrNull(),
                generatedAlert = generatedAlert,
                isDebounced = isDebounced,
                summaryMessage = summary,
                recommendations = recommendations
            )

            emit(Resource.Success(finalResult))

        } catch (e: Exception) {
            emit(Resource.Error("Lỗi khi phân tích ngưỡng chỉ số: ${e.localizedMessage ?: "Không xác định"}"))
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Kiểm tra một lần đo cụ thể có vượt ngưỡng hay không:
     * Huyết áp tâm thu >= 140 mmHg hoặc tâm trương >= 90 mmHg, hoặc nhịp tim > 100 BPM.
     */
    fun isMetricExceedingThreshold(metric: HealthMetric, threshold: HealthThreshold): Boolean {
        return when (metric.type) {
            MetricType.BLOOD_PRESSURE -> {
                val sys = metric.valuePrimary
                val dia = metric.valueSecondary ?: 0.0
                sys >= threshold.systolicMax || dia >= threshold.diastolicMax || sys < threshold.systolicMin
            }
            MetricType.HEART_RATE -> {
                val bpm = metric.valuePrimary
                bpm >= threshold.heartRateMax || bpm < threshold.heartRateMin
            }
            MetricType.BLOOD_SUGAR -> {
                metric.valuePrimary >= threshold.bloodSugarMax
            }
            MetricType.TEMPERATURE -> {
                metric.valuePrimary >= 38.5 || metric.valuePrimary < 35.5
            }
        }
    }

    private fun buildBreachAlertMessage(userName: String, breachCount: String, latest: HealthMetric): String {
        return "Người thân $userName đã có $breachCount lần đo vượt ngưỡng liên tiếp. " +
                "Chỉ số gần nhất: ${latest.valuePrimary.toInt()}${latest.valueSecondary?.let { "/${it.toInt()}" } ?: ""} ${latest.unit}. " +
                "Vui lòng liên hệ và kiểm tra tình trạng sức khỏe ngay!"
    }

    private fun buildBreachAlertMessage(userName: String, breachCount: Int, latest: HealthMetric): String {
        return buildBreachAlertMessage(userName, breachCount.toString(), latest)
    }

    private fun generateClinicalRecommendations(consecutiveCount: Int, latest: HealthMetric?): List<String> {
        val list = mutableListOf<String>()
        if (consecutiveCount >= 3) {
            list.add("Khuyên người thân ngồi nghỉ ngơi tại chỗ, thư giãn hít thở sâu.")
            list.add("Kiểm tra xem người thân đã uống thuốc huyết áp theo toa hôm nay chưa.")
            list.add("Nếu kèm triệu chứng đau thắt ngực, khó thở hoặc chóng mặt: Gọi ngay 115 hoặc bấm SOS.")
            list.add("Đo lại chỉ số sau 15-20 phút nghỉ ngơi.")
        } else if (consecutiveCount in 1..2) {
            list.add("Chỉ số vừa đo hơi cao so với ngưỡng an toàn.")
            list.add("Nhắc người thân uống đủ nước ấm và tránh vận động gắng sức.")
        } else {
            list.add("Chỉ số đo ổn định. Tiếp tục duy trì chế độ sinh hoạt lành mạnh.")
        }
        return list
    }

    companion object {
        const val TIME_WINDOW_48_HOURS_MS = 48 * 60 * 60 * 1000L
        const val CONSECUTIVE_BREACH_TRIGGER = 3
        const val DEFAULT_DEBOUNCE_WINDOW_MS = 6 * 60 * 60 * 1000L // 6 giờ chống spam cảnh báo trùng
    }
}
