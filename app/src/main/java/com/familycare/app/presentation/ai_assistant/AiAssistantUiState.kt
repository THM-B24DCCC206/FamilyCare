package com.familycare.app.presentation.ai_assistant

import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.UserHealthContext

/**
 * Trạng thái thực thi xử lý tin nhắn của AI Copilot
 * (Yêu cầu 4: Loading, Success, Error)
 */
sealed interface ChatUiStatus {
    object Idle : ChatUiStatus
    object Loading : ChatUiStatus
    data class Success(val messageId: String) : ChatUiStatus
    data class Error(val errorMessage: String) : ChatUiStatus
}

/**
 * Toàn bộ trạng thái giao diện của màn hình AI Health Copilot
 */
data class AiAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val chatStatus: ChatUiStatus = ChatUiStatus.Idle,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val healthContext: UserHealthContext? = null,
    val isRecordingVoice: Boolean = false,
    val showContextDialog: Boolean = false,
    val quickQuestions: List<String> = listOf(
        "Huyết áp của tôi hôm nay 145/92 có đáng lo không bác sĩ?",
        "Tôi bị cao huyết áp có uống được thuốc Panadol Extra không?",
        "Nhịp tim 86 BPM sau khi đi bộ về có bình thường không?",
        "Bác sĩ tư vấn chế độ ăn giảm muối cho người cao tuổi giúp tôi."
    )
)
