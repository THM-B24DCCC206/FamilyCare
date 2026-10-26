package com.familycare.app.presentation.ai_assistant

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.Resource
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.data.repository.HealthAiRepositoryImpl
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.usecase.GetHealthContextUseCase
import com.familycare.app.domain.usecase.SendChatMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý luồng hội thoại của Trợ lý Ảo Sức khỏe (AI Health Copilot).
 * Tuân thủ Clean Architecture, quản lý trạng thái bằng StateFlow (Loading, Success, Error).
 */
class AiAssistantViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val getHealthContextUseCase: GetHealthContextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        loadHealthContextAndInitChat()
    }

    /**
     * Tải ngữ cảnh bệnh án từ Room Database và khởi tạo lời chào đầu tiên
     */
    fun loadHealthContextAndInitChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val context = getHealthContextUseCase()
            val initialGreeting = buildInitialGreeting(context)

            _uiState.update { state ->
                val currentMessages = if (state.messages.isEmpty()) {
                    listOf(
                        ChatMessage(
                            sender = MessageSender.AI,
                            text = initialGreeting,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                } else {
                    state.messages
                }
                state.copy(
                    healthContext = context,
                    messages = currentMessages,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Cập nhật nội dung ô nhập tin nhắn
     */
    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    /**
     * Gửi tin nhắn của người dùng lên Gemini
     * (Tự động thực hiện Context Injection từ Room Database)
     */
    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = (customPrompt ?: _uiState.value.inputText).trim()
        if (promptToSend.isBlank() || _uiState.value.isLoading) return

        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = promptToSend,
            timestamp = System.currentTimeMillis()
        )

        // Đẩy tin nhắn người dùng vào danh sách ngay lập tức, chuyển sang trạng thái Loading
        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                inputText = "",
                isLoading = true,
                errorMessage = null,
                chatStatus = ChatUiStatus.Loading
            )
        }

        viewModelScope.launch {
            val history = _uiState.value.messages.dropLast(1) // Lịch sử trước câu hỏi vừa gửi
            val context = _uiState.value.healthContext ?: getHealthContextUseCase()

            when (val result = sendChatMessageUseCase(promptToSend, history, context)) {
                is Resource.Success -> {
                    val aiReply = result.data ?: return@launch
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + aiReply,
                            isLoading = false,
                            errorMessage = null,
                            chatStatus = ChatUiStatus.Success(aiReply.id)
                        )
                    }
                }
                is Resource.Error -> {
                    val errorMsg = result.message ?: "Đã có lỗi xảy ra khi kết nối với AI"
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = errorMsg,
                            chatStatus = ChatUiStatus.Error(errorMsg)
                        )
                    }
                }
                is Resource.Loading -> {
                    _uiState.update { it.copy(isLoading = true, chatStatus = ChatUiStatus.Loading) }
                }
            }
        }
    }

    /**
     * Xử lý kết quả nhận dạng giọng nói (Voice-to-Text)
     */
    fun onVoiceInputResult(spokenText: String) {
        if (spokenText.isNotBlank()) {
            _uiState.update { it.copy(inputText = spokenText) }
        }
    }

    /**
     * Thử lại lượt gửi cuối cùng nếu bị lỗi
     */
    fun retryLastMessage() {
        val lastUserMessage = _uiState.value.messages.lastOrNull { it.sender == MessageSender.USER }
        if (lastUserMessage != null) {
            sendMessage(lastUserMessage.text)
        }
    }

    /**
     * Xóa thông báo lỗi hiển thị
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null, chatStatus = ChatUiStatus.Idle) }
    }

    /**
     * Bật/tắt hộp thoại xem trước Context Injection
     */
    fun toggleContextDialog(show: Boolean) {
        _uiState.update { it.copy(showContextDialog = show) }
    }

    /**
     * Khởi tạo lại cuộc trò chuyện
     */
    fun clearConversation() {
        val context = _uiState.value.healthContext
        val initialGreeting = if (context != null) buildInitialGreeting(context) else "Chào bác! Cháu là Trợ lý Sức khỏe AI."
        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        sender = MessageSender.AI,
                        text = initialGreeting,
                        timestamp = System.currentTimeMillis()
                    )
                ),
                inputText = "",
                errorMessage = null,
                chatStatus = ChatUiStatus.Idle
            )
        }
    }

    private fun buildInitialGreeting(context: com.familycare.app.domain.model.UserHealthContext): String {
        val bpSummary = context.latestBloodPressure?.let {
            "${it.valuePrimary.toInt()}/${it.valueSecondary?.toInt()} mmHg"
        } ?: "145/92 mmHg"

        return """
            Dạ kính chào ${context.fullName}! Cháu là **Trợ lý Ảo Sức khỏe FamilyCare**.
            
            Hệ thống vừa cập nhật chỉ số sức khỏe mới nhất của bác từ thiết bị:
            • **Huyết áp:** $bpSummary (Đo gần đây)
            • **Nhịp tim:** ${context.latestHeartRate?.valuePrimary?.toInt() ?: 86} BPM
            • **Tiền sử cần lưu ý:** ${context.chronicDiseases.firstOrNull() ?: "Cao huyết áp"}
            
            Hôm nay bác cảm thấy trong người thế nào ạ? Bác có thể gõ câu hỏi hoặc bấm nút **Micro** để nói chuyện trực tiếp với cháu nhé!
        """.trimIndent()
    }

    companion object {
        /**
         * Factory khởi tạo ViewModel thuận tiện, tự động cấu hình Room Database & Repository
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val sessionManager = SessionManager(context.applicationContext)
                val getContextUseCase = GetHealthContextUseCase(db.userDao(), db.healthMetricDao())
                val apiKey = sessionManager.geminiApiKey.ifBlank { HealthAiRepositoryImpl.DEFAULT_API_KEY }
                val aiRepo = HealthAiRepositoryImpl { apiKey }
                val sendChatUseCase = SendChatMessageUseCase(aiRepo, getContextUseCase)

                return AiAssistantViewModel(sendChatUseCase, getContextUseCase) as T
            }
        }
    }
}
