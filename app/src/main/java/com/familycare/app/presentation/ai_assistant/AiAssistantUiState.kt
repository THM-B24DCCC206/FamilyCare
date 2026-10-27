package com.familycare.app.presentation.ai_assistant

import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.UserHealthContext
import com.familycare.app.presentation.home.Member

/**
 * Trạng thái thực thi xử lý tin nhắn của AI Copilot
 * (Loading, Success, Error)
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
    val familyMembers: List<Member> = emptyList(),
    val selectedMemberId: String = "",
    val quickQuestions: List<String> = emptyList()
)
