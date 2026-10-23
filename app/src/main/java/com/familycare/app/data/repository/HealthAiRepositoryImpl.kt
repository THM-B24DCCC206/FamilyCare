package com.familycare.app.data.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.model.UserHealthContext
import com.familycare.app.domain.repository.HealthAiRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Triển khai HealthAiRepository sử dụng Google AI Client SDK với mô hình Gemini 1.5 Flash.
 * Hỗ trợ Context Injection động vào System Instructions trước mỗi lượt hội thoại.
 */
class HealthAiRepositoryImpl(
    private val apiKeyProvider: () -> String = { DEFAULT_API_KEY }
) : HealthAiRepository {

    override suspend fun sendMessage(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage>
    ): Resource<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext Resource.Error("Chưa cấu hình API Key của Google Gemini. Vui lòng kiểm tra lại thiết lập.")
        }

        try {
            val generativeModel = createGenerativeModel(apiKey, context)
            val chatHistory = convertHistoryToGenerativeAiContent(history)

            val chat = generativeModel.startChat(chatHistory)
            val response = chat.sendMessage(userPrompt)

            val replyText = response.text
            if (!replyText.isNullOrBlank()) {
                Resource.Success(replyText.trim())
            } else {
                Resource.Error("Gemini không trả về nội dung phản hồi. Vui lòng thử lại.")
            }
        } catch (e: Exception) {
            Resource.Error(formatErrorMessage(e))
        }
    }

    override fun sendMessageStream(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage>
    ): Flow<Resource<String>> = flow {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            emit(Resource.Error("Chưa cấu hình API Key của Google Gemini."))
            return@flow
        }

        try {
            emit(Resource.Loading())
            val generativeModel = createGenerativeModel(apiKey, context)
            val chatHistory = convertHistoryToGenerativeAiContent(history)

            val chat = generativeModel.startChat(chatHistory)
            val responseStream = chat.sendMessageStream(userPrompt)

            val fullResponseBuilder = StringBuilder()
            responseStream.collect { chunk ->
                chunk.text?.let { text ->
                    fullResponseBuilder.append(text)
                    emit(Resource.Success(fullResponseBuilder.toString()))
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(formatErrorMessage(e)))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Tạo đối tượng GenerativeModel với System Instructions được đính kèm Ngữ cảnh sức khỏe
     * (CONTEXT INJECTION THEO YÊU CẦU ĐỒ ÁN)
     */
    private fun createGenerativeModel(
        apiKey: String,
        healthContext: UserHealthContext
    ): GenerativeModel {
        // Cấu hình sinh văn bản an toàn và thận trọng cho lĩnh vực y tế gia đình
        val config = generationConfig {
            temperature = 0.3f       // Nhiệt độ thấp giúp phản hồi chính xác, hạn chế ảo giác y khoa
            topK = 40
            topP = 0.9f
            maxOutputTokens = 1200   // Đủ dung lượng giải thích chi tiết và ân cần
        }

        // Thiết lập bộ lọc an toàn cao để bảo vệ người dùng cao tuổi
        val safetySettings = listOf(
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.LOW_AND_ABOVE),
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.LOW_AND_ABOVE)
        )

        // Tiêm ngữ cảnh bệnh án & chỉ số sinh tồn vào System Instruction
        val systemInstructionContent = content {
            text(healthContext.buildSystemInstruction())
        }

        return GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
            systemInstruction = systemInstructionContent
        )
    }

    /**
     * Chuyển đổi lịch sử chat của Domain Model sang đối tượng Content của Google AI Client SDK
     */
    private fun convertHistoryToGenerativeAiContent(history: List<ChatMessage>): List<Content> {
        return history
            .filter { !it.isError && it.text.isNotBlank() }
            .map { message ->
                val role = when (message.sender) {
                    MessageSender.USER -> "user"
                    MessageSender.AI -> "model"
                }
                content(role = role) {
                    text(message.text)
                }
            }
    }

    /**
     * Chuyển đổi ngoại lệ thành thông báo lỗi tiếng Việt thân thiện
     */
    private fun formatErrorMessage(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("API_KEY_INVALID", ignoreCase = true) ->
                "Khóa API Gemini không hợp lệ. Vui lòng kiểm tra lại cấu hình."
            msg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) || msg.contains("Quota", ignoreCase = true) ->
                "Đã vượt quá giới hạn lượt gọi Gemini trong ngày. Vui lòng chờ ít phút rồi thử lại."
            msg.contains("ConnectException", ignoreCase = true) || msg.contains("UnknownHost", ignoreCase = true) ->
                "Không có kết nối mạng Internet. Vui lòng bật Wi-Fi hoặc 4G để trò chuyện với Bác sĩ ảo."
            msg.contains("SocketTimeout", ignoreCase = true) ->
                "Kết nối tới máy chủ Gemini bị gián đoạn do mạng chậm. Vui lòng bấm thử lại."
            else -> "Đã xảy ra lỗi khi trao đổi với AI: ${e.localizedMessage ?: "Vui lòng thử lại"}"
        }
    }

    companion object {
        // Khóa Gemini API cấu hình qua biến môi trường hoặc truyền trực tiếp vào constructor
        val DEFAULT_API_KEY: String = System.getenv("GEMINI_API_KEY") ?: ""
    }
}
