package com.familycare.app.presentation.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.User
import com.familycare.app.domain.model.UserRole
import com.familycare.app.presentation.home.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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

    val selectedMember = uiState.selectedMemberUser ?: uiState.currentUser
    val selectedMemberName = selectedMember?.fullName.orEmpty().ifBlank { sessionManager.userName }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Hồ Sơ Sức Khỏe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (uiState.status is ProfileUiStatus.Loading && uiState.currentUser == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
            ) {
                // 1. Thẻ Tài Khoản Đăng Nhập Hiện Tại
                item {
                    AccountHeaderCard(
                        userName = sessionManager.userName.ifBlank { uiState.currentUser?.fullName ?: "Tôi" },
                        userEmail = sessionManager.userEmail.ifBlank { uiState.currentUser?.email ?: "Chưa liên kết email" },
                        userRole = sessionManager.userRole
                    )
                }

                // 2. Khối "Thành viên trong gia đình" (FamilyMemberList)
                item {
                    FamilyMemberSection(
                        members = uiState.familyMembers,
                        selectedMemberId = uiState.selectedMemberId,
                        onSelectMember = { viewModel.onMemberSelect(it) },
                        onAddMember = { viewModel.openAddMemberDialog() }
                    )
                }

                // 3. Thẻ Y Bạ Động theo Thành Viên Được Chọn (MemberHealthCard)
                item {
                    MemberHealthCard(
                        member = selectedMember,
                        memberName = selectedMemberName,
                        onEdit = { viewModel.openEditHealthCardDialog() }
                    )
                }

                // 4. Nút Xuất Báo Cáo Y Tế (PDF) theo Người Được Chọn (Mở màn hình Preview)
                item {
                    Button(
                        onClick = {
                            val targetId = uiState.selectedMemberId.ifBlank { sessionManager.userId }
                            onNavigateToPdfReport(targetId)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Xuất Báo Cáo Y Tế (PDF)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // 5. Thẻ Mã Mời Gia Đình Rút Gọn
                item {
                    CompactFamilyInviteCard(
                        familyName = sessionManager.familyName,
                        inviteCode = sessionManager.inviteCode.ifEmpty { "FAM-8606" },
                        onCopy = { code ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Mã mời gia đình", code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Đã sao chép mã mời: $code", Toast.LENGTH_SHORT).show()
                        }
                    )
                }

                // 6. Nút "Đăng xuất tài khoản" Tinh Tế ở Cuối Cùng
                item {
                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFDC2626)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Đăng xuất tài khoản",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }
        }
    }

    // Dialog xác nhận đăng xuất
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Đăng Xuất", fontWeight = FontWeight.Bold) },
            text = { Text("Bạn có chắc chắn muốn đăng xuất khỏi tài khoản này không?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        sessionManager.clearSession()
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Đăng xuất")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Hộp thoại chỉnh sửa thông tin Y Bạ của thành viên đang chọn
    if (uiState.showEditHealthCardDialog && selectedMember != null) {
        EditHealthCardDialog(
            memberName = selectedMemberName,
            initialBloodType = selectedMember.bloodType,
            initialDiseases = selectedMember.chronicDiseases,
            initialAllergies = selectedMember.drugAllergies,
            onDismiss = { viewModel.dismissEditHealthCardDialog() },
            onSave = { bloodType, diseases, allergies ->
                viewModel.saveHealthCard(bloodType, diseases, allergies)
            }
        )
    }

    // Hộp thoại Thêm Thành Viên Mới vào gia đình
    if (uiState.showAddMemberDialog) {
        AddMemberDialog(
            onDismiss = { viewModel.dismissAddMemberDialog() },
            onAdd = { name, relationship, bloodType, diseases, allergies ->
                viewModel.addNewMember(name, relationship, bloodType, diseases, allergies)
            }
        )
    }
}

/**
 * 1. Thẻ thông tin tài khoản đăng nhập ở đầu trang
 */
@Composable
private fun AccountHeaderCard(
    userName: String,
    userEmail: String,
    userRole: String
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFF6FF))
                    .border(2.dp, Color(0xFF2563EB), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = Color(0xFF2563EB)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B)
            )

            if (userEmail.isNotBlank()) {
                Text(
                    text = userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val isAdmin = userRole.uppercase() == "ADMIN"
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAdmin) Color(0xFFEFF6FF) else Color(0xFFF1F5F9)
            ) {
                Text(
                    text = if (isAdmin) "Trưởng Gia Đình" else "Thành Viên",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAdmin) Color(0xFF1D4ED8) else Color(0xFF475569)
                )
            }
        }
    }
}

/**
 * 2. Khối "Thành viên trong gia đình" (FamilyMemberList)
 */
@Composable
private fun FamilyMemberSection(
    members: List<Member>,
    selectedMemberId: String,
    onSelectMember: (String) -> Unit,
    onAddMember: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Thành viên gia đình",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            TextButton(
                onClick = onAddMember,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFF2563EB)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ Thêm thành viên",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB)
                )
            }
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(members, key = { it.id }) { member ->
                val isSelected = member.id == selectedMemberId
                val borderColor by animateColorAsState(
                    if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                    label = "border"
                )
                val bgColor by animateColorAsState(
                    if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                    label = "bg"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = bgColor,
                    border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onSelectMember(member.id) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Avatar tròn Initials
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = member.initials.ifBlank { member.fullName.take(1).uppercase() },
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = member.fullName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF1E40AF) else Color(0xFF334155)
                        )

                        // Dấu tích nhỏ nếu đang được chọn
                        if (isSelected) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Đang chọn",
                                    tint = Color.White,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3. Thẻ Y Bạ Động theo Thành Viên Được Chọn (MemberHealthCard)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MemberHealthCard(
    member: User?,
    memberName: String,
    onEdit: () -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Thẻ: Tiêu đề "Thông tin y tế: [Tên]" + Nút Chỉnh sửa nhỏ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thông tin y tế: $memberName",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Chỉnh sửa thông tin y tế",
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Mục 1: Nhóm Máu (Icon giọt máu)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = Color(0xFFE11D48),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Nhóm máu",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }

                val bloodType = member?.bloodType.orEmpty().ifBlank { "Chưa xác định" }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF1F2)
                ) {
                    Text(
                        text = bloodType,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFBE123C),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Mục 2: Bệnh nền mãn tính
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Bệnh nền mãn tính",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                val diseases = member?.chronicDiseases.orEmpty()
                if (diseases.isEmpty()) {
                    Text(
                        text = "Chưa ghi nhận bệnh nền",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 24.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 24.dp)
                    ) {
                        diseases.forEach { disease ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Text(
                                    text = disease,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFF1F5F9), thickness = 1.dp)

            // Mục 3: Dị ứng thuốc & thức ăn
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dị ứng thuốc & thức ăn",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                }

                val allergies = member?.drugAllergies.orEmpty()
                if (allergies.isEmpty()) {
                    Text(
                        text = "Chưa ghi nhận dị ứng",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(start = 24.dp)
                    )
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(start = 24.dp)
                    ) {
                        allergies.forEach { allergy ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Text(
                                    text = allergy,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFB91C1C),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. Thẻ mã mời gia đình rút gọn ở chân trang
 */
@Composable
private fun CompactFamilyInviteCard(
    familyName: String,
    inviteCode: String,
    onCopy: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tổ ấm: ${familyName.ifBlank { "Gia đình" }} • Mã: $inviteCode",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF334155),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = { onCopy(inviteCode) },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Sao chép mã mời",
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Hộp thoại chỉnh sửa thông tin Y bạ của thành viên (nhóm máu, bệnh nền, dị ứng)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditHealthCardDialog(
    memberName: String,
    initialBloodType: String,
    initialDiseases: List<String>,
    initialAllergies: List<String>,
    onDismiss: () -> Unit,
    onSave: (bloodType: String, diseases: List<String>, allergies: List<String>) -> Unit
) {
    var bloodType by remember { mutableStateOf(initialBloodType) }
    val diseasesList = remember { mutableStateListOf<String>().apply { addAll(initialDiseases) } }
    val allergiesList = remember { mutableStateListOf<String>().apply { addAll(initialAllergies) } }

    var newDiseaseText by remember { mutableStateOf("") }
    var newAllergyText by remember { mutableStateOf("") }

    val bloodOptions = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Cập nhật Y Bạ: $memberName",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Chọn nhóm máu
                Text(text = "Nhóm máu:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bloodOptions) { opt ->
                        FilterChip(
                            selected = bloodType == opt,
                            onClick = { bloodType = opt },
                            label = { Text(opt, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Bệnh nền mãn tính
                Text(text = "Bệnh nền mãn tính:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (diseasesList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        diseasesList.forEach { dis ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(dis, fontSize = 12.sp, color = Color(0xFF1D4ED8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { diseasesList.remove(dis) },
                                        tint = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newDiseaseText,
                        onValueChange = { newDiseaseText = it },
                        placeholder = { Text("Ví dụ: Cao huyết áp...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val t = newDiseaseText.trim()
                            if (t.isNotBlank() && !diseasesList.contains(t)) {
                                diseasesList.add(t)
                                newDiseaseText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm", tint = Color(0xFF2563EB))
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // Dị ứng thuốc & thức ăn
                Text(text = "Dị ứng thuốc & thức ăn:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (allergiesList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allergiesList.forEach { alg ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(alg, fontSize = 12.sp, color = Color(0xFFB91C1C))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { allergiesList.remove(alg) },
                                        tint = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newAllergyText,
                        onValueChange = { newAllergyText = it },
                        placeholder = { Text("Ví dụ: Dị ứng Penicillin...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val t = newAllergyText.trim()
                            if (t.isNotBlank() && !allergiesList.contains(t)) {
                                allergiesList.add(t)
                                newAllergyText = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm", tint = Color(0xFFDC2626))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(bloodType, diseasesList.toList(), allergiesList.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Lưu Thay Đổi")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}

/**
 * Hộp thoại Thêm Thành Viên Mới vào Gia Đình
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddMemberDialog(
    onDismiss: () -> Unit,
    onAdd: (name: String, relationship: String, bloodType: String, diseases: List<String>, allergies: List<String>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("Thành viên") }
    var bloodType by remember { mutableStateOf("O+") }
    val diseasesList = remember { mutableStateListOf<String>() }
    val allergiesList = remember { mutableStateListOf<String>() }

    var diseaseInput by remember { mutableStateOf("") }
    var allergyInput by remember { mutableStateOf("") }

    val relationshipOptions = listOf("Vợ / Chồng", "Bố", "Mẹ", "Con cái", "Thành viên")
    val bloodOptions = listOf("A+", "B+", "AB+", "O+", "A-", "B-", "AB-", "O-")

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Thêm Thành Viên Gia Đình", fontWeight = FontWeight.Bold, fontSize = 17.sp)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Họ và tên *") },
                    placeholder = { Text("Ví dụ: Ly Trần, Bác Nam...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Mối quan hệ:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(relationshipOptions) { rel ->
                        FilterChip(
                            selected = relationship == rel,
                            onClick = { relationship = rel },
                            label = { Text(rel, fontSize = 12.sp) }
                        )
                    }
                }

                Text("Nhóm máu:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(bloodOptions) { b ->
                        FilterChip(
                            selected = bloodType == b,
                            onClick = { bloodType = b },
                            label = { Text(b, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                Text("Bệnh nền (nếu có):", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (diseasesList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        diseasesList.forEach { d ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(d, fontSize = 12.sp, color = Color(0xFF1D4ED8))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { diseasesList.remove(d) },
                                        tint = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = diseaseInput,
                        onValueChange = { diseaseInput = it },
                        placeholder = { Text("Ví dụ: Cao huyết áp...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val t = diseaseInput.trim()
                            if (t.isNotBlank() && !diseasesList.contains(t)) {
                                diseasesList.add(t)
                                diseaseInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm", tint = Color(0xFF2563EB))
                    }
                }

                Text("Dị ứng thuốc & thức ăn:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (allergiesList.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allergiesList.forEach { a ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFEF2F2),
                                border = BorderStroke(1.dp, Color(0xFFFECACA))
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(a, fontSize = 12.sp, color = Color(0xFFB91C1C))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { allergiesList.remove(a) },
                                        tint = Color(0xFFB91C1C)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = allergyInput,
                        onValueChange = { allergyInput = it },
                        placeholder = { Text("Ví dụ: Dị ứng Hải sản...", fontSize = 12.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            val t = allergyInput.trim()
                            if (t.isNotBlank() && !allergiesList.contains(t)) {
                                allergiesList.add(t)
                                allergyInput = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Thêm", tint = Color(0xFFDC2626))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập họ và tên thành viên", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onAdd(name.trim(), relationship, bloodType, diseasesList.toList(), allergiesList.toList())
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
            ) {
                Text("Thêm Thành Viên")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
