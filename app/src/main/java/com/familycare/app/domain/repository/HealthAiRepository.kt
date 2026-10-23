package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.UserHealthContext
import kotlinx.coroutines.flow.Flow

/**
 * Interface Repository cho Trí tuệ nhân tạo Gemini Health Copilot
 * Tuân thủ Clean Architecture - Tách biệt hoàn toàn với tầng Data & SDK.
 */
interface HealthAiRepository {
    /**
     * Gửi tin nhắn đơn lẻ kèm ngữ cảnh sức khỏe tới Gemini 1.5 Flash
     */
    suspend fun sendMessage(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage> = emptyList()
    ): Resource<String>

    /**
     * Gửi tin nhắn dạng Stream (từng ký tự phản hồi thời gian thực)
     */
    fun sendMessageStream(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage> = emptyList()
    ): Flow<Resource<String>>
}
