package com.familycare.app.presentation.ai_assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.ContextWarningLevel
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.model.UserHealthContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantScreen(
    viewModel: AiAssistantViewModel = viewModel(
        factory = AiAssistantViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Voice-to-Text Speech Recognition Launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
            if (!spokenText.isNullOrBlank()) {
                viewModel.onVoiceInputResult(spokenText)
                Toast.makeText(context, "Đã nhận diện: \"$spokenText\"", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cuộn xuống tin nhắn mới nhất mỗi khi danh sách có cập nhật hoặc AI đang trả lời
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "AI Health Copilot",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Badge hiển thị tóm tắt ngữ cảnh y tế của người bệnh đang được bảo vệ
                        uiState.healthContext?.let { ctx ->
                            Text(
                                text = "${ctx.fullName} (${ctx.getDisplayAge()} tuổi) • Huyết áp: ${ctx.latestBloodPressure?.let { "${it.valuePrimary.toInt()}/${it.valueSecondary?.toInt()}" } ?: "--"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Nút bấm mở Dialog xem chi tiết Context Injection
                    IconButton(onClick = { viewModel.toggleContextDialog(true) }) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Xem ngữ cảnh y tế đã tiêm",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Nút làm mới đoạn hội thoại
                    IconButton(onClick = { viewModel.clearConversation() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Bắt đầu lại cuộc hội thoại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            ChatBottomInputBar(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                onTextChanged = viewModel::onInputTextChanged,
                onSendMessage = { viewModel.sendMessage() },
                onStartVoiceInput = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Bác hãy nói câu hỏi về sức khỏe hoặc thuốc...")
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, "Thiết bị không hỗ trợ nhận diện giọng nói", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Thanh hiển thị nhanh chip gợi ý câu hỏi y tế
            if (uiState.messages.size <= 2) {
                QuickQuestionsBar(
                    questions = uiState.quickQuestions,
                    onQuestionSelected = { viewModel.sendMessage(it) }
                )
            }

            // Danh sách tin nhắn hội thoại (LazyColumn)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        onActionClick = { actionText ->
                            Toast.makeText(context, "Đã chọn: $actionText", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // Trạng thái AI đang suy nghĩ / trả lời (Loading State)
                if (uiState.isLoading) {
                    item {
                        AiTypingIndicator()
                    }
                }

                // Trạng thái Báo lỗi và Nút Thử lại (Error State)
                if (uiState.errorMessage != null && !uiState.isLoading) {
                    item {
                        ErrorMessageCard(
                            errorMessage = uiState.errorMessage!!,
                            onRetry = { viewModel.retryLastMessage() },
                            onDismiss = { viewModel.clearError() }
                        )
                    }
                }
            }
        }
    }

    // Dialog giải thích chi tiết cơ chế Context Injection từ Room Database
    if (uiState.showContextDialog && uiState.healthContext != null) {
        ContextInjectionDialog(
            context = uiState.healthContext!!,
            onDismiss = { viewModel.toggleContextDialog(false) }
        )
    }
}

/**
 * Bong bóng tin nhắn tùy biến, phân biệt rõ người dùng và AI
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    onActionClick: (String) -> Unit
) {
    val isUser = message.sender == MessageSender.USER
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(if (isUser) 0.92f else 0.95f)
        ) {
            // Avatar AI
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            // Hộp nội dung tin nhắn
            Surface(
                shape = if (isUser) {
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp)
                } else {
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 4.dp, bottomEnd = 18.dp)
                },
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = if (isUser) 2.dp else 1.dp,
                modifier = Modifier.widthIn(min = 60.dp, max = 320.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Cảnh báo mức độ nguy hiểm nếu có
                    if (!isUser && message.warningLevel != ContextWarningLevel.NORMAL) {
                        WarningLevelBanner(warningLevel = message.warningLevel)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Nội dung tin nhắn chính với cỡ chữ dễ nhìn cho người lớn tuổi
                    Text(
                        text = message.text,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            lineHeight = 24.sp,
                            fontSize = 16.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Thời gian gửi tin
                    Text(
                        text = formatTime(message.timestamp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }

            // Avatar Người dùng
            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Các nút hành động gợi ý phía dưới phản hồi của AI
        if (!isUser && message.suggestedActions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .padding(start = 40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.suggestedActions.forEach { action ->
                    AssistChip(
                        onClick = { onActionClick(action) },
                        label = { Text(action, fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Hiệu ứng đang gõ / đang suy nghĩ của AI (Loading State)
 */
@Composable
fun AiTypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bác sĩ ảo đang phân tích chỉ số sức khỏe...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alphaAnim),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Khung báo lỗi và hỗ trợ Thử lại (Error State)
 */
@Composable
fun ErrorMessageCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.ErrorOutline,
                contentDescription = "Lỗi",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Không thể gửi tin nhắn",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            FilledTonalButton(
                onClick = onRetry,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text("Thử lại", fontSize = 13.sp)
            }
        }
    }
}

/**
 * Thanh gợi ý câu hỏi nhanh nằm phía trên
 */
@Composable
fun QuickQuestionsBar(
    questions: List<String>,
    onQuestionSelected: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(questions) { q ->
            SuggestionChip(
                onClick = { onQuestionSelected(q) },
                label = { Text(q, fontSize = 13.sp, maxLines = 1) },
                icon = {
                    Icon(
                        Icons.Default.QuestionAnswer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            )
        }
    }
}

/**
 * Thanh nhập liệu dưới cùng gồm Ô nhập văn bản, Nút Gửi và Nút Ghi âm giọng nói (Voice-to-Text)
 */
@Composable
fun ChatBottomInputBar(
    inputText: String,
    isLoading: Boolean,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStartVoiceInput: () -> Unit
) {
    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Micro thu âm giọng nói (Voice-to-Text)
            IconButton(
                onClick = onStartVoiceInput,
                enabled = !isLoading,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Ghi âm giọng nói",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Ô nhập văn bản hỗ trợ nhiều dòng
            TextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = {
                    Text(
                        text = "Hỏi về huyết áp, cách dùng thuốc...",
                        fontSize = 14.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp, max = 120.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading,
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Nút gửi tin nhắn
            val canSend = inputText.isNotBlank() && !isLoading
            FilledIconButton(
                onClick = onSendMessage,
                enabled = canSend,
                modifier = Modifier.size(44.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.outline
                )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi tin nhắn"
                )
            }
        }
    }
}

/**
 * Huy hiệu cảnh báo lâm sàng khẩn cấp hoặc chú ý
 */
@Composable
fun WarningLevelBanner(warningLevel: ContextWarningLevel) {
    val (bgColor, textColor, icon, label) = when (warningLevel) {
        ContextWarningLevel.CRITICAL -> Quadruple(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.onError,
            Icons.Default.Warning,
            "CẢNH BÁO NGUY HIỂM - GỌI 115 HOẶC BẤM SOS NGAY"
        )
        ContextWarningLevel.ATTENTION -> Quadruple(
            Color(0xFFE65100),
            Color.White,
            Icons.Default.PriorityHigh,
            "LƯU Ý ĐẶC BIỆT THEO TIỀN SỬ BỆNH"
        )
        ContextWarningLevel.NORMAL -> Quadruple(
            Color.Transparent, Color.Transparent, Icons.Default.Info, ""
        )
    }

    if (warningLevel != ContextWarningLevel.NORMAL) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Hộp thoại hiển thị thông tin Context Injection từ Room Database
 */
@Composable
fun ContextInjectionDialog(
    context: UserHealthContext,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ngữ cảnh được tiêm (Context Injection)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Trước khi gửi yêu cầu lên Gemini 1.5 Flash, hệ thống tự động đọc Room Database và tiêm dữ liệu sau vào System Instructions:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        ContextInfoRow("Họ tên:", context.fullName)
                        ContextInfoRow("Độ tuổi:", "${context.getDisplayAge()} tuổi")
                        ContextInfoRow("Nhóm máu:", context.bloodType.ifBlank { "O" })
                        ContextInfoRow("Bệnh nền:", context.chronicDiseases.joinToString(", "))
                        ContextInfoRow("Dị ứng thuốc:", context.drugAllergies.joinToString(", "))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        ContextInfoRow("Huyết áp mới nhất:", context.getBloodPressureSummary())
                        ContextInfoRow("Nhịp tim mới nhất:", context.getHeartRateSummary())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}

@Composable
fun ContextInfoRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
