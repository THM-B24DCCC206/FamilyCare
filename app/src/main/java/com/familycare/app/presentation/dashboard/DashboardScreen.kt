package com.familycare.app.presentation.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import com.familycare.app.domain.model.HealthMetric
import com.familycare.app.domain.model.Medicine
import com.familycare.app.domain.model.User
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (sessionManager.hasFamily) "Tá»• áº¥m: " + sessionManager.familyName else "KhÃ´ng gian gia Ä‘Ã¬nh thÃ´ng minh",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Nút chuông cảnh báo có huy hiệu đỏ nếu có cảnh báo khẩn cấp
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
                                contentDescription = "Cảnh báo khẩn cấp",
                                tint = if (uiState.activeRedAlert != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val status = uiState.status) {
                is DashboardUiStatus.Loading -> {
                    DashboardLoadingView()
                }
                is DashboardUiStatus.Error -> {
                    DashboardErrorView(
                        errorMessage = status.errorMessage,
                        onRetry = { viewModel.retry() }
                    )
                }
                is DashboardUiStatus.Success -> {
                    DashboardContentView(
                        uiState = uiState,
                        onSelectMember = viewModel::onSelectMember,
                        onMarkMedicineTaken = viewModel::markMedicineTaken,
                        onNavigateToEmergency = onNavigateToEmergency,
                        onNavigateToSos = onNavigateToSos,
                        onNavigateToPpg = onNavigateToPpg,
                        onNavigateToAi = onNavigateToAi,
                        onNavigateToOcr = onNavigateToOcr
                    )
                }
            }
        }
    }
}

/**
 * GIAO DIỆN CHÍNH CỦA DASHBOARD (TRẠNG THÁI SUCCESS)
 */
@Composable
fun DashboardContentView(
    uiState: DashboardUiState,
    onSelectMember: (User) -> Unit,
    onMarkMedicineTaken: () -> Unit,
    onNavigateToEmergency: () -> Unit,
    onNavigateToSos: () -> Unit,
    onNavigateToPpg: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToOcr: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp)
    ) {
        // 1. THANH CHUYỂN ĐỔI THÀNH VIÊN (MEMBER SWITCHER)
        item {
            MemberSwitcherBar(
                members = uiState.familyMembers,
                selectedMember = uiState.selectedMember,
                onSelectMember = onSelectMember
            )
        }

        // 2. THẺ CẢNH BÁO ĐỎ VƯỢT NGƯỠNG (NẾU CÓ CHUỖI VƯỢT NGƯỠNG HOẶC SOS)
        if (uiState.activeRedAlert != null) {
            item {
                CriticalRedAlertCard(
                    alertTitle = uiState.activeRedAlert.title,
                    alertMessage = uiState.activeRedAlert.message,
                    consecutiveBreachCount = uiState.consecutiveBreachCount,
                    onViewAlert = onNavigateToEmergency
                )
            }
        }

        // 3. NÚT BẤM SOS KHẨN CẤP TO BẢN (CHIỀU CAO ≥ 56.DP)
        item {
            LargeEmergencyPanicCard(onNavigateToEmergency = onNavigateToSos)
        }

        // 4. THẺ TÓM TẮT CHỈ SỐ SINH TỒN (HUYẾT ÁP & NHỊP TIM)
        item {
            VitalSignsSummaryCard(
                bloodPressure = uiState.latestBloodPressure,
                heartRate = uiState.latestHeartRate,
                onMeasureNow = onNavigateToPpg
            )
        }

        // 5. THẺ LỊCH UỐNG THUỐC HÔM NAY VỚI NÚT "ĐÃ UỐNG" TO BẢN (≥ 56.DP)
        item {
            MedicationScheduleCard(
                medicine = uiState.upcomingMedicine,
                isTaken = uiState.isMedicineTakenToday,
                complianceRate = uiState.complianceRatePercent,
                onMarkTaken = onMarkMedicineTaken,
                onScanOcr = onNavigateToOcr
            )
        }

        // 6. PHÍM TẮT TÍNH NĂNG THÔNG MINH (ĐO QUANG HỌC PPG & TRỢ LÝ AI GEMINI)
        item {
            SmartFeatureShortcuts(
                onNavigateToPpg = onNavigateToPpg,
                onNavigateToAi = onNavigateToAi
            )
        }
    }
}

/**
 * 1. Thanh chuyển đổi thành viên ngang (Member Switcher)
 */
@Composable
fun MemberSwitcherBar(
    members: List<User>,
    selectedMember: User?,
    onSelectMember: (User) -> Unit
) {
    Column {
        Text(
            text = "Thành viên đang theo dõi:",
            style = MaterialTheme.typography.titleMedium, // 20sp Bold
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(members, key = { it.id }) { member ->
                val isSelected = member.id == selectedMember?.id
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

                Surface(
                    shape = RoundedCornerShape(24.dp), // Quy chuẩn bo góc 24dp
                    color = bgColor,
                    shadowElevation = if (isSelected) 3.dp else 1.dp,
                    modifier = Modifier
                        .border(width = 2.dp, color = borderColor, shape = RoundedCornerShape(24.dp))
                        .clickable { onSelectMember(member) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.fullName.take(1),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = member.fullName,
                                style = MaterialTheme.typography.bodyLarge, // 17sp
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (member.id == "user_demo_01") "68 tuổi • Cao huyết áp" else "Sức khỏe ổn định",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 2. Thẻ Cảnh báo đỏ vượt ngưỡng (Red Alert Card)
 */
@Composable
fun CriticalRedAlertCard(
    alertTitle: String,
    alertMessage: String,
    consecutiveBreachCount: Int,
    onViewAlert: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Cảnh báo khẩn cấp",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = alertTitle,
                    style = MaterialTheme.typography.titleMedium, // 20sp Bold
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = alertMessage,
                style = MaterialTheme.typography.bodyLarge, // 17sp dễ đọc cho người già
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onViewAlert,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Chuẩn nút bấm cao ≥ 56dp
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
            ) {
                Text(
                    text = "XỬ LÝ CẢNH BÁO NGAY",
                    style = MaterialTheme.typography.labelLarge // 16sp Bold
                )
            }
        }
    }
}

/**
 * 3. Nút bấm SOS Khẩn Cấp To Bản (Large Emergency Panic Card)
 */
@Composable
fun LargeEmergencyPanicCard(onNavigateToEmergency: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.error,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToEmergency() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NÚT KHẨN CẤP SOS 1-CHẠM",
                    style = MaterialTheme.typography.titleMedium, // 20sp Bold
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nhấn giữ 2s để hú còi và gửi định vị GPS cho con cái",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "SOS",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

/**
 * 4. Thẻ tóm tắt chỉ số sinh tồn (Vital Signs Summary Card)
 */
@Composable
fun VitalSignsSummaryCard(
    bloodPressure: HealthMetric?,
    heartRate: HealthMetric?,
    onMeasureNow: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp), // 24dp góc bo chuẩn
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Chỉ số sinh tồn gần nhất",
                    style = MaterialTheme.typography.titleMedium, // 20sp Bold
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onMeasureNow) {
                    Text(
                        text = "Đo mới +",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Khối đo huyết áp
            val sys = bloodPressure?.valuePrimary?.toInt() ?: 145
            val dia = bloodPressure?.valueSecondary?.toInt() ?: 92
            val isBpHigh = sys >= 140 || dia >= 90

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isBpHigh) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isBpHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Huyết áp (Hôm nay)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isBpHigh) "Tăng huyết áp độ 1 (Vượt ngưỡng)" else "Bình thường lý tưởng",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isBpHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                    Text(
                        text = "$sys/$dia mmHg",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isBpHigh) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Khối đo nhịp tim
            val bpm = heartRate?.valuePrimary?.toInt() ?: 86
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE91E63)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Nhịp tim lúc nghỉ (PPG)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Đo bằng Camera & Flash",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Text(
                        text = "$bpm BPM",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 5. Thẻ lịch uống thuốc hôm nay với nút "ĐÃ UỐNG" to bản (≥ 56dp)
 */
@Composable
fun MedicationScheduleCard(
    medicine: Medicine?,
    isTaken: Boolean,
    complianceRate: Int,
    onMarkTaken: () -> Unit,
    onScanOcr: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lịch uống thuốc hôm nay",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "Tuân thủ: $complianceRate%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chi tiết cữ thuốc
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = medicine?.name ?: "Amlodipine 5mg",
                            style = MaterialTheme.typography.bodyLarge, // 17sp
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Liều dùng: ${medicine?.dosage ?: "1 viên"} • Sau bữa ăn",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Text(
                            text = "12:00",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // NÚT HÀNH ĐỘNG CHÍNH TO BẢN: CHIỀU CAO TỐI THIỂU 56.DP
            Button(
                onClick = onMarkTaken,
                enabled = !isTaken,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp), // Quy chuẩn Design System ≥ 56dp
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFE0E0E0),
                    disabledContentColor = Color(0xFF757575)
                )
            ) {
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = if (isTaken) "ĐÃ XÁC NHẬN UỐNG THUỐC" else "XÁC NHẬN ĐÃ UỐNG THUỐC",
                    style = MaterialTheme.typography.labelLarge // 16sp Bold
                )
            }
        }
    }
}

/**
 * 6. Phím tắt tính năng thông minh (Đo nhịp tim PPG & AI Gemini Copilot)
 */
@Composable
fun SmartFeatureShortcuts(
    onNavigateToPpg: () -> Unit,
    onNavigateToAi: () -> Unit
) {
    Column {
        Text(
            text = "Công cụ thông minh",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Nút mở đo PPG
            ElevatedCard(
                onClick = onNavigateToPpg,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Đo PPG",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Đo Nhịp Tim PPG",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Nút mở AI Copilot
            ElevatedCard(
                onClick = onNavigateToAi,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(120.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Trợ lý AI",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Bác Sĩ Ảo AI",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * TRẠNG THÁI LOADING (HIỂN THỊ TIẾN TRÌNH RÕ RÀNG)
 */
@Composable
fun DashboardLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Đang tải dữ liệu sức khỏe gia đình...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * TRẠNG THÁI ERROR (KÈM NÚT THỬ LẠI TO BẢN)
 */
@Composable
fun DashboardErrorView(
    errorMessage: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = "Lỗi",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Không thể tải dữ liệu",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(56.dp), // Chuẩn chiều cao nút ≥ 56dp
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Thử lại",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
