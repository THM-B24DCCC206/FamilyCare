
package com.familycare.app.presentation.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToPdfReport: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPreview by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var reportMetrics by remember {
        mutableStateOf<List<HealthMetricEntity>>(emptyList())
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val message = try {
                    withContext(Dispatchers.IO) {
                        HealthReportPdfExporter.export(
                            context,
                            uri,
                            reportMetrics
                        )
                    }
                    "Đã xuất báo cáo PDF thành công."
                } catch (e: Exception) {
                    "Xuất PDF thất bại: ${
                        e.localizedMessage ?: "Lỗi không xác định"
                    }"
                }

                snackbarHostState.showSnackbar(message)
            }
        }
    }

    val locale = Locale("vi", "VN")
    val now = Calendar.getInstance()

    val monthStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val nextMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        add(Calendar.MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val monthText = SimpleDateFormat("MM/yyyy", locale)
        .format(now.time)

    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", locale)

    val monthlyMetrics = reportMetrics
        .filter {
            it.timestamp >= monthStart &&
                it.timestamp < nextMonth
        }
        .sortedByDescending { it.timestamp }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (showPreview) "Xem trước báo cáo"
                        else "Hồ Sơ Sức Khỏe",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (showPreview) {
                        IconButton(
                            onClick = { showPreview = false }
                        ) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Quay lại"
                            )
                        }
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) { paddingValues ->

        if (showPreview) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "BÁO CÁO SỨC KHỎE GIA ĐÌNH",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Tháng báo cáo: $monthText",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    "Ngày xem: ${
                        dateFormat.format(Date())
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )

                HorizontalDivider()

                Text(
                    "Dữ liệu chỉ số sức khỏe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Số bản ghi trong tháng: ${monthlyMetrics.size}",
                    style = MaterialTheme.typography.bodyLarge
                )

                if (monthlyMetrics.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement =
                                Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Chưa có dữ liệu sức khỏe",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Chưa tìm thấy chỉ số nào được lưu " +
                                    "trong tháng này cho tài khoản demo-user."
                            )
                            Text(
                                "Báo cáo sẽ không tự tạo số đo giả.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                } else {
                    monthlyMetrics.forEachIndexed { index, metric ->
                        val typeName = when (metric.type) {
                            "BLOOD_PRESSURE" -> "Huyết áp"
                            "HEART_RATE" -> "Nhịp tim"
                            "BLOOD_SUGAR" -> "Đường huyết"
                            "TEMPERATURE" -> "Nhiệt độ"
                            else -> metric.type
                        }

                        val value = if (
                            metric.valueSecondary != null
                        ) {
                            "${metric.valuePrimary}/" +
                                "${metric.valueSecondary} ${metric.unit}"
                        } else {
                            "${metric.valuePrimary} ${metric.unit}"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${index + 1}. $typeName",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    value,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Thời gian đo: ${
                                        dateFormat.format(
                                            Date(metric.timestamp)
                                        )
                                    }"
                                )

                                if (metric.notes.isNotBlank()) {
                                    Text("Ghi chú: ${metric.notes}")
                                }
                            }
                        }
                    }
                }

                Text(
                    "Lưu ý: Báo cáo tổng hợp dữ liệu đã lưu, " +
                        "không thay thế chẩn đoán của bác sĩ.",
                    style = MaterialTheme.typography.bodySmall
                )

                Button(
                    onClick = {
                        pdfLauncher.launch("Bao_cao_suc_khoe.pdf")
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Xuất PDF")
                }

                OutlinedButton(
                    onClick = { showPreview = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Quay lại hồ sơ")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                Text(
                    text = "Nguyễn Văn Nam (68 tuổi)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Nhóm máu: O+ | Bệnh nền: Cao huyết áp",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Mã mời gia đình:",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "FAM-982143",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Chia sẻ mã này để con cháu cùng " +
                                "tham gia theo dõi sức khỏe",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isLoading = true

                            try {
                                val metrics = withContext(Dispatchers.IO) {
                                    AppDatabase.getInstance(context)
                                        .healthMetricDao
                                        .getMetricsByMember("demo-user")
                                        .first()
                                }

                                reportMetrics = metrics
                                showPreview = true
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(
                                    "Không tải được báo cáo: ${
                                        e.localizedMessage
                                            ?: "Lỗi không xác định"
                                    }"
                                )
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Đang tải báo cáo...")
                    } else {
                        Text("Xem trước báo cáo sức khỏe")
                    }
                }
            }
        }
    }
}
