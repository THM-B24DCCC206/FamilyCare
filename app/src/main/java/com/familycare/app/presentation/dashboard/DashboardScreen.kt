package com.familycare.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import com.familycare.app.domain.model.MetricType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPpg: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToOcr: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToSos: () -> Unit = onNavigateToEmergency,
    viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FamilyCare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (sessionManager.hasFamily) "Tổ ấm: ${sessionManager.familyName}" else "Không gian gia đình thông minh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToEmergency) {
                        BadgedBox(
                            badge = {
                                if (uiState.activeRedAlert != null) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Cảnh báo",
                                tint = if (uiState.activeRedAlert != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
        ) {
            // Cảnh báo đỏ vượt ngưỡng (nếu phát hiện chuỗi nguy cơ từ Rule Engine)
            if (uiState.activeRedAlert != null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToEmergency() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = uiState.activeRedAlert?.title ?: "CẢNH BÁO NGUY HIỂM",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = uiState.activeRedAlert?.message ?: "Chỉ số sinh tồn vượt ngưỡng an toàn",
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // 1. SOS Card Khẩn cấp (Chuẩn theo giao diện Nhánh 1)
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "NÚT KHẨN CẤP SOS",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                text = "Nhấn giữ 2s để hú còi & gửi định vị GPS cho người thân",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onNavigateToSos,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("SOS", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 2. Quick Actions: Tính năng thông minh (Chuẩn theo giao diện Nhánh 1)
            item {
                Text(
                    text = "Tính năng thông minh",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ElevatedCard(
                        onClick = onNavigateToPpg,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = "Đo nhịp tim",
                                tint = Color(0xFFE91E63),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Đo Nhịp Tim",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Camera & Flash (PPG)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    ElevatedCard(
                        onClick = onNavigateToAi,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "Trợ lý AI",
                                tint = Color(0xFF673AB7),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Trợ Lý AI",
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Gemini Health Copilot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 3. Tóm tắt chỉ số gần nhất (Chuẩn giao diện Nhánh 1, liên kết dữ liệu thực tế)
            item {
                Text(
                    text = "Chỉ số sức khỏe mới nhất",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Huyết áp
                        val bpMetric = uiState.latestBloodPressure
                        val bpText = if (bpMetric != null) {
                            "${bpMetric.valuePrimary.toInt()}/${bpMetric.valueSecondary?.toInt() ?: 0} ${bpMetric.unit}"
                        } else {
                            "125/82 mmHg"
                        }
                        val bpColor = if (bpMetric != null && (bpMetric.valuePrimary >= 140 || (bpMetric.valueSecondary ?: 0.0) >= 90)) {
                            Color(0xFFD32F2F)
                        } else {
                            Color(0xFF2E7D32)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Huyết áp sáng nay:")
                            Text(
                                text = bpText,
                                fontWeight = FontWeight.Bold,
                                color = bpColor
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Nhịp tim
                        val hrMetric = uiState.latestHeartRate
                        val hrText = if (hrMetric != null) {
                            "${hrMetric.valuePrimary.toInt()} ${hrMetric.unit}"
                        } else {
                            "72 BPM"
                        }
                        val hrColor = if (hrMetric != null && (hrMetric.valuePrimary > 100 || hrMetric.valuePrimary < 55)) {
                            Color(0xFFD32F2F)
                        } else {
                            Color(0xFF1976D2)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Nhịp tim lúc nghỉ:")
                            Text(
                                text = hrText,
                                fontWeight = FontWeight.Bold,
                                color = hrColor
                            )
                        }
                    }
                }
            }

            // 4. Lịch uống thuốc kế tiếp (Nếu có)
            val med = uiState.upcomingMedicine
            if (med != null) {
                item {
                    Text(
                        text = "Lịch thuốc hôm nay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(med.name, fontWeight = FontWeight.Bold)
                                Text("${med.dosage} • ${med.instruction}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = med.reminderTimes.firstOrNull() ?: "Đúng giờ",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
