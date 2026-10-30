package com.familycare.app.presentation.biometrics

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.presentation.biometrics.chart.BiometricsLineChart
import com.familycare.app.presentation.biometrics.dialog.AddMetricDialog
import com.familycare.app.presentation.biometrics.ppg.HeartRateClassifier
import com.familycare.app.presentation.schedule.MemberSwitcherRow
import com.familycare.app.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BiometricsScreen(
    onNavigateToCameraPpg: () -> Unit,
    onNavigateToSmartwatch: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val sessionManager = remember { SessionManager(context) }
    val defaultUserId = remember(sessionManager.userId) { sessionManager.userId.ifEmpty { "user_me" } }
    var selectedMemberId by remember { mutableStateOf(defaultUserId) }
    var familyMembers by remember { mutableStateOf<List<com.familycare.app.presentation.home.Member>>(emptyList()) }

    LaunchedEffect(sessionManager.userId, sessionManager.familyId) {
        val fakeIds = setOf("user_01", "user_demo_01", "member_bac_nam", "member_bo", "member_me", "member_anhtrai", "user_02", "member_ly_tran")
        val allUsers = database.userDao.getAllUsers()
        val currentDbUser = allUsers.firstOrNull { it.id == defaultUserId }
        val currentFamilyId = currentDbUser?.familyId?.trim().orEmpty().ifBlank { sessionManager.familyId.trim() }

        val membersList = mutableListOf<com.familycare.app.presentation.home.Member>()

        // 1. Luôn đưa tài khoản đang đăng nhập lên ĐẦU TIÊN
        val myName = currentDbUser?.fullName ?: sessionManager.userName.ifBlank { "Tôi" }
        val myInitials = myName.trim().split("\\s+".toRegex())
            .takeLast(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { myName.take(1).uppercase() }

        membersList.add(
            com.familycare.app.presentation.home.Member(
                id = defaultUserId,
                fullName = myName,
                role = if (currentDbUser?.role?.uppercase() == "ADMIN" || sessionManager.userRole == "ADMIN") "Trưởng gia đình" else "Thành viên",
                initials = myInitials
            )
        )

        // 2. Thêm các thành viên CÙNG familyId với tài khoản hiện tại
        if (currentFamilyId.isNotBlank()) {
            allUsers.filter {
                it.id != defaultUserId &&
                it.id !in fakeIds &&
                it.familyId.isNotBlank() &&
                it.familyId == currentFamilyId
            }.forEach { u ->
                val initials = u.fullName.trim().split("\\s+".toRegex())
                    .takeLast(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifBlank { u.fullName.take(1).uppercase() }
                membersList.add(
                    com.familycare.app.presentation.home.Member(
                        id = u.id,
                        fullName = u.fullName,
                        role = if (u.role.uppercase() == "ADMIN") "Trưởng gia đình" else "Thành viên",
                        initials = initials
                    )
                )
            }
        }

        familyMembers = membersList
        if (selectedMemberId.isEmpty() || familyMembers.none { it.id == selectedMemberId }) {
            selectedMemberId = defaultUserId
        }
    }

    val currentUserId = selectedMemberId
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val metricsList by database.healthMetricDao.getMetricsByMember(currentUserId)
        .collectAsState(initial = emptyList())

    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(MetricType.BLOOD_PRESSURE) }

    // State giọng nói truyền vào AddMetricDialog
    var voiceSystolic by remember { mutableStateOf<String?>(null) }
    var voiceDiastolic by remember { mutableStateOf<String?>(null) }
    var voicePulse by remember { mutableStateOf<String?>(null) }
    var voiceBloodSugar by remember { mutableStateOf<String?>(null) }
    var voiceTemp by remember { mutableStateOf<String?>(null) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                val parsed = parseVoiceMetrics(spoken)
                voiceSystolic = parsed.systolic
                voiceDiastolic = parsed.diastolic
                voicePulse = parsed.pulse
                voiceBloodSugar = parsed.bloodSugar
                voiceTemp = parsed.temperature
                dialogInitialType = parsed.metricType
                showAddDialog = true

                scope.launch {
                    snackbarHostState.showSnackbar("Đã nhận diện: \"$spoken\"")
                }
            }
        }
    }

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            scope.launch {
                snackbarHostState.showSnackbar("Thiết bị chưa cài đặt hoặc không hỗ trợ nhận diện giọng nói Google.")
            }
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Hãy đọc chỉ số (Ví dụ: Huyết áp 120 trên 80 nhịp tim 75)")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            scope.launch {
                snackbarHostState.showSnackbar("Không thể mở micro: ${e.localizedMessage}")
            }
        }
    }

    // Dialog xem chi tiết và xóa
    var metricDetailToView by remember { mutableStateOf<HealthMetricEntity?>(null) }
    var metricToDelete by remember { mutableStateOf<HealthMetricEntity?>(null) }

    // DatePicker state
    var selectedDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Bộ lọc danh sách lịch sử theo loại chỉ số
    var historyFilter by remember { mutableStateOf("ALL") }

    val filteredHistory = remember(metricsList, historyFilter) {
        if (historyFilter == "ALL") metricsList
        else metricsList.filter { it.type == historyFilter }
    }

    // Lọc danh sách đo thuộc về ngày được chọn (00:00:00 -> 23:59:59.999 của ngày đã chọn)
    val metricsForSelectedDay = remember(filteredHistory, selectedDateMillis) {
        val targetCal = Calendar.getInstance().apply {
            timeInMillis = selectedDateMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = targetCal.timeInMillis
        targetCal.set(Calendar.HOUR_OF_DAY, 23)
        targetCal.set(Calendar.MINUTE, 59)
        targetCal.set(Calendar.SECOND, 59)
        targetCal.set(Calendar.MILLISECOND, 999)
        val endOfDay = targetCal.timeInMillis

        filteredHistory.filter { metric ->
            metric.timestamp in startOfDay..endOfDay
        }
    }

    val selectedDateHeader = remember(selectedDateMillis) {
        formatSelectedDateHeader(selectedDateMillis)
    }

    val isTodaySelected = remember(selectedDateMillis) {
        val calTarget = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val calNow = Calendar.getInstance()
        calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chỉ Số Sinh Tồn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color(0xFF0F172A)
                ),
                actions = {
                    Surface(
                        shape = CircleShape,
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable {
                                voiceSystolic = null
                                voiceDiastolic = null
                                voicePulse = null
                                voiceBloodSugar = null
                                voiceTemp = null
                                dialogInitialType = MetricType.BLOOD_PRESSURE
                                showAddDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Nhập chỉ số thủ công",
                                tint = Color(0xFF0F172A),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
        ) {
            // 0. Bộ chọn thành viên trong gia đình (chuẩn phong cách Kế hoạch sức khỏe)
            if (familyMembers.isNotEmpty()) {
                item {
                    MemberSwitcherRow(
                        members = familyMembers,
                        selectedMemberId = selectedMemberId,
                        onSelectMember = { memberId ->
                            selectedMemberId = memberId
                        }
                    )
                }
            }

            // 1. Banner Đo Nhịp Tim Quang Học (PPG) tinh tế, sang trọng
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFF1F2)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Đo nhịp tim (PPG)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đo nhanh qua Camera",
                                    fontSize = 12.5.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = onNavigateToCameraPpg,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Bắt đầu đo",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 2. Khu vực nhập chỉ số lâm sàng thủ công & Nhập bằng giọng nói (Voice Input)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Nhập chỉ số lâm sàng ngoài máy",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // Nút Micro nhập giọng nói thông minh
                    Surface(
                        onClick = { startVoiceInput() },
                        shape = RoundedCornerShape(18.dp),
                        color = WarmPrimaryLight,
                        border = BorderStroke(1.dp, WarmPrimaryTeal.copy(alpha = 0.3f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(WarmPrimaryTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Nói để nhập",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎙️ Nói để nhập",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmPrimaryTeal
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đọc: \"Huyết áp 120 trên 80, tim 75\"",
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = WarmTextSecondary
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = WarmPrimaryTeal
                            )
                        }
                    }

                    // 3 phím tắt Huyết Áp, Đường Huyết, Thân Nhiệt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionCard(
                            title = "+ Huyết Áp",
                            subtitle = "Máy bắp tay",
                            icon = Icons.Default.Speed,
                            iconColor = Color(0xFFE53935),
                            onClick = {
                                voiceSystolic = null
                                voiceDiastolic = null
                                voicePulse = null
                                voiceBloodSugar = null
                                voiceTemp = null
                                dialogInitialType = MetricType.BLOOD_PRESSURE
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = "+ Đường Huyết",
                            subtitle = "Que thử máu",
                            icon = Icons.Default.WaterDrop,
                            iconColor = Color(0xFFFB8C00),
                            onClick = {
                                voiceSystolic = null
                                voiceDiastolic = null
                                voicePulse = null
                                voiceBloodSugar = null
                                voiceTemp = null
                                dialogInitialType = MetricType.BLOOD_SUGAR
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionCard(
                            title = "+ Thân Nhiệt",
                            subtitle = "Nhiệt kế",
                            icon = Icons.Default.Thermostat,
                            iconColor = Color(0xFF0288D1),
                            onClick = {
                                voiceSystolic = null
                                voiceDiastolic = null
                                voicePulse = null
                                voiceBloodSugar = null
                                voiceTemp = null
                                dialogInitialType = MetricType.TEMPERATURE
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. BIỂU ĐỒ ĐƯỜNG XU HƯỚNG TỔNG HỢP (LINE CHART CANVAS)
            item {
                BiometricsLineChart(metrics = metricsList)
            }

            // 4. Tiêu đề Lịch sử đo & Bộ lọc Chips
            item {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lịch sử đo chỉ số",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${filteredHistory.size} bản ghi",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Thanh lọc danh sách lịch sử
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val filterList = listOf(
                            "ALL" to "Tất cả",
                            MetricType.HEART_RATE.name to "❤️ Nhịp tim",
                            MetricType.BLOOD_PRESSURE.name to "🩺 Huyết áp",
                            MetricType.BLOOD_SUGAR.name to "🩸 Đường huyết",
                            MetricType.TEMPERATURE.name to "🌡️ Nhiệt độ"
                        )
                        items(filterList, key = { it.first }) { (typeKey, label) ->
                            val isSelected = historyFilter == typeKey
                            FilterChip(
                                selected = isSelected,
                                onClick = { historyFilter = typeKey },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    selectedContainerColor = WarmPrimaryLight,
                                    labelColor = Color(0xFF475569),
                                    selectedLabelColor = WarmPrimaryTeal
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) WarmPrimaryTeal else Color(0xFFE2E6EC)
                                )
                            )
                        }
                    }
                }
            }

            // 5. Thanh chọn ngày (DatePicker Trigger Button)
            item {
                Card(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cụm trái: Icon lịch + Text ngày + Chevron down
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Chọn ngày đo",
                                tint = WarmPrimaryTeal,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Mở lịch chọn ngày",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cụm phải: Badge số lần đo tinh gọn
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = "${metricsForSelectedDay.size} lần đo",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            // 6. Danh sách các lần đo thuộc về ngày được chọn
            if (metricsForSelectedDay.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF8FAFC)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Chưa có chỉ số nào được ghi nhận trong ngày này",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(metricsForSelectedDay, key = { it.id }) { metric ->
                    MetricHistoryCard(
                        metric = metric,
                        timeFormat = timeFormat,
                        onCardClick = { metricDetailToView = metric }
                    )
                }
            }
        }
    }

    // Modal nhập chỉ số thủ công
    if (showAddDialog) {
        val dialogTimestamp = remember(isTodaySelected, selectedDateMillis) {
            if (isTodaySelected) {
                System.currentTimeMillis()
            } else {
                val nowCal = Calendar.getInstance()
                Calendar.getInstance().apply {
                    timeInMillis = selectedDateMillis
                    set(Calendar.HOUR_OF_DAY, nowCal.get(Calendar.HOUR_OF_DAY))
                    set(Calendar.MINUTE, nowCal.get(Calendar.MINUTE))
                    set(Calendar.SECOND, nowCal.get(Calendar.SECOND))
                }.timeInMillis
            }
        }

        AddMetricDialog(
            userId = currentUserId,
            initialType = dialogInitialType,
            targetTimestamp = dialogTimestamp,
            initialSystolic = voiceSystolic ?: "",
            initialDiastolic = voiceDiastolic ?: "",
            initialPulse = voicePulse ?: "",
            initialBloodSugar = voiceBloodSugar ?: "",
            initialTemperature = voiceTemp ?: "",
            onDismiss = {
                showAddDialog = false
                voiceSystolic = null
                voiceDiastolic = null
                voicePulse = null
                voiceBloodSugar = null
                voiceTemp = null
            },
            onSave = { mainMetric, secondaryMetric ->
                scope.launch {
                    database.healthMetricDao.insertMetric(mainMetric)
                    secondaryMetric?.let { database.healthMetricDao.insertMetric(it) }
                    showAddDialog = false
                    voiceSystolic = null
                    voiceDiastolic = null
                    voicePulse = null
                    voiceBloodSugar = null
                    voiceTemp = null
                    snackbarHostState.showSnackbar("Đã lưu chỉ số sức khỏe thành công!")
                }
            }
        )
    }

    // Modal DatePicker chọn ngày đo trong quá khứ
    if (showDatePicker) {
        val initialUtcMillis = remember(selectedDateMillis) {
            val cal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
            val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                clear()
                set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 12, 0, 0)
            }
            utcCal.timeInMillis
        }
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialUtcMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis() + 86400000L
                }
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { utcMillis ->
                            val calUtc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                                timeInMillis = utcMillis
                            }
                            val year = calUtc.get(Calendar.YEAR)
                            val month = calUtc.get(Calendar.MONTH)
                            val day = calUtc.get(Calendar.DAY_OF_MONTH)

                            val localCal = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedDateMillis = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn", fontWeight = FontWeight.Bold, color = WarmPrimaryTeal)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Chọn ngày xem chỉ số",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            )
        }
    }

    // Modal chi tiết lần đo & tùy chọn xóa an toàn
    metricDetailToView?.let { detail ->
        AlertDialog(
            onDismissRequest = { metricDetailToView = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = {
                Text(
                    text = "Chi Tiết Bản Ghi Đo",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (detail.type) {
                                MetricType.BLOOD_PRESSURE.name -> "Huyết áp"
                                MetricType.BLOOD_SUGAR.name -> "Đường huyết"
                                MetricType.TEMPERATURE.name -> "Thân nhiệt"
                                else -> "Nhịp tim"
                            },
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                        Text(
                            text = when (detail.type) {
                                MetricType.BLOOD_PRESSURE.name -> "${detail.valuePrimary.toInt()} / ${detail.valueSecondary?.toInt() ?: 80} mmHg"
                                MetricType.BLOOD_SUGAR.name -> "${detail.valuePrimary.toInt()} mg/dL"
                                MetricType.TEMPERATURE.name -> "${detail.valuePrimary} °C"
                                else -> "${detail.valuePrimary.toInt()} BPM"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Thời gian đo:", color = Color(0xFF64748B), fontSize = 13.sp)
                        Text(
                            SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault()).format(Date(detail.timestamp)),
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = Color(0xFF1E293B)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Phương thức:", color = Color(0xFF64748B), fontSize = 13.sp)
                        Text(
                            if (detail.isFromCameraPpg) "Camera PPG quang học" else "Nhập thủ công",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (detail.isFromCameraPpg) Color(0xFFE11D48) else Color(0xFF1E293B)
                        )
                    }

                    if (detail.notes.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ghi chú:", color = Color(0xFF64748B), fontSize = 13.sp)
                            Text(
                                detail.notes,
                                fontWeight = FontWeight.Medium,
                                fontSize = 13.sp,
                                color = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = detail
                        metricDetailToView = null
                        metricToDelete = target
                    }
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Xóa bản ghi",
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { metricDetailToView = null }) {
                    Text("Đóng", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Hộp thoại xác nhận xóa chỉ số
    metricToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { metricToDelete = null },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("Xác nhận xóa chỉ số", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Bạn có chắc chắn muốn xóa bản ghi đo này khỏi lịch sử theo dõi sức khỏe?", color = Color(0xFF475569)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            database.healthMetricDao.deleteMetric(target.id)
                            metricToDelete = null
                            snackbarHostState.showSnackbar("Đã xóa bản ghi thành công")
                        }
                    }
                ) {
                    Text("Xóa", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { metricToDelete = null }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            }
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(17.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = Color(0xFF64748B),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MetricHistoryCard(
    metric: HealthMetricEntity,
    timeFormat: SimpleDateFormat,
    onCardClick: () -> Unit
) {
    val (icon, iconColor, pastelBg, title, displayValue, statusBadge, badgeColor) = when (metric.type) {
        MetricType.BLOOD_PRESSURE.name -> {
            val sys = metric.valuePrimary.toInt()
            val dia = metric.valueSecondary?.toInt() ?: 80
            val (badge, color) = when {
                sys < 90 || dia < 60 -> Pair("Huyết áp thấp", Color(0xFFEA580C))
                sys >= 140 || dia >= 90 -> Pair("Tăng huyết áp", Color(0xFFDC2626))
                sys in 130..139 || dia in 81..89 -> Pair("Tiền tăng HA", Color(0xFFEA580C))
                sys < 120 && dia < 80 -> Pair("Tối ưu", Color(0xFF15803D))
                else -> Pair("Bình thường", Color(0xFF15803D))
            }
            Tuple7(
                Icons.Default.Speed,
                Color(0xFFE53935),
                Color(0xFFFFEBEE),
                "Huyết áp",
                "$sys / $dia mmHg",
                badge,
                color
            )
        }
        MetricType.BLOOD_SUGAR.name -> {
            val sugar = metric.valuePrimary
            val (badge, color) = when {
                sugar < 70 -> Pair("Hạ đường huyết", Color(0xFFDC2626))
                sugar in 70.0..140.0 -> Pair("Bình thường", Color(0xFF15803D))
                sugar in 140.1..199.0 -> Pair("Tiền tiểu đường", Color(0xFFEA580C))
                else -> Pair("Đường huyết cao", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.WaterDrop,
                Color(0xFFFB8C00),
                Color(0xFFFFF3E0),
                "Đường huyết",
                "${sugar.toInt()} mg/dL",
                badge,
                color
            )
        }
        MetricType.TEMPERATURE.name -> {
            val temp = metric.valuePrimary
            val (badge, color) = when {
                temp < 36.0 -> Pair("Thân nhiệt thấp", Color(0xFF0288D1))
                temp in 36.0..37.5 -> Pair("Bình thường", Color(0xFF15803D))
                temp in 37.6..38.5 -> Pair("Sốt nhẹ", Color(0xFFEA580C))
                else -> Pair("Sốt cao", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.Thermostat,
                Color(0xFF0288D1),
                Color(0xFFE1F5FE),
                "Thân nhiệt",
                "$temp °C",
                badge,
                color
            )
        }
        else -> { // HEART_RATE
            val bpm = metric.valuePrimary.toInt()
            val hrLevel = HeartRateClassifier.classify(bpm)
            Tuple7(
                Icons.Default.Favorite,
                Color(0xFFE11D48),
                Color(0xFFFFF1F2),
                if (metric.isFromCameraPpg) "Nhịp tim PPG" else "Nhịp tim",
                "$bpm BPM",
                hrLevel.badgeLabel,
                hrLevel.color
            )
        }
    }

    val timeStr = timeFormat.format(Date(metric.timestamp))
    val subtitleText = remember(metric) {
        val note = metric.notes.trim()
        when {
            note.isNotEmpty() -> "$timeStr • $note"
            metric.isFromCameraPpg -> "$timeStr • Camera PPG"
            else -> "$timeStr • Nhập thủ công"
        }
    }

    Card(
        onClick = onCardClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // CỘT TRÁI: Icon background pastel + Tên chỉ số + Badge trạng thái
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(pastelBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        softWrap = false
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusBadge,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // CỘT PHẢI: Chỉ số đo to rõ + Text thời gian & ghi chú
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayValue,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.5.sp,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitleText,
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatSelectedDateHeader(timestamp: Long): String {
    val calTarget = Calendar.getInstance().apply { timeInMillis = timestamp }
    val calNow = Calendar.getInstance()

    val isToday = calTarget.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
            calTarget.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

    val calYesterday = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }
    val isYesterday = calTarget.get(Calendar.YEAR) == calYesterday.get(Calendar.YEAR) &&
            calTarget.get(Calendar.DAY_OF_YEAR) == calYesterday.get(Calendar.DAY_OF_YEAR)

    val dayOfWeek = when (calTarget.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Thứ Hai"
        Calendar.TUESDAY -> "Thứ Ba"
        Calendar.WEDNESDAY -> "Thứ Tư"
        Calendar.THURSDAY -> "Thứ Năm"
        Calendar.FRIDAY -> "Thứ Sáu"
        Calendar.SATURDAY -> "Thứ Bảy"
        Calendar.SUNDAY -> "Chủ Nhật"
        else -> ""
    }

    val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))

    return when {
        isToday -> "Hôm nay, $dateStr"
        isYesterday -> "Hôm qua, $dateStr"
        else -> "$dayOfWeek, $dateStr"
    }
}

private data class Tuple7<A, B, C, D, E, F, G>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F, val g: G
)

/**
 * Model chứa dữ liệu bóc tách từ giọng nói
 */
data class ParsedVoiceData(
    val metricType: MetricType = MetricType.BLOOD_PRESSURE,
    val systolic: String? = null,
    val diastolic: String? = null,
    val pulse: String? = null,
    val bloodSugar: String? = null,
    val temperature: String? = null
)

/**
 * Hàm phân tích câu nói tiếng Việt để bóc tách chỉ số sinh tồn
 */
fun parseVoiceMetrics(spokenText: String): ParsedVoiceData {
    val lower = spokenText.lowercase().trim()

    // 1. Thân nhiệt / Nhiệt độ
    if (lower.contains("nhiệt") || lower.contains("thân nhiệt") || lower.contains("sốt")) {
        val degreePattern = Regex("""(\d{2})\s*(?:độ|\.)\s*(\d)?""")
        val degreeMatch = degreePattern.find(lower)
        if (degreeMatch != null) {
            val main = degreeMatch.groupValues[1]
            val decimal = degreeMatch.groupValues.getOrNull(2)?.ifBlank { null }
            val temp = if (decimal != null) "$main.$decimal" else main
            return ParsedVoiceData(metricType = MetricType.TEMPERATURE, temperature = temp)
        }
        val numMatch = Regex("""\d{2}(?:[.,]\d+)?""").find(lower)
        if (numMatch != null) {
            return ParsedVoiceData(metricType = MetricType.TEMPERATURE, temperature = numMatch.value.replace(',', '.'))
        }
        return ParsedVoiceData(metricType = MetricType.TEMPERATURE)
    }

    // 2. Đường huyết
    if (lower.contains("đường") || lower.contains("tiểu đường")) {
        val numMatch = Regex("""\d+(?:[.,]\d+)?""").find(lower)
        if (numMatch != null) {
            return ParsedVoiceData(metricType = MetricType.BLOOD_SUGAR, bloodSugar = numMatch.value.replace(',', '.'))
        }
        return ParsedVoiceData(metricType = MetricType.BLOOD_SUGAR)
    }

    // 3. Huyết áp & Nhịp tim
    var pulseVal: String? = null
    val pulseMatch = Regex("""(?:nhịp tim|tim|mạch)\s*[:=]?\s*(\d{2,3})""").find(lower)
    if (pulseMatch != null) {
        pulseVal = pulseMatch.groupValues[1]
    }

    // Tìm dạng "120 trên 80" hoặc "120 / 80"
    val bpSlashPattern = Regex("""(\d{2,3})\s*(?:trên|\/)\s*(\d{2,3})""")
    val bpMatch = bpSlashPattern.find(lower)
    if (bpMatch != null) {
        val sys = bpMatch.groupValues[1]
        val dia = bpMatch.groupValues[2]
        return ParsedVoiceData(
            metricType = MetricType.BLOOD_PRESSURE,
            systolic = sys,
            diastolic = dia,
            pulse = pulseVal
        )
    }

    // Lấy tất cả các số 2-3 chữ số
    val numbers = Regex("""\b\d{2,3}\b""").findAll(lower).map { it.value }.toList()
    if (numbers.size >= 2) {
        val sys = numbers[0]
        val dia = numbers[1]
        val pulse = pulseVal ?: numbers.getOrNull(2)
        return ParsedVoiceData(
            metricType = MetricType.BLOOD_PRESSURE,
            systolic = sys,
            diastolic = dia,
            pulse = pulse
        )
    } else if (numbers.size == 1 && (lower.contains("tim") || lower.contains("nhịp"))) {
        return ParsedVoiceData(
            metricType = MetricType.HEART_RATE,
            pulse = numbers[0]
        )
    }

    return ParsedVoiceData(metricType = MetricType.BLOOD_PRESSURE)
}
