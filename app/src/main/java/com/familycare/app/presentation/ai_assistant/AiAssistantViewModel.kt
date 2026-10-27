package com.familycare.app.presentation.ai_assistant

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.Resource
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.UserDao
import com.familycare.app.core.database.UserEntity
import com.familycare.app.data.repository.HealthAiRepositoryImpl
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.model.UserHealthContext
import com.familycare.app.domain.usecase.GetHealthContextUseCase
import com.familycare.app.domain.usecase.SendChatMessageUseCase
import com.familycare.app.presentation.home.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel quản lý luồng hội thoại của Trợ lý Ảo Sức khỏe (AI Health Copilot).
 * Tuân thủ Clean Architecture, quản lý trạng thái bằng StateFlow (Loading, Success, Error).
 * Quản lý bối cảnh thành viên gia đình và tiêm 100% dữ liệu thực tế từ Room Database.
 */
class AiAssistantViewModel(
    private val sendChatMessageUseCase: SendChatMessageUseCase,
    private val getHealthContextUseCase: GetHealthContextUseCase,
    private val userDao: UserDao,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val currentUserId: String = sessionManager.userId.ifBlank { "user_me" }

    // State mặc định cố định: Biến selectedMemberId được khởi tạo bằng currentUserId của tài khoản đăng nhập
    private val _selectedMemberId = MutableStateFlow(currentUserId)
    val selectedMemberId: StateFlow<String> = _selectedMemberId.asStateFlow()

    private val _uiState = MutableStateFlow(
        AiAssistantUiState(
            selectedMemberId = currentUserId,
            currentApiKey = sessionManager.geminiApiKey
        )
    )
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        loadFamilyMembersAndInitChat()
    }

    /**
     * Tải danh sách thành viên gia đình và khởi tạo lời chào đầu tiên
     */
    fun loadFamilyMembersAndInitChat() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val members = withContext(Dispatchers.IO) { loadFamilyMembersInternal() }
            val targetMemberId = _selectedMemberId.value.ifBlank { currentUserId }

            val context = getHealthContextUseCase(targetMemberId)
            val isMe = (targetMemberId == currentUserId)
            val initialGreeting = buildInitialGreeting(context, isMe)
            val initialQuestions = generateQuickQuestions(context.fullName, isMe)

            _uiState.update { state ->
                val currentMessages = if (state.messages.isEmpty()) {
                    listOf(
                        ChatMessage(
                            sender = MessageSender.AI,
                            text = initialGreeting,
                            timestamp = System.currentTimeMillis(),
                            targetMemberName = context.fullName,
                            targetMemberId = context.userId
                        )
                    )
                } else {
                    state.messages
                }
                state.copy(
                    familyMembers = members,
                    selectedMemberId = targetMemberId,
                    healthContext = context,
                    messages = currentMessages,
                    quickQuestions = initialQuestions,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Chuyển đổi thành viên gia đình được chọn (Member Switcher)
     * Tự động nạp lại ngữ cảnh Room Database và gửi thông báo chuyển hồ sơ của AI
     */
    fun onMemberSelected(memberId: String) {
        if (memberId.isBlank() || memberId == _selectedMemberId.value) return
        _selectedMemberId.value = memberId

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedMemberId = memberId) }
            val newContext = getHealthContextUseCase(memberId)
            val isMe = (memberId == currentUserId)

            val switchNotice = "Cháu đã chuyển sang theo dõi hồ sơ của bác ${newContext.fullName}. Cháu có thể hỗ trợ gì về sức khỏe của bác ấy ạ?"

            val aiSwitchMessage = ChatMessage(
                sender = MessageSender.AI,
                text = switchNotice,
                timestamp = System.currentTimeMillis(),
                targetMemberName = newContext.fullName,
                targetMemberId = newContext.userId
            )

            _uiState.update { state ->
                state.copy(
                    selectedMemberId = memberId,
                    healthContext = newContext,
                    messages = state.messages + aiSwitchMessage,
                    quickQuestions = generateQuickQuestions(newContext.fullName, isMe),
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
     * (Tự động thực hiện Context Injection từ Room Database theo thành viên đang chọn)
     */
    fun sendMessage(customPrompt: String? = null) {
        val promptToSend = (customPrompt ?: _uiState.value.inputText).trim()
        if (promptToSend.isBlank() || _uiState.value.isLoading) return

        val currentContext = _uiState.value.healthContext
        val targetName = currentContext?.fullName ?: "Thành viên"
        val targetId = currentContext?.userId ?: _selectedMemberId.value

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
            val history = _uiState.value.messages.dropLast(1)
            val context = getHealthContextUseCase(_selectedMemberId.value)

            when (val result = sendChatMessageUseCase(promptToSend, history, context)) {
                is Resource.Success -> {
                    val rawAiMessage = result.data ?: return@launch
                    val enrichedAiMessage = rawAiMessage.copy(
                        targetMemberName = targetName,
                        targetMemberId = targetId
                    )
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages + enrichedAiMessage,
                            isLoading = false,
                            errorMessage = null,
                            chatStatus = ChatUiStatus.Success(enrichedAiMessage.id)
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
     * Cập nhật trạng thái đang thu âm giọng nói
     */
    fun setRecordingVoice(isRecording: Boolean) {
        _uiState.update { it.copy(isRecordingVoice = isRecording) }
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
     * Bật/tắt hộp thoại cấu hình Google Gemini API Key
     */
    fun toggleApiKeyDialog(show: Boolean) {
        _uiState.update { it.copy(showApiKeyDialog = show) }
    }

    /**
     * Lưu khóa Google Gemini API Key vào SharedPreferences và thử gửi lại nếu có
     */
    fun saveApiKey(newKey: String) {
        val cleanKey = newKey.trim()
        sessionManager.saveGeminiApiKey(cleanKey)
        _uiState.update {
            it.copy(
                currentApiKey = cleanKey,
                showApiKeyDialog = false,
                errorMessage = null
            )
        }
        retryLastMessage()
    }

    /**
     * Khởi tạo lại cuộc trò chuyện cho thành viên đang chọn
     */
    fun clearConversation() {
        val context = _uiState.value.healthContext
        val isMe = (_selectedMemberId.value == currentUserId)
        val initialGreeting = if (context != null) {
            buildInitialGreeting(context, isMe)
        } else {
            "Chào bác! Cháu là Trợ lý Sức khỏe AI."
        }

        _uiState.update {
            it.copy(
                messages = listOf(
                    ChatMessage(
                        sender = MessageSender.AI,
                        text = initialGreeting,
                        timestamp = System.currentTimeMillis(),
                        targetMemberName = context?.fullName,
                        targetMemberId = context?.userId
                    )
                ),
                inputText = "",
                errorMessage = null,
                chatStatus = ChatUiStatus.Idle
            )
        }
    }

    private suspend fun loadFamilyMembersInternal(): List<Member> {
        val currentUserName = sessionManager.userName.ifBlank { "Tôi" }
        val currentUserEmail = sessionManager.userEmail
        val currentRole = sessionManager.userRole

        // Đảm bảo user hiện tại có trong DB
        var currentUser = userDao.getUserById(currentUserId)
        if (currentUser == null) {
            val newCurr = UserEntity(
                id = currentUserId,
                fullName = currentUserName,
                email = currentUserEmail,
                role = currentRole,
                birthDate = "15/04/1958",
                bloodType = "O+",
                chronicDiseasesJson = "[]",
                drugAllergiesJson = "[]"
            )
            userDao.insertUser(newCurr)
            currentUser = newCurr
        }

        runCatching {
            userDao.deleteUserById("user_01")
            userDao.deleteUserById("user_demo_01")
            userDao.deleteUserById("member_bac_nam")
        }

        var allDbUsers = userDao.getAllUsers()
        val otherMembersInDb = allDbUsers.filter {
            it.id != currentUserId && it.id != "user_01" && it.id != "user_demo_01" && it.id != "member_bac_nam"
        }

        if (otherMembersInDb.isEmpty()) {
            val isCurrentLy = currentUser.fullName.contains("Ly", ignoreCase = true)
            if (isCurrentLy) {
                val haiMinh = UserEntity(
                    id = "user_02",
                    fullName = "Hải Minh Trần",
                    email = "haiminh@familycare.vn",
                    role = "MEMBER",
                    birthDate = "28/11/2006",
                    bloodType = "O+",
                    chronicDiseasesJson = "[]",
                    drugAllergiesJson = "[]"
                )
                userDao.insertUser(haiMinh)
            } else {
                val lyTran = UserEntity(
                    id = "member_ly_tran",
                    fullName = "Ly Trần",
                    email = "ly.tran@familycare.vn",
                    role = "ADMIN",
                    birthDate = "20/11/1992",
                    bloodType = "A+",
                    chronicDiseasesJson = "[]",
                    drugAllergiesJson = "[]"
                )
                userDao.insertUser(lyTran)
            }
            allDbUsers = userDao.getAllUsers()
        }

        val membersList = mutableListOf<Member>()
        val curDomain = currentUser.toDomainModel()
        membersList.add(
            Member(
                id = currentUserId,
                fullName = curDomain.fullName.ifBlank { "Tôi" },
                avatarUrl = curDomain.avatarUrl.takeIf { it.isNotBlank() },
                initials = "Tôi",
                role = "Tôi"
            )
        )

        allDbUsers
            .filter { it.id != currentUserId && it.id != "user_01" && it.id != "user_demo_01" }
            .forEach { u ->
                val displayRole = when (u.role) {
                    "ADMIN" -> "Trưởng gia đình"
                    "MEMBER" -> "Thành viên"
                    else -> u.role
                }
                membersList.add(
                    Member(
                        id = u.id,
                        fullName = u.fullName,
                        avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                        initials = u.fullName.trim().take(1).uppercase(),
                        role = displayRole
                    )
                )
            }

        return membersList
    }

    private fun buildInitialGreeting(context: UserHealthContext, isCurrentUser: Boolean): String {
        val bpSummary = context.latestBloodPressure?.let {
            "${it.valuePrimary.toInt()}/${it.valueSecondary?.toInt()} mmHg"
        } ?: "Chưa có lần đo mới"

        val hrSummary = context.latestHeartRate?.let {
            "${it.valuePrimary.toInt()} BPM"
        } ?: "Chưa có"

        val title = if (isCurrentUser) "bác ${context.fullName}" else "bác ${context.fullName}"
        val diseasesStr = context.chronicDiseases.firstOrNull() ?: "Không có bệnh nền"

        val medCount = context.todayMedicines.size
        val medStr = if (medCount > 0) "$medCount loại thuốc" else "Chưa có đơn thuốc"

        return """
            Dạ kính chào $title! Cháu là **Trợ lý Ảo Sức khỏe FamilyCare**.
            
            Hệ thống vừa cập nhật chỉ số sức khỏe của ${if (isCurrentUser) "bác" else context.fullName} từ hồ sơ:
            • **Huyết áp:** $bpSummary
            • **Nhịp tim:** $hrSummary
            • **Tiền sử cần lưu ý:** $diseasesStr
            • **Lịch thuốc hôm nay:** $medStr
            
            Bác cần cháu tư vấn hay hỗ trợ giải đáp điều gì về sức khỏe hôm nay ạ?
        """.trimIndent()
    }

    private fun generateQuickQuestions(memberName: String, isCurrentUser: Boolean): List<String> {
        return if (isCurrentUser) {
            listOf(
                "📊 Huyết áp hôm nay của tôi thế nào?",
                "💊 Lịch uống thuốc của tôi hôm nay",
                "🩺 Khi nào đến lịch tái khám?",
                "🍵 Tư vấn chế độ ăn giảm muối"
            )
        } else {
            listOf(
                "📊 Huyết áp hôm nay của $memberName thế nào?",
                "💊 Lịch uống thuốc của $memberName",
                "🩺 Lịch tái khám của $memberName",
                "⚠️ Bệnh nền của $memberName cần chú ý gì?"
            )
        }
    }

    companion object {
        /**
         * Factory khởi tạo ViewModel thuận tiện, tự động cấu hình Room Database & Repository
         * Tự động lấy API key từ SessionManager hoặc BuildConfig
         */
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val sessionManager = SessionManager(context.applicationContext)
                val getContextUseCase = GetHealthContextUseCase(
                    userDao = db.userDao(),
                    healthMetricDao = db.healthMetricDao(),
                    medicineDao = db.medicineDao(),
                    medicationLogDao = db.medicationLogDao()
                )
                val aiRepo = HealthAiRepositoryImpl(
                    apiKeyProvider = {
                        sessionManager.geminiApiKey.ifBlank {
                            HealthAiRepositoryImpl.DEFAULT_API_KEY
                        }
                    }
                )
                val sendChatUseCase = SendChatMessageUseCase(aiRepo, getContextUseCase)

                return AiAssistantViewModel(
                    sendChatMessageUseCase = sendChatUseCase,
                    getHealthContextUseCase = getContextUseCase,
                    userDao = db.userDao(),
                    sessionManager = sessionManager
                ) as T
            }
        }
    }
}
