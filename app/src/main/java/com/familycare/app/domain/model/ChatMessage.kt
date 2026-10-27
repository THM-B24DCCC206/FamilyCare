package com.familycare.app.domain.model

import java.util.UUID

enum class MessageSender {
    USER,
    AI
}

enum class ContextWarningLevel {
    NORMAL,     // Chỉ số bình thường
    ATTENTION,  // Cần chú ý (tiền tăng huyết áp, nhịp tim hơi nhanh)
    CRITICAL    // Khẩn cấp (cơn tăng huyết áp cấp cứu, nhịp tim nguy hiểm)
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val warningLevel: ContextWarningLevel = ContextWarningLevel.NORMAL,
    val suggestedActions: List<String> = emptyList(),
    val targetMemberName: String? = null,
    val targetMemberId: String? = null
)
