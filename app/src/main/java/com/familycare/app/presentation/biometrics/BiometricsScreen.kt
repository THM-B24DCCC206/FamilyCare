package com.familycare.app.presentation.biometrics

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.ui.window.Dialog
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.presentation.biometrics.chart.BiometricsLineChart
import com.familycare.app.presentation.biometrics.dialog.AddMetricDialog
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
    val currentUserId = remember(sessionManager.userId) { sessionManager.userId.ifEmpty { "user_me" } }
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

    // Lọc danh sách đo thuộc về ngày được chọn
    val metricsForSelectedDay = remember(filteredHistory, selectedDateMillis) {
        val targetCal = Calendar.getInstance().apply { timeInMillis = selectedDateMillis }
        val targetYear = targetCal.get(Calendar.YEAR)
        val targetMonth = targetCal.get(Calendar.MONTH)
        val targetDay = targetCal.get(Calendar.DAY_OF_MONTH)

        filteredHistory.filter { metric ->
            val metricCal = Calendar.getInstance().apply { timeInMillis = metric.timestamp }
            metricCal.get(Calendar.YEAR) == targetYear &&
            metricCal.get(Calendar.MONTH) == targetMonth &&
            metricCal.get(Calendar.DAY_OF_MONTH) == targetDay
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
        containerColor = Color(0xFFF8F9FA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Chỉ Số Sinh Tồn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF8F9FA),
                    titleContentColor = Color(0xFF0F172A)
                ),
                actions = {
                    IconButton(onClick = onNavigateToSmartwatch) {
                        Icon(
                            imageVector = Icons.Default.Watch,
                            contentDescription = "Kết nối BLE Smartwatch",
                            tint = Color(0xFF0F172A)
                        )
                    }
                    IconButton(onClick = {
                        voiceSystolic = null
                        voiceDiastolic = null
                        voicePulse = null
                        voiceBloodSugar = null
                        voiceTemp = null
                        dialogInitialType = MetricType.BLOOD_PRESSURE
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Nhập chỉ số thủ công", tint = Color(0xFF0F172A))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Banner Đo Nhịp Tim Quang Học (PPG) dạng ngang gọn gàng (cao ~80dp)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFE4E6)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Đo nhịp tim (PPG)",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đo nhanh qua Camera",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = onNavigateToCameraPpg,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
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
                        text = "Nhập Chỉ Số Lâm Sàng Ngoài Máy",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Nút Micro tròn to nổi bật màu xanh dương với nhãn "🎙️ Nói để nhập"
                    Surface(
                        onClick = { startVoiceInput() },
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEFF6FF), // Nền xanh dương siêu nhạt
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)), // Viền mảnh
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
                                    .background(Color(0xFF2563EB)), // Xanh dương chủ đạo
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
                                    color = Color(0xFF1E40AF) // Tiêu đề xanh đậm
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Đọc: \"Huyết áp 120 trên 80, tim 75\"",
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF3B82F6)
                            )
                        }
                    }

                    // 3 phím tắt Huyết Áp, Đường Huyết, Thân Nhiệt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                            iconColor = Color(0xFF1E88E5),
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
                Column(modifier = Modifier.padding(top = 6.dp)) {
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
                        Text(
                            text = "${filteredHistory.size} bản ghi",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Thanh lọc danh sách lịch sử
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
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
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White,
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    labelColor = Color(0xFF475569),
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else Color(0xFFE2E8F0)
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
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFECEFF1)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cụm trái: Icon lịch + Text ngày + Chevron down
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Chọn ngày đo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedDateHeader,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1
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

                        // Cụm phải: Nút về Hôm nay (nếu đang chọn ngày khác) + Badge số lần đo
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isTodaySelected) {
                                Surface(
                                    onClick = { selectedDateMillis = System.currentTimeMillis() },
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(end = 6.dp)
                                ) {
                                    Text(
                                        text = "Hôm nay",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
            }

            // 6. Danh sách các lần đo thuộc về ngày được chọn
            if (metricsForSelectedDay.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFECEFF1))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF8F9FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.EventNote,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
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

            // Khoảng trống đệm dưới cùng để không bị che bởi FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Modal nhập chỉ số thủ công
    if (showAddDialog) {
        AddMetricDialog(
            userId = currentUserId,
            initialType = dialogInitialType,
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
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDateMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    // Cho phép chọn các ngày từ quá khứ đến hôm nay (+ buffer 1 ngày múi giờ)
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
                            }
                            selectedDateMillis = localCal.timeInMillis
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Chọn", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Hủy")
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
                        fontWeight = FontWeight.Bold
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
                            .background(Color(0xFFF8F9FA), RoundedCornerShape(12.dp))
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
                            if (detail.isFromCameraPpg) "CameraX PPG quang học" else "Nhập thủ công",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (detail.isFromCameraPpg) Color(0xFFE91E63) else Color(0xFF1E293B)
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
                    Text("Đóng")
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
                    Text("Hủy")
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
        border = BorderStroke(1.dp, Color(0xFFECEFF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = Color(0xFF0F172A),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                sys < 120 && dia < 80 -> Pair("Tối ưu", Color(0xFF16A34A))
                sys in 120..129 && dia < 80 -> Pair("Bình thường cao", Color(0xFF16A34A))
                sys in 130..139 || dia in 80..89 -> Pair("Tiền tăng HA", Color(0xFFEA580C))
                else -> Pair("Tăng huyết áp", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.Speed,
                Color(0xFFE53935),
                Color(0xFFFFEBEE), // pastel red
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
                sugar in 70.0..140.0 -> Pair("Bình thường", Color(0xFF16A34A))
                sugar in 140.1..199.0 -> Pair("Tiền tiểu đường", Color(0xFFEA580C))
                else -> Pair("Đường huyết cao", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.WaterDrop,
                Color(0xFFFB8C00),
                Color(0xFFFFF3E0), // pastel orange
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
                temp in 36.0..37.5 -> Pair("Bình thường", Color(0xFF16A34A))
                temp in 37.6..38.5 -> Pair("Sốt nhẹ", Color(0xFFEA580C))
                else -> Pair("Sốt cao", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.Thermostat,
                Color(0xFF0288D1),
                Color(0xFFE1F5FE), // pastel blue
                "Thân nhiệt",
                "$temp °C",
                badge,
                color
            )
        }
        else -> { // HEART_RATE
            val bpm = metric.valuePrimary.toInt()
            val (badge, color) = when {
                bpm < 60 -> Pair("Nhịp chậm", Color(0xFF0288D1))
                bpm in 60..100 -> Pair("Bình thường", Color(0xFF16A34A))
                else -> Pair("Nhịp nhanh", Color(0xFFDC2626))
            }
            Tuple7(
                Icons.Default.Favorite,
                Color(0xFFE91E63),
                Color(0xFFFCE4EC), // pastel pink
                if (metric.isFromCameraPpg) "Nhịp tim PPG" else "Nhịp tim",
                "$bpm BPM",
                badge,
                color
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECEFF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp, pressedElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // === CỘT TRÁI ===
            // Icon background pastel + Tên chỉ số + Badge trạng thái
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(pastelBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
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
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // === CỘT PHẢI ===
            // Chỉ số đo to rõ (align right) + Text thời gian & ghi chú (màu xám nhạt)
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = displayValue,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
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

    return if (isToday) {
        "Hôm nay, $dateStr"
    } else {
        "$dayOfWeek, $dateStr"
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
 * Ví dụ:
 * - "Huyết áp 120 trên 80 nhịp tim 75" -> SYS=120, DIA=80, PULSE=75
 * - "130 trên 85" -> SYS=130, DIA=85
 * - "Đường huyết 110" -> BLOOD_SUGAR=110
 * - "Thân nhiệt 37 độ 5" -> TEMPERATURE=37.5
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
