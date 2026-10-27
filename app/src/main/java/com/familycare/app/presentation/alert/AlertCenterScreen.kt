package com.familycare.app.presentation.alert

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.database.AlertEntity
import com.familycare.app.domain.model.AlertLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSos: () -> Unit = {},
    viewModel: AlertViewModel = viewModel(
        factory = AlertViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Trung Tâm Cảnh Báo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearAllAlerts() }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Xóa sạch tất cả cảnh báo",
                            tint = Color(0xFF64748B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // THANH TAB BỘ LỌC (FILTER CHIPS HEADER)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = uiState.selectedFilter == AlertFilter.ALL
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(AlertFilter.ALL) },
                        label = {
                            Text(
                                text = "Tất cả (${uiState.allCount})",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.wrapContentWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE2E8F0),
                            selectedLabelColor = Color(0xFF0F172A)
                        )
                    )
                }

                item {
                    val isSelected = uiState.selectedFilter == AlertFilter.CRITICAL
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(AlertFilter.CRITICAL) },
                        label = {
                            Text(
                                text = "Khẩn cấp (${uiState.criticalCount})",
                                color = if (isSelected) Color(0xFFDC2626) else Color(0xFF64748B),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.wrapContentWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEE2E2),
                            selectedLabelColor = Color(0xFFDC2626)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(0xFFEF4444) else Color(0xFFCBD5E1),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }

                item {
                    val isSelected = uiState.selectedFilter == AlertFilter.WARNING
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onFilterSelected(AlertFilter.WARNING) },
                        label = {
                            Text(
                                text = "Nhắc nhở (${uiState.warningCount})",
                                color = if (isSelected) Color(0xFFD97706) else Color(0xFF64748B),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                modifier = Modifier.wrapContentWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFEF3C7),
                            selectedLabelColor = Color(0xFFD97706)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // DANH SÁCH THẺ CẢNH BÁO
            if (uiState.alerts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không có cảnh báo bất thường",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Các chỉ số sức khỏe của gia đình đang ở vùng an toàn.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = uiState.alerts,
                        key = { alert -> "${alert.id}_${alert.timestamp}" }
                    ) { alert ->
                        AlertItemCard(
                            alert = alert,
                            onMarkAsRead = { viewModel.onMarkAsRead(alert.id) },
                            onOpenMap = { lat, lng ->
                                openNavigationMap(context, lat, lng)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onMarkAsRead: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    val isCritical = alert.alertLevel == AlertLevel.CRITICAL
    val isWarning = alert.alertLevel == AlertLevel.WARNING

    val cardBg = if (alert.isRead) Color(0xFFF8FAFC) else Color.White
    // Viền bo tròn trực tiếp trên Card: đỏ tươi cho khẩn cấp chưa đọc, vàng hổ phách cho nhắc nhở chưa đọc, xám mỏng khi đã đọc
    val cardBorder = when {
        !alert.isRead && isCritical -> BorderStroke(1.5.dp, Color(0xFFEF4444))
        !alert.isRead && isWarning -> BorderStroke(1.5.dp, Color(0xFFF59E0B))
        else -> BorderStroke(1.dp, Color(0xFFE2E8F0))
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (alert.isRead) 1.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. Dòng thông tin trên cùng: "Tên thành viên • Giờ - Ngày" và Huy hiệu [ MỚI ]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${alert.memberName.ifBlank { "Thành viên gia đình" }} • ${formatAlertTime(alert.timestamp)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B),
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!alert.isRead) {
                    val badgeBg = if (isCritical) Color(0xFFFEE2E2) else Color(0xFFFEF3C7)
                    val badgeTextColor = if (isCritical) Color(0xFFDC2626) else Color(0xFFB45309)
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "MỚI",
                            color = badgeTextColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Tiêu đề cảnh báo chống rớt dòng: "🚨 Báo động SOS: [Tên]"
            val displayTitle = when {
                isCritical -> "🚨 Báo động SOS: ${alert.memberName.ifBlank { "Người thân" }}"
                alert.title.trim() == "🚨" || alert.title.isBlank() -> "🚨 Báo động SOS: ${alert.memberName.ifBlank { "Người thân" }}"
                else -> alert.title
            }

            Text(
                text = displayTitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCritical) Color(0xFFDC2626) else if (isWarning) Color(0xFFD97706) else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Nội dung mô tả: Loại bỏ hoàn toàn "Vị trí thực tế: ...", chỉ giữ thông báo ngắn gọn
            val displayMessage = if (isCritical) {
                "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân."
            } else {
                val cleaned = alert.message
                    .replace(Regex("Vị trí thực tế:[^.]*\\.\\s*"), "")
                    .replace("Chưa nhận diện được vị trí GPS (Vui lòng bật Định vị). ", "")
                    .replace("(Vui lòng bật Định vị)", "")
                    .trim()
                cleaned.ifBlank { "Đã phát cảnh báo khẩn cấp tới toàn bộ người thân." }
            }

            Text(
                text = displayMessage,
                fontSize = 13.5.sp,
                color = Color(0xFF334155),
                lineHeight = 19.sp
            )

            // 4. Địa chỉ duy nhất bên dưới với icon ghim định vị màu cam
            val cleanAddress = alert.address?.takeIf {
                it.isNotBlank() &&
                        !it.startsWith("QPG2") &&
                        !it.startsWith("(") &&
                        !it.contains("Vui lòng bật") &&
                        !it.contains("Định vị") &&
                        !it.contains("Chưa nhận diện")
            }
            if (cleanAddress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEA580C),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cleanAddress,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Hàng nút hành động: Đồng bộ màu sắc theo trạng thái đã đọc / chưa đọc
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val lat = alert.latitude
                val lng = alert.longitude
                if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                    // Chưa đọc: màu đỏ cấp cứu Color(0xFFDC2626) | Đã xử lý: màu xám xanh Color(0xFF475569)
                    val mapBtnBg = if (!alert.isRead) Color(0xFFDC2626) else Color(0xFF475569)
                    Button(
                        onClick = { onOpenMap(lat, lng) },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = mapBtnBg),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Xem Bản Đồ",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!alert.isRead) {
                    OutlinedButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF475569)
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đánh dấu đã xem",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đã xử lý",
                            fontSize = 12.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

fun openNavigationMap(context: Context, latitude: Double, longitude: Double) {
    val navUri = Uri.parse("google.navigation:q=$latitude,$longitude")
    val intent = Intent(Intent.ACTION_VIEW, navUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun formatAlertTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
