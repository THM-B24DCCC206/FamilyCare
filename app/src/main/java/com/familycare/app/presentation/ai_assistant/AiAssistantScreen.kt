package com.familycare.app.presentation.ai_assistant

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.domain.model.ChatMessage
import com.familycare.app.domain.model.ContextWarningLevel
import com.familycare.app.domain.model.MessageSender
import com.familycare.app.domain.model.UserHealthContext
import com.familycare.app.presentation.home.Member
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiAssistantScreen(
    onNavigateBack: (() -> Unit)? = null,
    onNavigateToVitals: ((memberId: String) -> Unit)? = null,
    onNavigateToSchedule: ((memberId: String) -> Unit)? = null,
    viewModel: AiAssistantViewModel = viewModel(
        factory = AiAssistantViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Launcher nhận diện giọng nói tiếng Việt (Google Speech Recognizer vi-VN)
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setRecordingVoice(false)
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

    val currentMember = uiState.familyMembers.find { it.id == uiState.selectedMemberId }
    val isCurrentMe = currentMember?.role == "Tôi" || currentMember?.initials == "Tôi"
    val memberDisplayName = if (currentMember != null) {
        if (isCurrentMe) "${currentMember.fullName} (Tôi)" else currentMember.fullName
    } else {
        uiState.healthContext?.let { "${it.fullName} (Tôi)" } ?: "Chọn thành viên"
    }

    val bpShort = uiState.healthContext?.latestBloodPressure?.let {
        "${it.valuePrimary.toInt()}/${it.valueSecondary?.toInt()} mmHg"
    } ?: "Chưa đo"
    val hrShort = uiState.healthContext?.latestHeartRate?.let {
        "${it.valuePrimary.toInt()} BPM"
    } ?: "--"
    val healthSummaryText = "HA: $bpShort • $hrShort"

    Scaffold(
        topBar = {
            AiAssistantTopBar(
                onNavigateBack = onNavigateBack,
                selectedMemberDisplayName = memberDisplayName,
                healthSummaryText = healthSummaryText,
                familyMembers = uiState.familyMembers,
                selectedMemberId = uiState.selectedMemberId,
                onMemberSelected = viewModel::onMemberSelected,
                onShowApiKeyDialog = { viewModel.toggleApiKeyDialog(true) },
                onShowContextDialog = { viewModel.toggleContextDialog(true) },
                onClearConversation = viewModel::clearConversation
            )
        },
        bottomBar = {
            AiAssistantBottomBar(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                isRecordingVoice = uiState.isRecordingVoice,
                onTextChanged = viewModel::onInputTextChanged,
                onSendMessage = { viewModel.sendMessage() },
                onStartVoiceInput = {
                    viewModel.setRecordingVoice(true)
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Bác hãy nói câu hỏi về sức khỏe hoặc thuốc...")
                    }
                    try {
                        speechRecognizerLauncher.launch(intent)
                    } catch (_: Exception) {
                        viewModel.setRecordingVoice(false)
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
            // Thanh hiển thị chip gợi ý câu hỏi thông minh dạng LazyRow cuộn ngang
            QuickPromptRow(
                questions = uiState.quickQuestions,
                onQuestionClick = { viewModel.sendMessage(it) }
            )

            // Danh sách tin nhắn hội thoại (LazyColumn)
            val currentName = currentMember?.fullName ?: uiState.healthContext?.fullName ?: "Thành viên"

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                items(uiState.messages, key = { it.id }) { message ->
                    ChatMessageBubble(
                        message = message,
                        currentMemberName = currentName,
                        onActionClick = { actionType, targetMemberId ->
                            val resolvedMemberId = targetMemberId ?: uiState.selectedMemberId
                            when (actionType) {
                                "NAV_VITALS" -> {
                                    if (onNavigateToVitals != null) {
                                        onNavigateToVitals(resolvedMemberId)
                                    } else {
                                        Toast.makeText(context, "Mở màn hình chỉ số sinh tồn", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                "NAV_SCHEDULE" -> {
                                    if (onNavigateToSchedule != null) {
                                        onNavigateToSchedule(resolvedMemberId)
                                    } else {
                                        Toast.makeText(context, "Mở màn hình lịch uống thuốc", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> {
                                    Toast.makeText(context, "Thao tác: $actionType", Toast.LENGTH_SHORT).show()
                                }
                            }
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
                            onOpenApiKeyDialog = { viewModel.toggleApiKeyDialog(true) },
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

    // Dialog cấu hình Google Gemini API Key
    if (uiState.showApiKeyDialog) {
        GeminiApiKeyDialog(
            initialKey = uiState.currentApiKey,
            onDismiss = { viewModel.toggleApiKeyDialog(false) },
            onSave = { newKey ->
                viewModel.saveApiKey(newKey)
                Toast.makeText(context, "Đã lưu Google Gemini API Key thành công!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

/**
 * 1. Thiết Kế Lại TopAppBar Theo Phong Cách Phẳng (Flat & Seamless Header)
 * Gỡ bỏ hoàn toàn Card/Surface bao quanh; tích hợp avatar tròn 38dp, tên thành viên và trạng thái
 * cân đối tự nhiên cùng các nút thao tác.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantTopBar(
    onNavigateBack: (() -> Unit)?,
    selectedMemberDisplayName: String,
    healthSummaryText: String,
    familyMembers: List<Member>,
    selectedMemberId: String,
    onMemberSelected: (String) -> Unit,
    onShowApiKeyDialog: () -> Unit,
    onShowContextDialog: () -> Unit,
    onClearConversation: () -> Unit
) {
    var expandedMemberMenu by remember { mutableStateOf(false) }

    TopAppBar(
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color(0xFF0F172A)
                    )
                }
            }
        },
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { expandedMemberMenu = true }
                        .padding(vertical = 4.dp)
                ) {
                    // Avatar tròn: Kích thước 38.dp
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2563EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Cụm văn bản trong Column(modifier = Modifier.padding(start = 10.dp))
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = selectedMemberDisplayName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Chọn thành viên",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = healthSummaryText,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                DropdownMenu(
                    expanded = expandedMemberMenu,
                    onDismissRequest = { expandedMemberMenu = false }
                ) {
                    familyMembers.forEach { member ->
                        val isSelected = member.id == selectedMemberId
                        val isMe = member.role == "Tôi" || member.initials == "Tôi"
                        val label = if (isMe) "${member.fullName} (Tôi)" else member.fullName
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF2563EB) else Color(0xFF0F172A),
                                        fontSize = 14.sp
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color(0xFF2563EB),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            onClick = {
                                expandedMemberMenu = false
                                onMemberSelected(member.id)
                            }
                        )
                    }
                }
            }
        },
        actions = {
            // Nút cấu hình Google Gemini API Key
            IconButton(onClick = onShowApiKeyDialog) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = "Cấu hình Google Gemini API Key",
                    tint = Color(0xFF2563EB)
                )
            }
            // Nút xem chi tiết Context Injection
            IconButton(onClick = onShowContextDialog) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Xem ngữ cảnh y tế đã tiêm",
                    tint = Color(0xFF2563EB)
                )
            }
            // Nút làm mới đoạn hội thoại
            IconButton(onClick = onClearConversation) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Bắt đầu lại cuộc hội thoại",
                    tint = Color(0xFF64748B)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

/**
 * 2. Thu Gọn Thanh Nhập Liệu Chân Trang (Slim & Minimalist Input Bar)
 * Chiều cao thanh mảnh 40-44dp, BasicTextField bo góc mềm 22dp, nút micro 38dp, nút gửi đổi màu xanh.
 */
@Composable
fun AiAssistantBottomBar(
    inputText: String,
    isLoading: Boolean,
    isRecordingVoice: Boolean,
    onTextChanged: (String) -> Unit,
    onSendMessage: () -> Unit,
    onStartVoiceInput: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Viền siêu mỏng phía trên
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFF1F5F9))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Nút Micro thu âm 38.dp (icon 20.dp)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isRecordingVoice) Color(0xFFDC2626) else Color(0xFFEFF6FF))
                    .clickable(enabled = !isLoading, onClick = onStartVoiceInput),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Ghi âm giọng nói",
                    tint = if (isRecordingVoice) Color.White else Color(0xFF2563EB),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Ô nhập văn bản thon gọn (chiều cao 38-42dp ở 1 dòng, tối đa 3 dòng)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 38.dp, max = 84.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFF8FAFC))
                    .border(BorderStroke(1.dp, Color(0xFFE2E8F0)), RoundedCornerShape(22.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (inputText.isEmpty()) {
                    Text(
                        text = "Hỏi về huyết áp, thuốc...",
                        fontSize = 14.sp,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    enabled = !isLoading,
                    maxLines = 3,
                    textStyle = TextStyle(
                        fontSize = 14.sp,
                        color = Color(0xFF0F172A)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Nút gửi tin nhắn (38.dp)
            val hasText = inputText.isNotBlank()
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (hasText) Color(0xFF2563EB) else Color(0xFFF1F5F9))
                    .clickable(
                        enabled = hasText && !isLoading,
                        onClick = onSendMessage
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi tin nhắn",
                    tint = if (hasText) Color.White else Color(0xFF94A3B8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * Hàng gợi ý câu hỏi thông minh dạng LazyRow cuộn ngang với padding chuẩn
 */
@Composable
fun QuickPromptRow(
    questions: List<String>,
    onQuestionClick: (String) -> Unit
) {
    if (questions.isEmpty()) return
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentPadding = PaddingValues(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(questions) { prompt ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFEFF6FF),
                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onQuestionClick(prompt) }
            ) {
                Text(
                    text = prompt,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1D4ED8),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * 3. Parser Markdown đơn giản để tự động in đậm các cụm từ bọc trong **...**
 */
fun parseMarkdownToAnnotatedString(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        parts.forEachIndexed { index, part ->
            if (index % 2 == 1) {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(part)
                }
            } else {
                append(part)
            }
        }
    }
}

/**
 * Bong bóng tin nhắn tùy biến, phân biệt rõ người dùng và AI
 */
@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    currentMemberName: String,
    onActionClick: (actionType: String, targetMemberId: String?) -> Unit
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
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.95f)
        ) {
            // Avatar AI
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = "AI",
                        tint = Color(0xFF2563EB),
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
                    Color(0xFF2563EB)
                } else {
                    Color(0xFFF8FAFC)
                },
                border = if (!isUser) BorderStroke(1.dp, Color(0xFFE2E8F0)) else null,
                tonalElevation = if (isUser) 2.dp else 1.dp,
                modifier = Modifier.widthIn(min = 60.dp, max = 340.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Tag nhỏ trên đầu tin nhắn AI: Tư vấn cho: [Tên thành viên]
                    if (!isUser) {
                        val target = message.targetMemberName ?: currentMemberName
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEFF6FF),
                            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF1D4ED8),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Tư vấn cho: $target",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1D4ED8),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Cảnh báo mức độ nguy hiểm nếu có
                    if (!isUser && message.warningLevel != ContextWarningLevel.NORMAL) {
                        WarningLevelBanner(warningLevel = message.warningLevel)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Nội dung tin nhắn chính với AnnotatedString in đậm Markdown
                    if (isUser) {
                        Text(
                            text = message.text,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 23.sp,
                                fontSize = 15.sp
                            )
                        )
                    } else {
                        Text(
                            text = parseMarkdownToAnnotatedString(message.text),
                            color = Color(0xFF0F172A),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                lineHeight = 23.sp,
                                fontSize = 15.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Thời gian gửi tin
                    Text(
                        text = formatTime(message.timestamp),
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else Color(0xFF94A3B8),
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
                        .background(Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "User",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Dưới câu trả lời của AI: Nút tắt thao tác nhanh dẫn thẳng vào dữ liệu của thành viên đó
        if (!isUser) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .padding(start = 40.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onActionClick("NAV_VITALS", message.targetMemberId) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🩺 Đo chỉ số",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1D4ED8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onActionClick("NAV_SCHEDULE", message.targetMemberId) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "💊 Xem lịch thuốc",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF15803D)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hiệu ứng đang suy nghĩ của AI (Loading State)
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
                .background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = Color(0xFF2563EB),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bác sĩ ảo đang phân tích chỉ số sức khỏe...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF475569).copy(alpha = alphaAnim),
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(8.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF2563EB)
                )
            }
        }
    }
}

/**
 * Khung báo lỗi và hỗ trợ Thử lại / Nhập API Key (Error State)
 */
@Composable
fun ErrorMessageCard(
    errorMessage: String,
    onRetry: () -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onDismiss: () -> Unit
) {
    val isApiKeyError = errorMessage.contains("API Key", ignoreCase = true) ||
            errorMessage.contains("chìa khóa", ignoreCase = true)

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
                    text = if (isApiKeyError) "Chưa cấu hình API Key" else "Không thể gửi tin nhắn",
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

            if (isApiKeyError) {
                Button(
                    onClick = onOpenApiKeyDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("🔑 Nhập Key", fontSize = 12.sp)
                }
            } else {
                FilledTonalButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("Thử lại", fontSize = 12.sp)
                }
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
 * Hộp thoại cho phép người dùng nhập trực tiếp Google Gemini API Key
 */
@Composable
fun GeminiApiKeyDialog(
    initialKey: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(initialKey) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Google Gemini API Key",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Dùng cho Trợ lý AI & Quét đơn thuốc",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Text(
                    text = "Hãy nhập hoặc dán khóa Google Gemini API Key của bạn vào đây (bắt đầu bằng AIzaSy...):",
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("Dán API Key (AIzaSy...)", fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Ẩn" else "Hiện",
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                )

                Text(
                    text = "💡 Lưu ý: Bạn có thể lấy khóa API hoàn toàn miễn phí tại trang Google AI Studio: https://aistudio.google.com/app/apikey",
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Hủy", color = Color(0xFF64748B))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(apiKeyInput.trim()) },
                        enabled = apiKeyInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lưu Khóa API")
                    }
                }
            }
        }
    }
}

/**
 * Hộp thoại hiển thị thông tin Context Injection 100% từ Room Database
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
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ngữ Cảnh Được Tiêm (100% Room DB)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dữ liệu thực tế từ thiết bị và bệnh án được tiêm vào System Instructions của Gemini:",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "1. THÀNH VIÊN TRỌNG TÂM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ContextInfoRow("Họ tên:", "${context.fullName} (${context.getDisplayAge()} tuổi)")
                        ContextInfoRow("Nhóm máu:", context.bloodType.ifBlank { "Chưa xác định" })
                        ContextInfoRow("Bệnh nền:", if (context.chronicDiseases.isEmpty()) "Không có" else context.chronicDiseases.joinToString(", "))
                        ContextInfoRow("Dị ứng:", if (context.drugAllergies.isEmpty()) "Không có" else context.drugAllergies.joinToString(", "))
                        ContextInfoRow("Huyết áp:", context.getBloodPressureSummary())
                        ContextInfoRow("Nhịp tim:", context.getHeartRateSummary())

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Thuốc hôm nay (${context.todayMedicines.size}):",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                        if (context.todayMedicines.isEmpty()) {
                            Text("Chưa có thuốc hôm nay", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        } else {
                            context.todayMedicines.forEach { m ->
                                Text("• ${m.name} (${m.dosage}) - ${m.scheduleStatus}", fontSize = 11.sp, color = Color(0xFF475569))
                            }
                        }

                        if (context.otherFamilyMembers.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "2. TỔ ẤM GIA ĐÌNH (${context.otherFamilyMembers.size} người)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            context.otherFamilyMembers.forEach { other ->
                                Text("• ${other.fullName} (${other.age}t): HA ${other.bloodPressureSummary}, Tim ${other.heartRateSummary}", fontSize = 11.sp, color = Color(0xFF334155))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
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
            color = Color(0xFF2563EB)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF334155)
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
