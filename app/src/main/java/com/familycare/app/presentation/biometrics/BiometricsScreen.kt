package com.familycare.app.presentation.biometrics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onNavigateToCameraPpg: () -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val metricsList by database.healthMetricDao.getMetricsByMember("user_me")
        .collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault()) }

    // Dialog state
    var showAddDialog by remember { mutableStateOf(false) }
    var dialogInitialType by remember { mutableStateOf(MetricType.BLOOD_PRESSURE) }

    // Dialog xác nhận xóa
    var metricToDelete by remember { mutableStateOf<HealthMetricEntity?>(null) }

    // Bộ lọc danh sách lịch sử
    var historyFilter by remember { mutableStateOf("ALL") }

    val filteredHistory = remember(metricsList, historyFilter) {
        if (historyFilter == "ALL") metricsList
        else metricsList.filter { it.type == historyFilter }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Chỉ Số Sinh Tồn", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        dialogInitialType = MetricType.BLOOD_PRESSURE
                        showAddDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Nhập chỉ số thủ công")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Card kêu gọi đo PPG bằng Camera (Tính năng độc đáo của TV1)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Đo Nhịp Tim Quang Học (PPG)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Áp ngón tay lên camera & flash để đo nhịp tim tự động qua mao mạch",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onNavigateToCameraPpg,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Bắt đầu đo Camera")
                        }
                    }
                }
            }

            // 2. Thanh phím tắt nhập nhanh các chỉ số y tế lâm sàng
            item {
                Column {
                    Text(
                        text = "Nhập Chỉ Số Lâm Sàng Ngoài Máy",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lịch sử đo chỉ số (${filteredHistory.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Thanh lọc danh sách lịch sử
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = historyFilter == "ALL",
                            onClick = { historyFilter = "ALL" },
                            label = { Text("Tất cả", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = historyFilter == MetricType.HEART_RATE.name,
                            onClick = { historyFilter = MetricType.HEART_RATE.name },
                            label = { Text("Nhịp tim", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = historyFilter == MetricType.BLOOD_PRESSURE.name,
                            onClick = { historyFilter = MetricType.BLOOD_PRESSURE.name },
                            label = { Text("Huyết áp", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = historyFilter == MetricType.BLOOD_SUGAR.name,
                            onClick = { historyFilter = MetricType.BLOOD_SUGAR.name },
                            label = { Text("Đường", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = historyFilter == MetricType.TEMPERATURE.name,
                            onClick = { historyFilter = MetricType.TEMPERATURE.name },
                            label = { Text("Nhiệt độ", fontSize = 11.sp) }
                        )
                    }
                }
            }

            // 5. Danh sách các lần đo lấy trực tiếp từ Database
            if (filteredHistory.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Chưa có dữ liệu nào phù hợp với bộ lọc này.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredHistory, key = { it.id }) { metric ->
                    MetricHistoryCard(
                        metric = metric,
                        dateFormat = dateFormat,
                        onDelete = { metricToDelete = metric }
                    )
                }
            }

            // Khoảng trống đệm dưới cùng để không bị che bởi FAB
            item {
                Spacer(modifier = Modifier.height(64.dp))
            }
        }
    }

    // Modal nhập chỉ số thủ công
    if (showAddDialog) {
        AddMetricDialog(
            initialType = dialogInitialType,
            onDismiss = { showAddDialog = false },
            onSave = { mainMetric, secondaryMetric ->
                scope.launch {
                    database.healthMetricDao.insertMetric(mainMetric)
                    secondaryMetric?.let { database.healthMetricDao.insertMetric(it) }
                    showAddDialog = false
                    snackbarHostState.showSnackbar("Đã lưu chỉ số sức khỏe thành công!")
                }
            }
        )
    }

    // Hộp thoại xác nhận xóa chỉ số
    metricToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { metricToDelete = null },
            title = { Text("Xác nhận xóa chỉ số") },
            text = { Text("Bạn có chắc chắn muốn xóa bản ghi đo này khỏi lịch sử?") },
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricHistoryCard(
    metric: HealthMetricEntity,
    dateFormat: SimpleDateFormat,
    onDelete: () -> Unit
) {
    val (icon, iconColor, title, displayValue, statusBadge, badgeColor) = when (metric.type) {
        MetricType.BLOOD_PRESSURE.name -> {
            val sys = metric.valuePrimary.toInt()
            val dia = metric.valueSecondary?.toInt() ?: 80
            val (badge, color) = when {
                sys < 90 || dia < 60 -> Pair("Huyết áp thấp", Color(0xFFFB8C00))
                sys < 120 && dia < 80 -> Pair("Tối ưu", Color(0xFF2E7D32))
                sys in 120..129 && dia < 80 -> Pair("Bình thường cao", Color(0xFF43A047))
                sys in 130..139 || dia in 80..89 -> Pair("Tiền tăng HA", Color(0xFFE65100))
                else -> Pair("Tăng huyết áp", Color(0xFFD32F2F))
            }
            Tuple6(
                Icons.Default.Speed,
                Color(0xFFE53935),
                "Huyết áp",
                "$sys / $dia mmHg",
                badge,
                color
            )
        }
        MetricType.BLOOD_SUGAR.name -> {
            val sugar = metric.valuePrimary
            val (badge, color) = when {
                sugar < 70 -> Pair("Hạ đường huyết", Color(0xFFD32F2F))
                sugar in 70.0..140.0 -> Pair("Bình thường", Color(0xFF2E7D32))
                sugar in 140.1..199.0 -> Pair("Tiền tiểu đường", Color(0xFFE65100))
                else -> Pair("Đường huyết cao", Color(0xFFD32F2F))
            }
            Tuple6(
                Icons.Default.WaterDrop,
                Color(0xFFFB8C00),
                "Đường huyết",
                "${sugar.toInt()} mg/dL",
                badge,
                color
            )
        }
        MetricType.TEMPERATURE.name -> {
            val temp = metric.valuePrimary
            val (badge, color) = when {
                temp < 36.0 -> Pair("Thân nhiệt thấp", Color(0xFF1976D2))
                temp in 36.0..37.5 -> Pair("Bình thường", Color(0xFF2E7D32))
                temp in 37.6..38.5 -> Pair("Sốt nhẹ", Color(0xFFFB8C00))
                else -> Pair("Sốt cao", Color(0xFFD32F2F))
            }
            Tuple6(
                Icons.Default.Thermostat,
                Color(0xFF1E88E5),
                "Thân nhiệt",
                "$temp °C",
                badge,
                color
            )
        }
        else -> { // HEART_RATE
            val bpm = metric.valuePrimary.toInt()
            val (badge, color) = when {
                bpm < 60 -> Pair("Nhịp chậm", Color(0xFF1976D2))
                bpm in 60..100 -> Pair("Bình thường", Color(0xFF2E7D32))
                else -> Pair("Nhịp nhanh", Color(0xFFD32F2F))
            }
            Tuple6(
                Icons.Default.Favorite,
                Color(0xFFE91E63),
                if (metric.isFromCameraPpg) "Nhịp tim PPG" else "Nhịp tim",
                "$bpm BPM",
                badge,
                color
            )
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Đánh giá y tế Tag
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = badgeColor.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = statusBadge,
                                color = badgeColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(Date(metric.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (metric.notes.isNotBlank()) {
                        Text(
                            text = "📝 ${metric.notes}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (metric.isFromCameraPpg) {
                        Text(
                            text = "⚡ Đo bằng CameraX PPG",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFE91E63)
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = iconColor
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
