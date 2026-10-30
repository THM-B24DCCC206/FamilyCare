package com.familycare.app.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import com.familycare.app.domain.model.User
import com.familycare.app.presentation.home.Member
import com.familycare.app.ui.theme.*
import java.util.Calendar

private fun calculateAge(birthDateStr: String): Int? {
    if (birthDateStr.isBlank()) return null
    return try {
        val parts = birthDateStr.split("/", "-")
        val year = when {
            parts.size == 3 && parts[2].length == 4 -> parts[2].toIntOrNull()
            parts.size == 3 && parts[0].length == 4 -> parts[0].toIntOrNull()
            else -> null
        }
        if (year != null) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            (currentYear - year).coerceAtLeast(0)
        } else null
    } catch (_: Exception) {
        null
    }
}

private fun getInitials(fullName: String): String {
    val words = fullName.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "FC"
        words.size == 1 -> words[0].take(2).uppercase()
        else -> "${words.first().take(1)}${words.last().take(1)}".uppercase()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onNavigateToPdfReport: (memberId: String) -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModel.provideFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val snackbarHostState = remember { SnackbarHostState() }

    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.saveSuccessMessage) {
        uiState.saveSuccessMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearSaveMessage()
        }
    }

    // Nếu đang xem chi tiết một thành viên (Cách 2)
    val viewingMember = uiState.viewingMember
    if (viewingMember != null) {
        MemberDetailScreen(
            member = viewingMember,
            medicines = uiState.viewingMemberMedicines,
            onBack = { viewModel.closeMemberDetail() },
            onEdit = { viewModel.openEditProfileDialog(viewingMember) },
            onExportPdf = { onNavigateToPdfReport(viewingMember.id) }
        )
    } else {
        // Màn hình chính Tab Hồ Sơ
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Hồ Sơ",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = Color(0xFF0F172A)
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { paddingValues ->
            if (uiState.status is ProfileUiStatus.Loading && uiState.currentUser == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = WarmPrimaryTeal)
                }
            } else {
                val currentUser = uiState.currentUser
                val displayName = currentUser?.fullName.orEmpty().ifBlank { sessionManager.userName.ifBlank { "Tài khoản của tôi" } }
                val age = currentUser?.birthDate?.takeIf { it.isNotBlank() }?.let { calculateAge(it) }
                val roleTitle = if (sessionManager.userRole == "ADMIN" || currentUser?.role?.name == "ADMIN") "Trưởng gia đình" else "Thành viên"
                val subtitle = if (age != null) "$roleTitle • $age tuổi" else roleTitle

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 90.dp)
                ) {
                    // 1. Header Tài Khoản Người Dùng (Theo Mockup)
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Avatar tròn chữ cái viết tắt nền xanh pastel
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFD1FAE5)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = getInitials(displayName),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF047857)
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        fontSize = 13.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                // Icon chỉnh sửa bút chì
                                IconButton(
                                    onClick = { viewModel.openEditProfileDialog(currentUser) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Chỉnh sửa hồ sơ",
                                        tint = Color(0xFF475569),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Khối: THÔNG TIN CÁ NHÂN (Theo Mockup)
                    item {
                        Column {
                            Text(
                                text = "Thông tin cá nhân",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                            )

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                    InfoRowItem(
                                        label = "Ngày sinh",
                                        value = currentUser?.birthDate.orEmpty().ifBlank { "Chưa cập nhật" }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                    InfoRowItem(
                                        label = "Giới tính",
                                        value = currentUser?.gender.orEmpty().ifBlank { "Chưa cập nhật" }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                    InfoRowItem(
                                        label = "Số điện thoại",
                                        value = currentUser?.phoneNumber.orEmpty().ifBlank { "Chưa cập nhật" }
                                    )
                                    HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                                    InfoRowItem(
                                        label = "Địa chỉ",
                                        value = currentUser?.address.orEmpty().ifBlank { "Chưa cập nhật" }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Khối: THÀNH VIÊN GIA ĐÌNH (Theo Mockup)
                    item {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp, start = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Thành viên gia đình",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                TextButton(
                                    onClick = { viewModel.openAddMemberDialog() },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = WarmPrimaryTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Thêm thành viên",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = WarmPrimaryTeal
                                    )
                                }
                            }

                            // Danh sách thành viên người thân (loại trừ tài khoản đang đăng nhập)
                            val otherMembers = uiState.allUsers.filter { it.id != sessionManager.userId }

                            Card(
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    if (otherMembers.isEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(24.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Chưa có thành viên nào khác. Bấm 'Thêm thành viên' để mời người thân.",
                                                fontSize = 13.sp,
                                                color = Color(0xFF94A3B8),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        otherMembers.forEachIndexed { index, member ->
                                            val memberAge = calculateAge(member.birthDate)
                                            val title = if (memberAge != null) {
                                                "${member.fullName} ($memberAge tuổi)"
                                            } else {
                                                member.fullName
                                            }

                                            MemberListItemRow(
                                                nameAndAge = title,
                                                initials = getInitials(member.fullName),
                                                onClick = { viewModel.openMemberDetail(member.id) }
                                            )

                                            if (index < otherMembers.lastIndex) {
                                                HorizontalDivider(
                                                    color = Color(0xFFF1F5F9),
                                                    thickness = 1.dp,
                                                    modifier = Modifier.padding(horizontal = 16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Khối: TIỆN ÍCH & CÀI ĐẶT (Theo Mockup)
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                UtilityMenuItem(
                                    icon = Icons.Outlined.MonitorHeart,
                                    iconTint = Color(0xFF0F766E),
                                    title = "Dữ liệu sức khỏe",
                                    onClick = { viewModel.openHealthDataDialog() }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                                UtilityMenuItem(
                                    icon = Icons.Outlined.Description,
                                    iconTint = Color(0xFF0F766E),
                                    title = "Báo cáo y tế",
                                    onClick = {
                                        val targetId = sessionManager.userId.ifBlank { "user_me" }
                                        onNavigateToPdfReport(targetId)
                                    }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                                UtilityMenuItem(
                                    icon = Icons.Outlined.Settings,
                                    iconTint = Color(0xFF475569),
                                    title = "Cài đặt",
                                    onClick = { viewModel.openSettingsDialog() }
                                )
                                HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

                                UtilityMenuItem(
                                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                                    iconTint = Color(0xFF475569),
                                    title = "Trợ giúp & Hỗ trợ",
                                    onClick = { viewModel.openHelpDialog() }
                                )
                            }
                        }
                    }

                    // 5. Khối: ĐĂNG XUẤT (Card Đỏ Pastel Theo Mockup)
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFEE2E2)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showLogoutDialog = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 15.dp, horizontal = 18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFEE2E2)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Đăng xuất",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog xác nhận đăng xuất
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White,
            title = { Text("Đăng Xuất", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?", color = Color(0xFF475569)) },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        sessionManager.clearSession()
                        onLogout()
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Đăng xuất", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy", color = Color(0xFF64748B))
                }
            }
        )
    }

    // Dialog Chỉnh sửa Hồ Sơ (Cá nhân + Y Bạ)
    if (uiState.showEditProfileDialog) {
        val targetUser = uiState.viewingMember ?: uiState.currentUser
        if (targetUser != null) {
            EditUserProfileDialog(
                initialUser = targetUser,
                onDismiss = { viewModel.dismissEditProfileDialog() },
                onSave = { name, dob, gender, phone, addr, blood, diseases, allergies ->
                    viewModel.saveProfile(
                        userId = targetUser.id,
                        fullName = name,
                        birthDate = dob,
                        gender = gender,
                        phoneNumber = phone,
                        address = addr,
                        bloodType = blood,
                        chronicDiseases = diseases,
                        drugAllergies = allergies
                    )
                }
            )
        }
    }

    // Dialog Dữ liệu sức khỏe (Y bạ của nick đăng nhập)
    if (uiState.showHealthDataDialog) {
        val currentUser = uiState.currentUser
        HealthDataSummaryDialog(
            user = currentUser,
            onDismiss = { viewModel.dismissHealthDataDialog() },
            onEdit = {
                viewModel.dismissHealthDataDialog()
                viewModel.openEditProfileDialog(currentUser)
            }
        )
    }

    // Dialog Thêm thành viên
    if (uiState.showAddMemberDialog) {
        AddMemberDialog(
            inviteCode = sessionManager.inviteCode.ifEmpty { "FAM-8606" },
            onDismiss = { viewModel.dismissAddMemberDialog() },
            onAddDirect = { name, relation, dob, gender, phone, blood, diseases, allergies ->
                viewModel.addNewMember(
                    fullName = name,
                    relationship = relation,
                    birthDate = dob,
                    gender = gender,
                    phoneNumber = phone,
                    bloodType = blood,
                    chronicDiseases = diseases,
                    drugAllergies = allergies
                )
            }
        )
    }

    // Dialog Cài đặt (Mã gia đình, thông báo)
    if (uiState.showSettingsDialog) {
        SettingsDialog(
            familyName = sessionManager.familyName,
            inviteCode = sessionManager.inviteCode.ifEmpty { "FAM-8606" },
            onDismiss = { viewModel.dismissSettingsDialog() }
        )
    }

    // Dialog Trợ giúp & Hỗ trợ
    if (uiState.showHelpDialog) {
        HelpDialog(onDismiss = { viewModel.dismissHelpDialog() })
    }
}

// ==========================================
// MÀN HÌNH CHI TIẾT THÀNH VIÊN (Cách 2)
// ==========================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MemberDetailScreen(
    member: User,
    medicines: List<String>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onExportPdf: () -> Unit
) {
    val age = calculateAge(member.birthDate)
    val subtitle = if (age != null) "${member.role.name} • $age tuổi" else member.role.name

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hồ sơ của ${member.fullName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = Color(0xFF0F172A)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Chỉnh sửa",
                            tint = WarmPrimaryTeal
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp)
        ) {
            // Header thành viên
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(member.fullName),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF334155)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.fullName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = subtitle,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFECFDF5))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "● Đang theo dõi",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF15803D)
                                )
                            }
                        }
                    }
                }
            }

            // Thông tin cá nhân
            item {
                Column {
                    Text(
                        text = "Thông tin cá nhân",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            InfoRowItem(label = "Ngày sinh", value = member.birthDate.ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            InfoRowItem(label = "Giới tính", value = member.gender.ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            InfoRowItem(label = "Số điện thoại", value = member.phoneNumber.ifBlank { "Chưa cập nhật" })
                            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)
                            InfoRowItem(label = "Địa chỉ", value = member.address.ifBlank { "Chưa cập nhật" })
                        }
                    }
                }
            }

            // Hồ sơ Y Bạ
            item {
                Column {
                    Text(
                        text = "Hồ sơ y tế",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Nhóm máu", color = Color(0xFF475569), fontSize = 14.sp)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFFFEF2F2))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = member.bloodType.ifBlank { "Chưa rõ" },
                                        color = Color(0xFFDC2626),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Bệnh mãn tính:", color = Color(0xFF475569), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (member.chronicDiseases.isEmpty()) {
                                Text("Không có", fontSize = 13.sp, color = Color(0xFF94A3B8), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    member.chronicDiseases.forEach { disease ->
                                        TagChip(text = disease, bg = Color(0xFFFEF3C7), textCol = Color(0xFFB45309))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            Text("Dị ứng thuốc:", color = Color(0xFF475569), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(6.dp))
                            if (member.drugAllergies.isEmpty()) {
                                Text("Không có dị ứng", fontSize = 13.sp, color = Color(0xFF94A3B8), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            } else {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    member.drugAllergies.forEach { allergy ->
                                        TagChip(text = allergy, bg = Color(0xFFFEE2E2), textCol = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Đơn thuốc đang uống
            item {
                Column {
                    Text(
                        text = "Thuốc đang uống hàng ngày",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp)
                    )

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (medicines.isEmpty()) {
                                Text(
                                    text = "Hiện chưa có lịch uống thuốc nào được ghi nhận cho thành viên này.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            } else {
                                medicines.forEachIndexed { i, med ->
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text("💊", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(text = med, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color(0xFF0F172A))
                                    }
                                    if (i < medicines.lastIndex) {
                                        HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Nút Xuất Báo Cáo PDF
            item {
                Button(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Icon(imageVector = Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Xuất Báo Cáo Y Tế (PDF)", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// CÁC HÀNG VÀ MỤC GIAO DIỆN CON
// ==========================================

@Composable
private fun InfoRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF475569)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF0F172A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MemberListItemRow(
    nameAndAge: String,
    initials: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF1F5F9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color(0xFF475569)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = nameAndAge,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFECFDF5))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "● Đang theo dõi",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF15803D)
                )
            }
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun UtilityMenuItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF0F172A),
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF94A3B8),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun TagChip(text: String, bg: Color, textCol: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = textCol)
    }
}

// ==========================================
// CÁC HỘP THOẠI (DIALOGS)
// ==========================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditUserProfileDialog(
    initialUser: User,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        dob: String,
        gender: String,
        phone: String,
        addr: String,
        blood: String,
        diseases: List<String>,
        allergies: List<String>
    ) -> Unit
) {
    var name by remember { mutableStateOf(initialUser.fullName) }
    var dob by remember { mutableStateOf(initialUser.birthDate) }
    var gender by remember { mutableStateOf(initialUser.gender) }
    var phone by remember { mutableStateOf(initialUser.phoneNumber) }
    var addr by remember { mutableStateOf(initialUser.address) }
    var blood by remember { mutableStateOf(initialUser.bloodType) }

    val diseases = remember { mutableStateListOf<String>().apply { addAll(initialUser.chronicDiseases) } }
    val allergies = remember { mutableStateListOf<String>().apply { addAll(initialUser.drugAllergies) } }

    var newDiseaseInput by remember { mutableStateOf("") }
    var newAllergyInput by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Chỉnh Sửa Hồ Sơ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = { dob = it },
                    label = { Text("Ngày sinh (dd/MM/yyyy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Giới tính
                Text("Giới tính", fontSize = 13.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Nam", "Nữ").forEach { g ->
                        val selected = gender == g
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (selected) WarmPrimaryLight else Color(0xFFF1F5F9))
                                .border(1.dp, if (selected) WarmPrimaryTeal else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable { gender = g }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = g,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) WarmPrimaryTeal else Color(0xFF334155),
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Số điện thoại") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = addr,
                    onValueChange = { addr = it },
                    label = { Text("Địa chỉ") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Nhóm máu
                Text("Nhóm máu", fontSize = 13.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("A+", "B+", "O+", "AB+").forEach { b ->
                        val selected = blood == b
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) Color(0xFFFEF2F2) else Color(0xFFF8FAFC))
                                .border(1.dp, if (selected) Color(0xFFDC2626) else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable { blood = b }
                            .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = b,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color(0xFFDC2626) else Color(0xFF475569),
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bệnh nền
                Text("Bệnh nền mãn tính", fontSize = 13.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newDiseaseInput,
                        onValueChange = { newDiseaseInput = it },
                        placeholder = { Text("VD: Tăng huyết áp...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newDiseaseInput.isNotBlank()) {
                                diseases.add(newDiseaseInput.trim())
                                newDiseaseInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                    ) {
                        Text("Thêm")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    diseases.forEach { d ->
                        InputChip(
                            selected = false,
                            onClick = { diseases.remove(d) },
                            label = { Text(d, fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Dị ứng thuốc
                Text("Dị ứng thuốc", fontSize = 13.sp, color = Color(0xFF475569))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newAllergyInput,
                        onValueChange = { newAllergyInput = it },
                        placeholder = { Text("VD: Penicillin...", fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newAllergyInput.isNotBlank()) {
                                allergies.add(newAllergyInput.trim())
                                newAllergyInput = ""
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                    ) {
                        Text("Thêm")
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    allergies.forEach { a ->
                        InputChip(
                            selected = false,
                            onClick = { allergies.remove(a) },
                            label = { Text(a, fontSize = 12.sp) },
                            trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        onSave(name, dob, gender, phone, addr, blood, diseases.toList(), allergies.toList())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Text("Lưu Thay Đổi", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// Dialog xem Dữ liệu sức khỏe của nick đăng nhập
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthDataSummaryDialog(
    user: User?,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dữ Liệu Sức Khỏe Của Tôi",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Nhóm máu
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Nhóm máu:", color = Color(0xFF475569), fontSize = 14.sp)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEF2F2))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = user?.bloodType.orEmpty().ifBlank { "Chưa cập nhật" },
                            color = if (user?.bloodType.isNullOrBlank()) Color(0xFF64748B) else Color(0xFFDC2626),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Bệnh nền mãn tính:", color = Color(0xFF475569), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val diseases = user?.chronicDiseases.orEmpty()
                if (diseases.isEmpty()) {
                    Text("Không có ghi nhận", fontSize = 13.sp, color = Color(0xFF94A3B8), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        diseases.forEach { TagChip(text = it, bg = Color(0xFFFEF3C7), textCol = Color(0xFFB45309)) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Dị ứng thuốc:", color = Color(0xFF475569), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                val allergies = user?.drugAllergies.orEmpty()
                if (allergies.isEmpty()) {
                    Text("Không có dị ứng", fontSize = 13.sp, color = Color(0xFF94A3B8), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        allergies.forEach { TagChip(text = it, bg = Color(0xFFFEE2E2), textCol = Color(0xFFDC2626)) }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cập Nhật Dữ Liệu Y Tế")
                }
            }
        }
    }
}

// Dialog Thêm thành viên (2 tab: Trực tiếp & Mã mời)
@Composable
fun AddMemberDialog(
    inviteCode: String,
    onDismiss: () -> Unit,
    onAddDirect: (
        name: String,
        relation: String,
        dob: String,
        gender: String,
        phone: String,
        blood: String,
        diseases: List<String>,
        allergies: List<String>
    ) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var fullName by remember { mutableStateOf("") }
    var relation by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Nam") }
    var phone by remember { mutableStateOf("") }
    var blood by remember { mutableStateOf("") }

    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Thêm Thành Viên", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFFF1F5F9),
                    contentColor = WarmPrimaryTeal,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Thêm trực tiếp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Mời qua mã", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Tên người thân (VD: Bố, Mẹ...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = relation,
                        onValueChange = { relation = it },
                        label = { Text("Mối quan hệ (VD: Bố, Mẹ, Con...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("Ngày sinh (dd/MM/yyyy)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Số điện thoại (tùy chọn)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (fullName.isNotBlank()) {
                                onAddDirect(fullName, relation, dob, gender, phone, blood, emptyList(), emptyList())
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                    ) {
                        Text("Lưu Thành Viên", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Mã mời gia đình của bạn:", fontSize = 13.sp, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = inviteCode,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmPrimaryTeal,
                                letterSpacing = 2.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Người thân chỉ cần nhập mã này khi đăng ký là sẽ tự động gia nhập tổ ấm của bạn.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Mã mời", inviteCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép mã mời!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sao Chép Mã Mời")
                        }
                    }
                }
            }
        }
    }
}

// Dialog Cài đặt
@Composable
fun SettingsDialog(
    familyName: String,
    inviteCode: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Cài Đặt", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Tổ ấm: $familyName", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF0F172A))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Mã mời tổ ấm: $inviteCode", color = Color(0xFF475569), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Mã mời", inviteCode))
                        Toast.makeText(context, "Đã sao chép mã: $inviteCode", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sao chép mã tổ ấm")
                }
            }
        }
    }
}

// Dialog Trợ giúp & Hỗ trợ
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = Color.White, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trợ Giúp & Hỗ Trợ", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFF64748B))
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("🚨 Đường dây nóng Cấp cứu: 115", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFDC2626))
                Spacer(modifier = Modifier.height(8.dp))
                Text("📞 Tổng đài FamilyCare: 1900 6868", fontSize = 14.sp, color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Text("💡 Hướng dẫn đo nhịp tim: Đặt ngón tay trỏ che kín camera sau và bật đèn flash, giữ yên trong 30 giây.", fontSize = 13.sp, color = Color(0xFF64748B))
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
                ) {
                    Text("Đóng")
                }
            }
        }
    }
}
