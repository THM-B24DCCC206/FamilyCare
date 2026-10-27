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
            // PHẦN 4.1: THANH TAB BỘ LỌC (FILTER CHIPS HEADER)
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Chip 1: Tất cả (X)
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

                // Chip 2: Khẩn cấp (Y) (chữ đỏ khi được chọn)
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

                // Chip 3: Nhắc nhở (Z) (chữ cam khi được chọn)
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

            // PHẦN 4.2: DANH SÁCH THẺ CẢNH BÁO
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
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.alerts, key = { it.id }) { alert ->
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

/**
 * Tối ưu hóa Thẻ Cảnh Báo (AlertItemCard):
 * 1. Card nền trắng, viền mỏng E2E8F0, góc bo tròn 16.dp, elevation 2.dp.
 * 2. Dải màu nhận diện mép trái: Đỏ tươi EF4444 (CRITICAL) hoặc Vàng hổ phách F59E0B (WARNING).
 * 3. Khi isRead == true: nền xám nhạt F8FAFC.
 * 4. Nút bấm chiều cao 42.dp, wrapContentHeight(), cỡ chữ 13sp, không bao giờ bị cắt chữ.
 */
@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onMarkAsRead: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    val isCritical = alert.alertLevel == AlertLevel.CRITICAL
    val isWarning = alert.alertLevel == AlertLevel.WARNING

    val indicatorColor = when {
        isCritical -> Color(0xFFEF4444)
        isWarning -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }

    val cardBg = if (alert.isRead) Color(0xFFF8FAFC) else Color.White

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = if (alert.isRead) 1.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Dải nhận diện màu sắc bên trái (Indicator Strip) 5.dp
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(indicatorColor)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(14.dp)
            ) {
                // Dòng trên cùng: Tên thành viên thật + Mốc thời gian + Tag "MỚI" nếu chưa đọc
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = alert.memberName.ifBlank { "Thành viên gia đình" },
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "•  ${formatAlertTime(alert.timestamp)}",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    if (!alert.isRead) {
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "MỚI",
                                color = Color(0xFFDC2626),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tiêu đề: Font đậm 15sp, màu sắc phân cấp rõ ràng
                Text(
                    text = alert.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCritical) Color(0xFFDC2626) else if (isWarning) Color(0xFFB45309) else Color(0xFF0F172A),
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Nội dung thông điệp
                Text(
                    text = alert.message,
                    fontSize = 13.sp,
                    color = Color(0xFF334155),
                    lineHeight = 18.sp
                )

                // Địa chỉ thực tế từ Geocoder (Loại bỏ hoàn toàn các mã tọa độ thô)
                val cleanAddress = alert.address?.takeIf {
                    it.isNotBlank() && !it.startsWith("QPG2") && !it.startsWith("(")
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
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF475569)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Nút bấm: Chiều cao 42.dp, wrapContentHeight(), cỡ chữ 13sp, không bao giờ bị cắt chữ
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val lat = alert.latitude
                    val lng = alert.longitude
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        Button(
                            onClick = { onOpenMap(lat, lng) },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "XEM GOOGLE MAPS",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (!alert.isRead) {
                        OutlinedButton(
                            onClick = onMarkAsRead,
                            modifier = Modifier
                                .weight(if (lat != null && lng != null && lat != 0.0 && lng != 0.0) 0.9f else 1f)
                                .height(42.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đánh dấu đã xem",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.DoneAll,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Đã xem",
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
}

/**
 * Mở ứng dụng Google Maps dẫn đường trực tiếp tới tọa độ
 */
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

/**
 * Định dạng thời gian theo mốc: 15:49 - 27/10/2026
 */
fun formatAlertTime(timestamp: Long): String {
    return try {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } catch (_: Exception) {
        ""
    }
}
