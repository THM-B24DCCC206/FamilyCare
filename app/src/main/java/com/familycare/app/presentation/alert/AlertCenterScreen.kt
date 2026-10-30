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
import com.familycare.app.presentation.common.FamilyCareAmbientBackground
import com.familycare.app.ui.theme.*
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

    FamilyCareAmbientBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Trung Tâm Cảnh Báo",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = WarmTextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Quay lại",
                                tint = WarmTextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.clearAllAlerts() }) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Xóa sạch tất cả cảnh báo",
                                tint = WarmTextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
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
                                    color = if (isSelected) WarmPrimaryTeal else WarmTextSecondary,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = WarmCreamSurface,
                                selectedContainerColor = WarmPrimaryLight,
                                labelColor = WarmTextSecondary,
                                selectedLabelColor = WarmPrimaryTeal
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) WarmPrimaryTeal else WarmDivider,
                                enabled = true,
                                selected = isSelected
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
                                    color = if (isSelected) SosRed else WarmTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = WarmCreamSurface,
                                selectedContainerColor = SosRedLight,
                                labelColor = WarmTextSecondary,
                                selectedLabelColor = SosRed
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) SosRedBorder else WarmDivider,
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
                                    color = if (isSelected) HealthWarning else WarmTextSecondary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    modifier = Modifier.wrapContentWidth()
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = WarmCreamSurface,
                                selectedContainerColor = HealthWarningLight,
                                labelColor = WarmTextSecondary,
                                selectedLabelColor = HealthWarning
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) HealthWarningBorder else WarmDivider,
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
                                tint = WarmPrimaryTeal,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Không có cảnh báo bất thường",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Các chỉ số sức khỏe của gia đình đang ở vùng an toàn.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WarmTextSecondary
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
                                onDelete = { viewModel.onDeleteAlert(alert.id) },
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
}

@Composable
fun AlertItemCard(
    alert: AlertEntity,
    onMarkAsRead: () -> Unit,
    onDelete: () -> Unit,
    onOpenMap: (Double, Double) -> Unit
) {
    val isCritical = alert.alertLevel == AlertLevel.CRITICAL
    val isWarning = alert.alertLevel == AlertLevel.WARNING
    val isAppointment = alert.alertType == com.familycare.app.domain.model.AlertType.APPOINTMENT_REMINDER ||
            alert.title.contains("lịch khám", ignoreCase = true)

    // Giải pháp 1: Khi đã xem, dùng nền ngà đặc 100% Color(0xFFF9F7F1) (không dùng alpha)
    // và triệt tiêu hoàn toàn bóng RenderNode (0.dp) để chống vệt đen viền lem nhem
    val cardBg = if (alert.isRead) Color(0xFFF9F7F1) else WarmCreamSurface
    val cardBorder = when {
        !alert.isRead && isCritical -> BorderStroke(1.5.dp, SosRedBorder)
        !alert.isRead && isAppointment -> BorderStroke(1.2.dp, WarmPrimaryTeal.copy(alpha = 0.4f))
        !alert.isRead && isWarning -> BorderStroke(1.5.dp, HealthWarningBorder)
        else -> BorderStroke(1.dp, WarmDivider)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(defaultElevation = if (alert.isRead) 0.dp else 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // 1. Dòng thông tin trên cùng: "Tên thành viên • Giờ - Ngày", Huy hiệu [ MỚI ] và Nút Xóa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${alert.memberName.ifBlank { "Thành viên gia đình" }} • ${formatAlertTime(alert.timestamp)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = WarmTextSecondary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!alert.isRead) {
                        val badgeBg = if (isCritical) SosRedLight else if (isAppointment) WarmPrimaryLight else HealthWarningLight
                        val badgeTextColor = if (isCritical) SosRed else if (isAppointment) WarmPrimaryTeal else HealthWarning
                        Surface(
                            color = badgeBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isAppointment) "LỊCH KHÁM" else "MỚI",
                                color = badgeTextColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Xóa thông báo này",
                            tint = WarmTextSecondary.copy(alpha = 0.6f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 2. Tiêu đề cảnh báo
            val displayTitle = when {
                isAppointment -> alert.title
                isCritical -> "🚨 Báo động SOS: ${alert.memberName.ifBlank { "Người thân" }}"
                alert.title.trim() == "🚨" || alert.title.isBlank() -> "🚨 Báo động SOS: ${alert.memberName.ifBlank { "Người thân" }}"
                else -> alert.title
            }

            val titleColor = when {
                alert.isRead -> WarmTextSecondary
                isCritical -> SosRed
                isAppointment -> WarmPrimaryTeal
                isWarning -> HealthWarning
                else -> WarmTextPrimary
            }

            Text(
                text = displayTitle,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 3. Nội dung mô tả
            val displayMessage = if (isAppointment) {
                alert.message
            } else if (isCritical) {
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
                color = if (alert.isRead) WarmTextSecondary else WarmTextPrimary.copy(alpha = 0.85f),
                lineHeight = 19.sp
            )

            // 4. Địa chỉ duy nhất bên dưới với icon ghim định vị
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
                        tint = WarmAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = cleanAddress,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = WarmTextSecondary,
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
                    if (!alert.isRead) {
                        Button(
                            onClick = { onOpenMap(lat, lng) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCritical) SosRed else WarmPrimaryTeal
                            ),
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
                        OutlinedButton(
                            onClick = { onOpenMap(lat, lng) },
                            modifier = Modifier
                                .weight(1f)
                                .height(36.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, WarmDivider),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WarmTextSecondary)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(14.dp), tint = WarmTextSecondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Xem Bản Đồ",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = WarmTextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (!alert.isRead) {
                    OutlinedButton(
                        onClick = onMarkAsRead,
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        border = BorderStroke(1.dp, WarmDivider),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = WarmTextSecondary
                        )
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = WarmTextSecondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đánh dấu đã xem",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarmTextSecondary,
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
                            tint = WarmPrimaryTeal,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đã xử lý",
                            fontSize = 12.sp,
                            color = WarmPrimaryTeal,
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
