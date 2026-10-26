package com.familycare.app.domain.usecase

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.ContextWarningLevel
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.model.UserHealthContext
import com.familycare.app.domain.repository.HealthAiRepository

/**
 * UseCase điều phối luồng trò chuyện:
 * 1. Thu thập Context sức khỏe mới nhất từ Room Database (GetHealthContextUseCase).
 * 2. Tiêm Context vào Gemini 1.5 Flash (Context Injection).
 * 3. Nhận phản hồi và phân tích mức độ cảnh báo (Normal/Attention/Critical).
 */
class SendChatMessageUseCase(
    private val healthAiRepository: HealthAiRepository,
    private val getHealthContextUseCase: GetHealthContextUseCase
) {

    suspend operator fun invoke(
        userPrompt: String,
        history: List<ChatMessage>,
        overrideContext: UserHealthContext? = null
    ): Resource<ChatMessage> {
        if (userPrompt.trim().isBlank()) {
            return Resource.Error("Nội dung tin nhắn không được để trống")
        }

        // Lấy ngữ cảnh y tế thời gian thực từ Room
        val healthContext = overrideContext ?: getHealthContextUseCase()

        // Gửi tới Gemini qua Repository
        val result = healthAiRepository.sendMessage(
            userPrompt = userPrompt,
            context = healthContext,
            history = history
        )

        return when (result) {
            is Resource.Success -> {
                val replyText = result.data ?: ""
                val warningLevel = analyzeWarningLevel(userPrompt, replyText)
                val actions = extractSuggestedActions(userPrompt, replyText)

                val aiMessage = ChatMessage(
                    sender = MessageSender.AI,
                    text = replyText,
                    timestamp = System.currentTimeMillis(),
                    warningLevel = warningLevel,
                    suggestedActions = actions
                )
                Resource.Success(aiMessage)
            }
            is Resource.Error -> {
                Resource.Error(result.message ?: "Không thể kết nối với Trợ lý AI")
            }
            is Resource.Loading -> {
                Resource.Loading()
            }
        }
    }

    /**
     * Phân tích nội dung để gắn nhãn cảnh báo trực quan cho tin nhắn AI trên giao diện
     */
    private fun analyzeWarningLevel(userPrompt: String, aiReply: String): ContextWarningLevel {
        val combined = "$userPrompt $aiReply".lowercase()
        return when {
            combined.contains("cấp cứu") ||
            combined.contains("nguy kịch") ||
            combined.contains("khẩn cấp") ||
            combined.contains("gọi 115") ||
            combined.contains("nhập viện ngay") ||
            combined.contains("đột quỵ") ||
            combined.contains("tai biến") -> ContextWarningLevel.CRITICAL

            combined.contains("chú ý") ||
            combined.contains("vượt ngưỡng") ||
            combined.contains("hơi cao") ||
            combined.contains("tăng huyết áp") ||
            combined.contains("dị ứng") -> ContextWarningLevel.ATTENTION

            else -> ContextWarningLevel.NORMAL
        }
    }

    private fun extractSuggestedActions(userPrompt: String, aiReply: String): List<String> {
        val actions = mutableListOf<String>()
        val lower = aiReply.lowercase()
        if (lower.contains("uống thuốc") || lower.contains("đơn thuốc")) {
            actions.add("Xem lịch uống thuốc")
        }
        if (lower.contains("đo lại") || lower.contains("huyết áp") || lower.contains("nhịp tim")) {
            actions.add("Đo chỉ số ngay")
        }
        if (lower.contains("bác sĩ") || lower.contains("khám")) {
            actions.add("Đặt lịch khám bệnh")
        }
        return actions
    }
}
