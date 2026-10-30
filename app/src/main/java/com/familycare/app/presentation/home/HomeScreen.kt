package com.familycare.app.presentation.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.painterResource
import com.familycare.app.R
import com.familycare.app.core.common.SessionManager
import com.familycare.app.presentation.common.FamilyCareAmbientBackground
import com.familycare.app.presentation.schedule.formatDosageString
import com.familycare.app.presentation.schedule.formatMedicineSummaryList
import com.familycare.app.presentation.schedule.getTimeSessionLabel
import com.familycare.app.ui.theme.*
import androidx.compose.material.icons.outlined.Circle
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeScreen(
    onNavigateToVitals: () -> Unit,
    onNavigateToMedication: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToSmartwatch: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPpg: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(LocalContext.current)
    )
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showAddMemberDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sessionManager.userId, sessionManager.familyId) {
        viewModel.refreshHomeData()
    }

    // Tự động kiểm tra và làm mới dữ liệu thuốc nếu đã sang ngày mới khi người dùng mở lại app
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshHomeData()
                viewModel.checkAndRefreshIfDayChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Đồng bộ tên tổ ấm an toàn tuyệt đối
    val familyName = uiState.familyName.ifBlank {
        sessionManager.familyName.ifBlank { "Tổ ấm của chúng ta" }
    }

    // Lời chào động theo thời gian thực của thiết bị
    val greetingText = remember {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        when {
            hour < 12 -> "Chào buổi sáng!"
            hour < 18 -> "Chào buổi chiều!"
            else -> "Chào buổi tối!"
        }
    }

    FamilyCareAmbientBackground {
        Scaffold(
            containerColor = Color.Transparent,
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = 76.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = WarmTextPrimary,
                        contentColor = WarmCreamSurface,
                        actionColor = WarmAccent,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            floatingActionButton = {
                Surface(
                    shape = CircleShape,
                    color = SosRed,
                    shadowElevation = 6.dp,
                    border = BorderStroke(2.5.dp, Color.White),
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .clickable { onNavigateToSos() }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "SOS",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            },
            topBar = {
                // A. HEADER THƯƠNG HIỆU
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cụm trái: Logo FamilyCare + Tổ ấm
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Spa,
                                    contentDescription = null,
                                    tint = WarmPrimaryTeal,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "FamilyCare",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmTextPrimary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { onNavigateToNotifications() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = WarmTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (familyName.startsWith("Tổ ấm")) familyName else "Tổ ấm $familyName",
                                    color = WarmTextSecondary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = WarmTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Cụm phải: Nút chuông thông báo tròn
                        Surface(
                            shape = CircleShape,
                            color = WarmCreamSurface,
                            border = BorderStroke(1.dp, WarmDivider),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onNavigateToNotifications() }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (uiState.unreadNotificationCount > 0) {
                                            Badge(
                                                containerColor = SosRed,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    text = if (uiState.unreadNotificationCount > 99) "99+" else "${uiState.unreadNotificationCount}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsNone,
                                        contentDescription = "Thông báo",
                                        tint = WarmTextPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
            ) {
                // B. LỜI CHÀO & MINH HỌA NGÔI NHÀ TINH TẾ
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = greetingText,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary,
                                letterSpacing = (-0.3).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Cùng nhau chăm sóc sức khỏe\ncho gia đình nhé.",
                                fontSize = 14.sp,
                                color = WarmTextSecondary,
                                lineHeight = 20.sp
                            )
                        }

                        // Họa tiết trang trí ngôi nhà & mặt trời dịu nhẹ (Editorial Vector)
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 75.dp)
                                .padding(start = 8.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height

                                // Mặt trời vàng ấm mờ
                                drawCircle(
                                    color = WarmAccent.copy(alpha = 0.35f),
                                    radius = 14.dp.toPx(),
                                    center = Offset(w * 0.45f, h * 0.28f)
                                )

                                // Tán cây xanh mờ phía sau
                                drawCircle(
                                    color = WarmPrimaryLight.copy(alpha = 0.8f),
                                    radius = 20.dp.toPx(),
                                    center = Offset(w * 0.22f, h * 0.65f)
                                )
                                drawCircle(
                                    color = WarmPrimaryLight.copy(alpha = 0.9f),
                                    radius = 18.dp.toPx(),
                                    center = Offset(w * 0.85f, h * 0.55f)
                                )

                                // Mái nhà
                                val roofPath = androidx.compose.ui.graphics.Path().apply {
                                    moveTo(w * 0.38f, h * 0.52f)
                                    lineTo(w * 0.64f, h * 0.36f)
                                    lineTo(w * 0.86f, h * 0.52f)
                                    close()
                                }
                                drawPath(roofPath, color = WarmPrimaryTeal.copy(alpha = 0.85f))

                                // Tường nhà
                                drawRect(
                                    color = WarmCreamSurface,
                                    topLeft = Offset(w * 0.42f, h * 0.52f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.40f, h * 0.36f)
                                )
                                // Cửa nhà
                                drawRect(
                                    color = WarmPrimaryTeal.copy(alpha = 0.6f),
                                    topLeft = Offset(w * 0.56f, h * 0.65f),
                                    size = androidx.compose.ui.geometry.Size(w * 0.12f, h * 0.23f)
                                )
                            }
                        }
                    }
                }

                // C. DANH SÁCH THÀNH VIÊN TRONG GIA ĐÌNH
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Thành viên trong gia đình",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarmTextPrimary
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(uiState.familyMembers, key = { it.id }) { member ->
                                val isSelected = member.id == uiState.selectedMemberId
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isSelected) WarmPrimaryLight else WarmCreamSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) WarmPrimaryTeal.copy(alpha = 0.4f) else WarmDivider
                                    ),
                                    shadowElevation = if (isSelected) 0.5.dp else 0.dp,
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { viewModel.onMemberSelect(member.id) }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        // Avatar hình tròn thon gọn
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (isSelected) Color.White else WarmPrimaryLight
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = member.initials,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                color = WarmPrimaryTeal
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(9.dp))

                                        Column(
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            Text(
                                                text = member.fullName,
                                                fontSize = 13.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = WarmTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(1.dp))
                                            Text(
                                                text = member.role,
                                                fontSize = 11.sp,
                                                color = WarmTextSecondary,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }

                            // Ô Thêm thành viên thon gọn
                            item {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, WarmDivider),
                                    modifier = Modifier
                                        .height(56.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showAddMemberDialog = true }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .clip(CircleShape)
                                                .background(WarmCreamSecondary),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Thêm thành viên",
                                                tint = WarmTextSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(7.dp))
                                        Text(
                                            text = "Thêm",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WarmTextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // D. TỔNG QUAN SỨC KHỎE (VÙNG NỀN XANH SAGE NHẠT NHẸ NHÀNG)
                item {
                    val alert = uiState.alertStatus
                    val vitals = uiState.latestVitals
                    val hasMeasuredBp = vitals?.hasMeasuredBp == true
                    val hasMeasuredHr = vitals?.hasMeasuredHr == true
                    val hasAnyMeasurement = hasMeasuredBp || hasMeasuredHr
                    val isDataStale = vitals != null && hasAnyMeasurement &&
                            (System.currentTimeMillis() - vitals.lastUpdated > 24 * 60 * 60 * 1000L)

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (alert != null) SosRedLight else WarmPrimaryLight,
                        border = BorderStroke(
                            1.dp,
                            if (alert != null) SosRedBorder else Color(0xFFD6E4D2)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToVitals() }
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
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (alert != null) SosRed else WarmPrimaryTeal
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (alert != null) Icons.Default.Warning else Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tổng quan sức khỏe",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (alert != null) SosRed else WarmTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            alert != null -> alert.title
                                            !hasAnyMeasurement -> "Chưa có đủ dữ liệu hôm nay"
                                            isDataStale -> "Số đo chưa được cập nhật hôm nay"
                                            else -> "Sức khỏe ghi nhận ổn định"
                                        },
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (alert != null) SosRed else WarmTextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            alert != null -> alert.message
                                            !hasAnyMeasurement -> "Hãy cập nhật chỉ số để theo dõi tình trạng sức khỏe chính xác hơn."
                                            isDataStale -> "Lần đo gần nhất đã hơn 24 giờ. Cần đo lại để theo dõi chuẩn xác."
                                            else -> "Chỉ số sinh tồn của thành viên trong ngưỡng an toàn."
                                        },
                                        fontSize = 12.sp,
                                        color = if (alert != null) SosRed.copy(alpha = 0.85f) else WarmTextSecondary,
                                        lineHeight = 17.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Nút mũi tên tròn
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (alert != null) SosRed else WarmPrimaryTeal),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Xem chi tiết",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // E. BỐN CHỈ SỐ SỨC KHỎE — BẢNG EDITORIAL 2×2 NẰM TRỰC TIẾP TRÊN NỀN (KHÔNG CARD RIÊNG)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chỉ số sức khỏe",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToVitals() }
                                    .padding(4.dp)
                            ) {
                                Text(
                                    text = "Xem lịch sử",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WarmPrimaryTeal
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = WarmPrimaryTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        val vitals = uiState.latestVitals
                        val hasBp = vitals?.hasMeasuredBp == true
                        val hasHr = vitals?.hasMeasuredHr == true
                        val hasTemp = vitals?.hasMeasuredTemp == true
                        val hasSugar = vitals?.hasMeasuredSugar == true

                        val lastTimeFormatted = remember(vitals?.lastUpdated) {
                            vitals?.lastUpdated?.let {
                                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                            } ?: ""
                        }

                        // Khối bảng Editorial 2x2 ngăn cách bằng đường kẻ mảnh
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = WarmCreamSurface,
                            border = BorderStroke(1.dp, WarmDivider),
                            shadowElevation = 0.5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                // HÀNG 1: HUYẾT ÁP (Trái) & NHỊP TIM (Phải)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // 1. Huyết áp
                                    EditorialMetricCell(
                                        title = "Huyết áp",
                                        value = if (hasBp) "${vitals?.bloodPressureSystolic}/${vitals?.bloodPressureDiastolic}" else "--/--",
                                        unit = "mmHg",
                                        status = if (hasBp) (if (vitals?.isBloodPressureWarning == true) "Tăng HA" else "Bình thường") else "Chưa đo hôm nay",
                                        isWarning = vitals?.isBloodPressureWarning == true,
                                        icon = Icons.Default.Speed,
                                        iconBg = Color(0xFFF9EAE1),
                                        iconTint = WarmAccent,
                                        onClick = onNavigateToVitals,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Vạch kẻ dọc 1
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(WarmDivider)
                                    )

                                    // 2. Nhịp tim
                                    EditorialMetricCell(
                                        title = "Nhịp tim",
                                        value = if (hasHr) "${vitals?.heartRateBpm}" else "--",
                                        unit = "BPM",
                                        status = if (hasHr) (if (lastTimeFormatted.isNotBlank()) "Dữ liệu gần nhất: $lastTimeFormatted" else "Bình thường") else "Chưa đo hôm nay",
                                        isWarning = vitals?.isHeartRateWarning == true,
                                        icon = Icons.Default.Favorite,
                                        iconBg = Color(0xFFFCEAEB),
                                        iconTint = HeartRateAccent,
                                        onClick = onNavigateToPpg,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                // Vạch kẻ ngang giữa 2 hàng
                                HorizontalDivider(color = WarmDivider, thickness = 1.dp)

                                // HÀNG 2: NHIỆT ĐỘ CƠ THỂ (Trái) & ĐƯỜNG HUYẾT (Phải)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    // 3. Nhiệt độ cơ thể
                                    EditorialMetricCell(
                                        title = "Nhiệt độ cơ thể",
                                        value = if (hasTemp) String.format(Locale.US, "%.1f", vitals?.temperature ?: 0.0) else "--,-",
                                        unit = "°C",
                                        status = if (hasTemp) "Lúc $lastTimeFormatted" else "Chưa có dữ liệu",
                                        isWarning = false,
                                        icon = Icons.Default.Thermostat,
                                        iconBg = Color(0xFFF8EEDF),
                                        iconTint = TemperatureAccent,
                                        onClick = onNavigateToVitals,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Vạch kẻ dọc 2
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .fillMaxHeight()
                                            .background(WarmDivider)
                                    )

                                    // 4. Đường huyết
                                    EditorialMetricCell(
                                        title = "Đường huyết",
                                        value = if (hasSugar) String.format(Locale.US, "%.1f", vitals?.bloodSugar ?: 0.0) else "--,-",
                                        unit = "mmol/L",
                                        status = if (hasSugar) "Lúc $lastTimeFormatted" else "Chưa có dữ liệu",
                                        isWarning = false,
                                        icon = Icons.Default.WaterDrop,
                                        iconBg = Color(0xFFE8EEF8),
                                        iconTint = BloodGlucoseAccent,
                                        onClick = onNavigateToVitals,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // F. VIỆC CẦN LÀM HÔM NAY (DANH SÁCH DÒNG VỚI DIVIDER MẢNH)
                item {
                    val todayAppt = uiState.todayAppointment
                    val medications = uiState.todayMedications
                    val totalTasks = medications.size + (if (todayAppt != null) 1 else 0)
                    val pendingCount = medications.count { !it.isTaken && !it.isSkipped }
                    val allDone = medications.isNotEmpty() && pendingCount == 0

                    val groupedSessions = remember(medications) {
                        medications.groupBy { it.scheduledTime }.toList().sortedBy { it.first }
                    }
                    val pendingSessionsCount = groupedSessions.count { (_, doses) ->
                        doses.any { it.status == DoseStatus.DUE || it.status == DoseStatus.OVERDUE }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f, fill = false)
                            ) {
                                Text(
                                    text = "Việc cần làm hôm nay",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmTextPrimary,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (allDone) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WarmPrimaryLight,
                                        border = BorderStroke(0.8.dp, WarmPrimaryTeal.copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = WarmPrimaryTeal,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = "Hoàn thành",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarmPrimaryTeal
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onNavigateToMedication() }
                                    .padding(vertical = 4.dp, horizontal = 2.dp)
                            ) {
                                Text(
                                    text = "Xem tất cả",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = WarmPrimaryTeal,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = WarmPrimaryTeal,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        if (totalTasks == 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = WarmCreamSurface,
                                border = BorderStroke(1.dp, WarmDivider),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(18.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Chưa có việc cần làm hôm nay",
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = WarmTextPrimary
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "Thêm thuốc hoặc đặt lịch khám để nhận lời nhắc",
                                            fontSize = 12.5.sp,
                                            color = WarmTextSecondary,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Button(
                                        onClick = onNavigateToMedication,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Text("+ Thêm thuốc", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Danh sách các cữ thuốc hôm nay (Mỗi cữ là 1 Card độc lập, tinh gọn)
                                groupedSessions.forEach { (scheduledTime, doses) ->
                                    GroupedSessionCard(
                                        scheduledTime = scheduledTime,
                                        doses = doses,
                                        onTakeSession = {
                                            viewModel.onTakeSession(scheduledTime)
                                        },
                                        onUndoTakeSession = {
                                            viewModel.onUndoTakeSession(scheduledTime)
                                        }
                                    )
                                }

                                // 2. Lịch khám định kỳ diễn ra TRONG NGÀY HÔM NAY (nếu có)
                                if (todayAppt != null) {
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = WarmCreamSurface,
                                        border = BorderStroke(1.dp, WarmDivider),
                                        shadowElevation = 0.5.dp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigateToMedication() }
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(38.dp)
                                                        .clip(CircleShape)
                                                        .background(WarmPrimaryLight),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarMonth,
                                                        contentDescription = null,
                                                        tint = WarmPrimaryTeal,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }

                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = todayAppt.title,
                                                        fontSize = 14.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = WarmTextPrimary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = "${todayAppt.timeFormatted} · ${todayAppt.doctorOrDepartment}",
                                                        fontSize = 12.sp,
                                                        color = WarmTextSecondary,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = WarmTextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // G. LỊCH HẸN KHÁM SẮP TỚI (Tương lai / Ngày mai - Tách riêng khối độc lập)
                if (uiState.upcomingAppointment != null) {
                    item {
                        val upcoming = uiState.upcomingAppointment!!
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f, fill = false)
                                ) {
                                    Text(
                                        text = "Lịch hẹn sắp tới",
                                        fontSize = 15.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = WarmTextPrimary,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = WarmPrimaryLight,
                                        border = BorderStroke(0.8.dp, WarmPrimaryTeal.copy(alpha = 0.3f))
                                    ) {
                                        Text(
                                            text = upcoming.timeFormatted.split(",").firstOrNull()?.trim() ?: "Sắp tới",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = WarmPrimaryTeal,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { onNavigateToMedication() }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = "Chi tiết",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = WarmPrimaryTeal,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = null,
                                        tint = WarmPrimaryTeal,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = WarmCreamSurface,
                                border = BorderStroke(1.dp, WarmDivider),
                                shadowElevation = 0.5.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToMedication() }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(WarmPrimaryLight),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = WarmPrimaryTeal,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = upcoming.title,
                                                fontSize = 14.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = WarmTextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${upcoming.timeFormatted} · ${upcoming.doctorOrDepartment}",
                                                fontSize = 12.sp,
                                                color = WarmTextSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = WarmTextSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showAddMemberDialog) {
            AddMemberHomeDialog(
                onDismiss = { showAddMemberDialog = false },
                onAdd = { fullName, relationship ->
                    viewModel.addNewMember(fullName, relationship)
                },
                inviteCode = uiState.inviteCode.ifBlank { "FAM-8606" }
            )
        }
    }
}

/**
 * Ô thành phần chỉ số trong bảng Editorial 2x2 (Phong cách A)
 * Nằm trực tiếp trên bề mặt bảng, ngăn cách bằng đường kẻ mảnh, touch target >= 48dp
 */
@Composable
private fun EditorialMetricCell(
    title: String,
    value: String,
    unit: String,
    status: String,
    isWarning: Boolean,
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hàng 1: Icon tròn + Mũi tên góc phải
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = WarmTextSecondary.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Hàng 2: Tên chỉ số
            Text(
                text = title,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Medium,
                color = WarmTextPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Hàng 3: Giá trị lớn & đơn vị
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isWarning) SosRed else WarmTextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 11.5.sp,
                    color = WarmTextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Hàng 4: Trạng thái đo / thời gian
            Text(
                text = status,
                fontSize = 11.sp,
                color = if (isWarning) SosRed else WarmTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Thẻ Cữ Gộp: Hiển thị một cữ uống gồm các loại thuốc trong cùng cữ giờ.
 * Thiết kế phẳng, độc lập, tự động gom nhóm các thuốc có cùng liều dùng (ví dụ: "abc, abcd (1 viên)"),
 * hiển thị thẳng dưới giờ cữ và có 1 nút bấm "Đã uống cả cữ".
 */
@Composable
fun GroupedSessionCard(
    scheduledTime: String,
    doses: List<MedicationSchedule>,
    onTakeSession: () -> Unit,
    onUndoTakeSession: () -> Unit
) {
    val sessionLabel = getTimeSessionLabel(scheduledTime)
    val allTaken = doses.all { it.status == DoseStatus.TAKEN }
    val anyOverdue = doses.any { it.status == DoseStatus.OVERDUE }
    val anyDue = doses.any { it.status == DoseStatus.DUE }
    val allSkipped = doses.all { it.status == DoseStatus.SKIPPED }

    val sessionStatus = when {
        allTaken -> DoseStatus.TAKEN
        anyOverdue -> DoseStatus.OVERDUE
        anyDue -> DoseStatus.DUE
        allSkipped -> DoseStatus.SKIPPED
        else -> DoseStatus.UPCOMING
    }

    val (iconBg, iconTint) = when (sessionStatus) {
        DoseStatus.TAKEN -> Pair(WarmPrimaryLight, WarmPrimaryTeal)
        DoseStatus.OVERDUE -> Pair(Color(0xFFFDF4E7), WarmAccent)
        DoseStatus.DUE -> Pair(Color(0xFFE8F3EE), WarmPrimaryTeal)
        DoseStatus.UPCOMING -> Pair(WarmCreamSecondary, WarmTextSecondary)
        DoseStatus.SKIPPED -> Pair(Color(0xFFF1EFEA), WarmTextSecondary.copy(alpha = 0.6f))
    }

    // Tự động gom các thuốc có cùng liều lượng: ví dụ "Thuốc A, B (1 viên)"
    val groupedDoses = remember(doses) {
        doses.groupBy { formatDosageString(it.dosage) }
    }

    val commonInstruction = remember(doses) {
        doses.map { it.instruction.trim() }.firstOrNull { it.isNotBlank() } ?: ""
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (allTaken) Color(0xFFFBFDFB) else WarmCreamSurface,
        border = BorderStroke(
            1.dp,
            if (allTaken) WarmPrimaryTeal.copy(alpha = 0.35f) else WarmDivider
        ),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // TẦNG 1: HEADER (Icon + Giờ + Badge Quá giờ/Đến giờ --- Nút [✓ Đã uống])
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (sessionStatus == DoseStatus.TAKEN) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_pill),
                                contentDescription = "Cữ thuốc",
                                tint = iconTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "$scheduledTime ($sessionLabel)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionStatus == DoseStatus.SKIPPED) WarmTextSecondary else WarmTextPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (sessionStatus == DoseStatus.DUE) {
                            Surface(shape = RoundedCornerShape(4.dp), color = WarmPrimaryLight) {
                                Text(
                                    text = "Đến giờ",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmPrimaryTeal,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        } else if (sessionStatus == DoseStatus.OVERDUE) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFFDF4E7)) {
                                Text(
                                    text = "Quá giờ",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmAccent,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Nút thao tác nhanh
                if (anyDue || anyOverdue || sessionStatus == DoseStatus.UPCOMING) {
                    Button(
                        onClick = onTakeSession,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (anyOverdue) WarmAccent else WarmPrimaryTeal,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Đã uống",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                } else if (allTaken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(6.dp), color = WarmPrimaryLight) {
                            Text(
                                text = "✅ Đã uống",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmPrimaryTeal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Hoàn tác",
                            fontSize = 11.5.sp,
                            color = WarmTextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onUndoTakeSession() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // TẦNG 2: DANH SÁCH THUỐC (100% BỀ NGANG, MỖI NHÓM LIỀU 1 DÒNG RÕ RÀNG)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 46.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                groupedDoses.forEach { (dosage, items) ->
                    val names = items.map {
                        val clean = it.medicineName.trim()
                        if (clean.startsWith("Thuốc ", ignoreCase = true)) {
                            clean.substring(6).trim()
                        } else if (clean.startsWith("thuốc ", ignoreCase = true)) {
                            clean.substring(6).trim()
                        } else clean
                    }.filter { it.isNotBlank() }.distinct().joinToString(", ")

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "•",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (allTaken) WarmPrimaryTeal else WarmAccent,
                            modifier = Modifier.padding(top = 1.dp, end = 6.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Thuốc $names",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (allTaken) WarmTextSecondary else WarmTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Liều dùng: $dosage",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = WarmTextSecondary
                            )
                        }
                    }
                }
            }

            // TẦNG 3: LỜI DẶN CHUNG (NẾU CÓ)
            if (commonInstruction.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 46.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(WarmCreamSecondary.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = WarmPrimaryTeal,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Lời dặn: $commonInstruction",
                        fontSize = 12.sp,
                        color = WarmTextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Hộp thoại thêm thành viên gia đình trực tiếp tại Dashboard:
 * Hỗ trợ 2 chế độ:
 * 1. Thêm hồ sơ trực tiếp (Bà Nội, Con nhỏ...)
 * 2. Mời qua Mã tổ ấm
 */
@Composable
fun AddMemberHomeDialog(
    onDismiss: () -> Unit,
    onAdd: (fullName: String, relationship: String) -> Unit,
    inviteCode: String
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Thêm trực tiếp, 1: Mời qua mã
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Thành viên") }

    val relationshipOptions = listOf("Vợ / Chồng", "Bố", "Mẹ", "Con cái", "Thành viên")

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = WarmCreamSurface,
        title = {
            Column {
                Text(
                    text = "Thành Viên Gia Đình",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = WarmTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                // Tab chuyển đổi 2 chế độ
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = WarmCreamSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 0) WarmCreamSurface else Color.Transparent,
                            shadowElevation = if (selectedTab == 0) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 0 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Thêm trực tiếp",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 0) WarmPrimaryTeal else WarmTextSecondary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedTab == 1) WarmCreamSurface else Color.Transparent,
                            shadowElevation = if (selectedTab == 1) 1.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedTab = 1 }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Mời qua mã",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selectedTab == 1) WarmPrimaryTeal else WarmTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        },
        text = {
            if (selectedTab == 0) {
                // Tùy chọn 1: Thêm trực tiếp hồ sơ người thân
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Tạo hồ sơ theo dõi sức khỏe cho người thân (ông bà, con nhỏ không có điện thoại riêng):",
                        fontSize = 12.sp,
                        color = WarmTextSecondary,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Họ và tên *") },
                        placeholder = { Text("Ví dụ: Bà Nội, Bé Bắp...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "Mối quan hệ:",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmTextPrimary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(relationshipOptions) { rel ->
                            FilterChip(
                                selected = relationship == rel,
                                onClick = { relationship = rel },
                                label = { Text(rel, fontSize = 11.5.sp) }
                            )
                        }
                    }
                }
            } else {
                // Tùy chọn 2: Mời qua mã tổ ấm
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "Gửi mã này cho người thân có điện thoại riêng. Khi tải app xong, chọn 'Tham gia tổ ấm bằng mã':",
                        fontSize = 12.sp,
                        color = WarmTextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarmPrimaryLight,
                        border = BorderStroke(1.dp, WarmPrimaryTeal.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 14.dp)
                        ) {
                            Text(
                                text = "MÃ TỔ ẤM CỦA BẠN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmPrimaryTeal.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = inviteCode,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp,
                                color = WarmPrimaryTeal
                            )
                        }
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clip = ClipData.newPlainText("Mã tổ ấm FamilyCare", inviteCode)
                            clipboard?.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép mã: $inviteCode", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sao chép mã mời", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            if (selectedTab == 0) {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            Toast.makeText(context, "Vui lòng nhập họ và tên", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onAdd(name.trim(), relationship)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Text("Lưu thành viên", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng", color = WarmTextSecondary)
            }
        }
    )
}
