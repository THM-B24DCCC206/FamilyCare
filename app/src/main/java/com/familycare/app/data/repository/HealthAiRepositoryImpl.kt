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
 * Triển khai HealthAiRepository sử dụng Google AI Client SDK với mô hình Gemini Flash thế hệ mới.
 * Hỗ trợ Context Injection động vào System Instructions và cơ chế tự động chuyển đổi model dự phòng.
 */
class HealthAiRepositoryImpl(
    private val apiKeyProvider: () -> String = { DEFAULT_API_KEY }
) : HealthAiRepository {

    override suspend fun sendMessage(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage>
    ): Resource<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            return@withContext Resource.Error("Chưa cấu hình API Key của Google Gemini. Vui lòng kiểm tra lại thiết lập.")
        }

        var lastException: Exception? = null
        val chatHistory = convertHistoryToGenerativeAiContent(history)

        for (modelName in CANDIDATE_MODELS) {
            try {
                val generativeModel = createGenerativeModel(apiKey, context, modelName)
                val chat = generativeModel.startChat(chatHistory)
                val response = chat.sendMessage(userPrompt)

                val replyText = response.text
                if (!replyText.isNullOrBlank()) {
                    return@withContext Resource.Success(replyText.trim())
                }
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                val isRetryable = errorMsg.contains("not found", ignoreCase = true) ||
                        errorMsg.contains("503", ignoreCase = true) ||
                        errorMsg.contains("UNAVAILABLE", ignoreCase = true) ||
                        errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        errorMsg.contains("MissingFieldException", ignoreCase = true)
                if (!isRetryable) {
                    break
                }
            }
        }

        Resource.Error(formatErrorMessage(lastException ?: Exception("Không thể kết nối đến mô hình AI")))
    }

    override fun sendMessageStream(
        userPrompt: String,
        context: UserHealthContext,
        history: List<ChatMessage>
    ): Flow<Resource<String>> = flow {
        val apiKey = apiKeyProvider().trim()
        if (apiKey.isBlank() || apiKey == "YOUR_GEMINI_API_KEY") {
            emit(Resource.Error("Chưa cấu hình API Key của Google Gemini."))
            return@flow
        }

        emit(Resource.Loading())
        val chatHistory = convertHistoryToGenerativeAiContent(history)
        var streamedSuccess = false
        var lastException: Exception? = null

        for (modelName in CANDIDATE_MODELS) {
            try {
                val generativeModel = createGenerativeModel(apiKey, context, modelName)
                val chat = generativeModel.startChat(chatHistory)
                val responseStream = chat.sendMessageStream(userPrompt)

                val fullResponseBuilder = StringBuilder()
                responseStream.collect { chunk ->
                    chunk.text?.let { text ->
                        fullResponseBuilder.append(text)
                        emit(Resource.Success(fullResponseBuilder.toString()))
                    }
                }
                streamedSuccess = true
                break
            } catch (e: Exception) {
                lastException = e
                val errorMsg = e.message ?: ""
                val isRetryable = errorMsg.contains("not found", ignoreCase = true) ||
                        errorMsg.contains("503", ignoreCase = true) ||
                        errorMsg.contains("UNAVAILABLE", ignoreCase = true) ||
                        errorMsg.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
                        errorMsg.contains("MissingFieldException", ignoreCase = true)
                if (!isRetryable) {
                    break
                }
            }
        }

        if (!streamedSuccess) {
            emit(Resource.Error(formatErrorMessage(lastException ?: Exception("Không thể kết nối đến mô hình AI"))))
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Tạo đối tượng GenerativeModel với System Instructions được đính kèm Ngữ cảnh sức khỏe
     */
    private fun createGenerativeModel(
        apiKey: String,
        healthContext: UserHealthContext,
        modelName: String
    ): GenerativeModel {
        val config = generationConfig {
            temperature = 0.3f
            topK = 40
            topP = 0.9f
            maxOutputTokens = 1200
        }

        val safetySettings = listOf(
            SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.LOW_AND_ABOVE),
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.LOW_AND_ABOVE)
        )

        val systemInstructionContent = content {
            text(healthContext.buildSystemInstruction())
        }

        return GenerativeModel(
            modelName = modelName,
            apiKey = apiKey,
            generationConfig = config,
            safetySettings = safetySettings,
            systemInstruction = systemInstructionContent
        )
    }

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
            msg.contains("503", ignoreCase = true) || msg.contains("UNAVAILABLE", ignoreCase = true) ->
                "Hệ thống máy chủ AI đang chịu tải cao, vui lòng bấm 'Thử lại' sau giây lát."
            msg.contains("MissingFieldException", ignoreCase = true) || msg.contains("not found", ignoreCase = true) ->
                "Máy chủ Google Gemini đang cập nhật phiên bản model. Vui lòng bấm 'Thử lại'."
            else -> "Đã xảy ra lỗi khi trao đổi với AI. Vui lòng bấm 'Thử lại'."
        }
    }

    companion object {
        val DEFAULT_API_KEY: String = com.familycare.app.BuildConfig.GEMINI_API_KEY.ifBlank {
            System.getenv("GEMINI_API_KEY") ?: ""
        }

        val CANDIDATE_MODELS = listOf(
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash-lite",
            "gemini-3.8-flash",
            "gemini-flash-latest"
        )
    }
}
