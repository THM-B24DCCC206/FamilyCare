package com.familycare.app

import com.familycare.app.core.common.Resource
import com.familycare.app.core.emergency.EmergencyPayload
import com.familycare.app.domain.model.*
import com.familycare.app.domain.usecase.AnalyzeThresholdUseCase
import com.familycare.app.presentation.ai_assistant.ChatUiStatus
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AiHealthCopilotTest {

    @Test
    fun testContextInjectionGeneratesProperSystemInstructions() {
        val bp = HealthMetric(
            id = "bp1",
            userId = "u1",
            type = MetricType.BLOOD_PRESSURE,
            valuePrimary = 150.0,
            valueSecondary = 95.0,
            unit = "mmHg"
        )
        val hr = HealthMetric(
            id = "hr1",
            userId = "u1",
            type = MetricType.HEART_RATE,
            valuePrimary = 82.0,
            unit = "BPM"
        )

        val context = UserHealthContext(
            userId = "u1",
            fullName = "Bác Trần Văn An",
            calculatedAge = 72,
            chronicDiseases = listOf("Cao huyết áp", "Đái tháo đường"),
            drugAllergies = listOf("Aspirin"),
            latestBloodPressure = bp,
            latestHeartRate = hr
        )

        val instructions = context.buildSystemInstruction()

        // Kiểm tra System Instructions đã chứa đầy đủ Context Injection
        assertTrue(instructions.contains("Bác Trần Văn An"))
        assertTrue(instructions.contains("72 tuổi"))
        assertTrue(instructions.contains("Cao huyết áp"))
        assertTrue(instructions.contains("Aspirin"))
        assertTrue(instructions.contains("150/95 mmHg"))
        assertTrue(instructions.contains("82 BPM"))
        assertTrue(instructions.contains("CLINICAL & ETHICAL GUIDELINES"))
    }

    @Test
    fun testChatMessageDataModel() {
        val message = ChatMessage(
            sender = MessageSender.AI,
            text = "Huyết áp 150/95 là mức tăng huyết áp độ 1, bác nên nghỉ ngơi.",
            warningLevel = ContextWarningLevel.ATTENTION,
            suggestedActions = listOf("Đo lại sau 15 phút", "Uống thuốc đúng giờ")
        )

        assertEquals(MessageSender.AI, message.sender)
        assertEquals(ContextWarningLevel.ATTENTION, message.warningLevel)
        assertEquals(2, message.suggestedActions.size)
    }

    @Test
    fun testChatUiStatusStates() {
        val idleStatus: ChatUiStatus = ChatUiStatus.Idle
        val loadingStatus: ChatUiStatus = ChatUiStatus.Loading
        val successStatus: ChatUiStatus = ChatUiStatus.Success("msg-123")
        val errorStatus: ChatUiStatus = ChatUiStatus.Error("Lỗi kết nối")

        assertTrue(idleStatus is ChatUiStatus.Idle)
        assertTrue(loadingStatus is ChatUiStatus.Loading)
        assertTrue(successStatus is ChatUiStatus.Success)
        assertEquals("msg-123", (successStatus as ChatUiStatus.Success).messageId)
        assertTrue(errorStatus is ChatUiStatus.Error)
        assertEquals("Lỗi kết nối", (errorStatus as ChatUiStatus.Error).errorMessage)
    }

    @Test
    fun testAnalyzeThresholdUseCase_TriggersRedAlertOnThreeConsecutiveBreaches() = runBlocking {
        val useCase = AnalyzeThresholdUseCase(alertDao = null)
        val now = System.currentTimeMillis()

        // Tạo chuỗi 3 lần đo liên tiếp vượt ngưỡng trong vòng 48h
        val metrics = listOf(
            HealthMetric(id = "m1", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 155.0, valueSecondary = 98.0, unit = "mmHg", timestamp = now - 1 * 3600 * 1000L),
            HealthMetric(id = "m2", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 148.0, valueSecondary = 94.0, unit = "mmHg", timestamp = now - 5 * 3600 * 1000L),
            HealthMetric(id = "m3", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 145.0, valueSecondary = 92.0, unit = "mmHg", timestamp = now - 12 * 3600 * 1000L)
        )

        val results = useCase.invoke(
            userId = "u1",
            metrics = metrics,
            currentTime = now
        ).toList()

        val successResult = results.last() as Resource.Success
        val analysis = successResult.data!!

        // Xác minh cảnh báo đỏ đã được kích hoạt
        assertTrue(analysis.isCriticalRedAlert)
        assertEquals(3, analysis.consecutiveBreachCount)
        assertEquals(3, analysis.totalBreachesInWindow)
        assertFalse(analysis.isDebounced)
    }

    @Test
    fun testAnalyzeThresholdUseCase_NormalMeasurementBreaksSequence() = runBlocking {
        val useCase = AnalyzeThresholdUseCase(alertDao = null)
        val now = System.currentTimeMillis()

        // Đo lần gần nhất bình thường (120/80) -> Phải ngắt chuỗi liên tiếp
        val metrics = listOf(
            HealthMetric(id = "m1", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 120.0, valueSecondary = 80.0, unit = "mmHg", timestamp = now - 1 * 3600 * 1000L), // Bình thường
            HealthMetric(id = "m2", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 148.0, valueSecondary = 94.0, unit = "mmHg", timestamp = now - 5 * 3600 * 1000L), // Vượt
            HealthMetric(id = "m3", userId = "u1", type = MetricType.BLOOD_PRESSURE, valuePrimary = 145.0, valueSecondary = 92.0, unit = "mmHg", timestamp = now - 12 * 3600 * 1000L) // Vượt
        )

        val results = useCase.invoke(
            userId = "u1",
            metrics = metrics,
            currentTime = now
        ).toList()

        val successResult = results.last() as Resource.Success
        val analysis = successResult.data!!

        // Chuỗi liên tiếp từ lần đo gần nhất = 0 -> Không kích hoạt cảnh báo đỏ
        assertFalse(analysis.isCriticalRedAlert)
        assertEquals(0, analysis.consecutiveBreachCount)
        assertEquals(2, analysis.totalBreachesInWindow)
    }

    @Test
    fun testEmergencyPayloadFcmGeneration() {
        val payload = EmergencyPayload(
            eventId = "evt-123",
            userId = "user_01",
            userName = "Bác Nam",
            familyId = "fam_123",
            latitude = 10.7769,
            longitude = 106.7009
        )

        val fcmMap = payload.toFcmDataMap()

        assertEquals("high", fcmMap["priority"])
        assertEquals("SOS_EMERGENCY", fcmMap["type"])
        assertEquals("10.7769", fcmMap["latitude"])
        assertEquals("106.7009", fcmMap["longitude"])
        assertEquals("https://maps.google.com/?q=10.7769,106.7009", fcmMap["mapsUrl"])
        assertTrue(fcmMap["title"]!!.contains("Bác Nam CẦN TRỢ GIÚP"))
    }
}
