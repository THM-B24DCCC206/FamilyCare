package com.familycare.app.presentation.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.EventNote
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.domain.model.Appointment
import com.familycare.app.domain.model.Medicine
import com.familycare.app.presentation.home.Member
import androidx.compose.ui.res.painterResource
import com.familycare.app.R
import com.familycare.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScheduleScreen(
    onNavigateToOcr: (memberId: String) -> Unit,
    viewModel: HealthScheduleViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadFamilyMembersAndInitialData()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadFamilyMembersAndInitialData()
                viewModel.checkAndRefreshIfDayChanged()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var showAddMedicineSessionDialog by remember { mutableStateOf(false) }
    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var editingMedicine by remember { mutableStateOf<Medicine?>(null) }

    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                // Tiêu đề lớn "Kế Hoạch Sức Khỏe"
                Text(
                    text = "Kế Hoạch Sức Khỏe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Thanh chọn thành viên gia đình (Member Switcher) LazyRow ngang
                if (uiState.familyMembers.isNotEmpty()) {
                    MemberSwitcherRow(
                        members = uiState.familyMembers,
                        selectedMemberId = uiState.selectedMemberId,
                        onSelectMember = { memberId ->
                            viewModel.selectMember(memberId)
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs chuyển đổi kép (Segmented Bar)
            DualSegmentedBar(
                selectedTab = uiState.selectedTab,
                medicineCount = uiState.medicines.size,
                appointmentCount = uiState.upcomingAppointments.size,
                onTabSelect = { viewModel.selectTab(it) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Hiển thị nội dung tương ứng theo Tab
            when (uiState.selectedTab) {
                HealthScheduleTab.MEDICATION -> {
                    MedicationTabContent(
                        medicines = uiState.medicines,
                        todayLogs = uiState.todayLogs,
                        viewMode = uiState.medicationViewMode,
                        onSelectViewMode = { viewModel.selectMedicationViewMode(it) },
                        onAddSession = { showAddMedicineSessionDialog = true },
                        onAddSingleMedicine = { showAddMedicineDialog = true },
                        onScanAi = { onNavigateToOcr(uiState.selectedMemberId) },
                        onTakeMedication = { medicine, time ->
                            viewModel.markMedicationTaken(medicine, time)
                            Toast.makeText(context, "Đã ghi nhận uống thuốc ${medicine.name}", Toast.LENGTH_SHORT).show()
                        },
                        onUndoTakeMedication = { medicine, time ->
                            viewModel.undoMedicationTaken(medicine, time)
                            Toast.makeText(context, "Đã hoàn tác trạng thái", Toast.LENGTH_SHORT).show()
                        },
                        onTakeSession = { time, meds ->
                            viewModel.markSessionTaken(time, meds)
                            Toast.makeText(context, "Đã ghi nhận uống cữ $time (${meds.size} loại thuốc)", Toast.LENGTH_SHORT).show()
                        },
                        onUndoTakeSession = { time, meds ->
                            viewModel.undoSessionTaken(time, meds)
                            Toast.makeText(context, "Đã hoàn tác cữ $time", Toast.LENGTH_SHORT).show()
                        },
                        onEditMedicine = { editingMedicine = it },
                        onDeleteMedicine = { viewModel.deleteMedicine(it.id) }
                    )
                }

                HealthScheduleTab.APPOINTMENT -> {
                    AppointmentTabContent(
                        upcomingAppointments = uiState.upcomingAppointments,
                        pastAppointments = uiState.pastAppointments,
                        onAddAppointment = { showAddAppointmentDialog = true },
                        onCompleteAppointment = {
                            viewModel.markAppointmentCompleted(it)
                            Toast.makeText(context, "Đã hoàn thành lịch khám!", Toast.LENGTH_SHORT).show()
                        },
                        onEditAppointment = { editingAppointment = it },
                        onDeleteAppointment = { viewModel.deleteAppointment(it.id) }
                    )
                }
            }
        }
    }

    // Dialog thêm/sửa thuốc thủ công
    if (showAddMedicineDialog || editingMedicine != null) {
        AddEditMedicineDialog(
            initialMedicine = editingMedicine,
            onDismiss = {
                showAddMedicineDialog = false
                editingMedicine = null
            },
            onConfirm = { name, dosage, purpose, instruction, times ->
                if (editingMedicine != null) {
                    viewModel.updateMedicine(editingMedicine!!, name, dosage, purpose, instruction, times)
                    Toast.makeText(context, "Đã cập nhật thuốc", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addMedicine(name, dosage, purpose, instruction, times)
                    Toast.makeText(context, "Đã thêm thuốc mới", Toast.LENGTH_SHORT).show()
                }
                showAddMedicineDialog = false
                editingMedicine = null
            }
        )
    }

    // Dialog thêm cữ thuốc trực tiếp (nhiều loại thuốc cùng 1 cữ giờ - Cách 1)
    if (showAddMedicineSessionDialog) {
        AddMedicineSessionDialog(
            onDismiss = { showAddMedicineSessionDialog = false },
            onConfirm = { sessionTime, commonInstruction, items ->
                viewModel.addMedicineSession(sessionTime, commonInstruction, items)
                Toast.makeText(context, "Đã lưu cữ $sessionTime với ${items.size} loại thuốc", Toast.LENGTH_SHORT).show()
                showAddMedicineSessionDialog = false
            }
        )
    }

    // Dialog thêm/sửa lịch khám bệnh
    if (showAddAppointmentDialog || editingAppointment != null) {
        AddEditAppointmentDialog(
            initialAppointment = editingAppointment,
            onDismiss = {
                showAddAppointmentDialog = false
                editingAppointment = null
            },
            onConfirm = { hospital, dept, doctor, time, notes ->
                if (editingAppointment != null) {
                    viewModel.updateAppointment(
                        editingAppointment!!.copy(
                            hospitalName = hospital,
                            department = dept,
                            doctorName = doctor,
                            appointmentTime = time,
                            notes = notes
                        )
                    )
                    Toast.makeText(context, "Đã cập nhật lịch khám", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addAppointment(hospital, dept, doctor, time, notes)
                    Toast.makeText(context, "Đã thêm lịch khám mới", Toast.LENGTH_SHORT).show()
                }
                showAddAppointmentDialog = false
                editingAppointment = null
            }
        )
    }
}

/**
 * Thanh chọn thành viên gia đình (Member Switcher) dạng LazyRow ngang
 */
@Composable
fun MemberSwitcherRow(
    members: List<Member>,
    selectedMemberId: String,
    onSelectMember: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(members, key = { it.id }) { member ->
            val isSelected = member.id == selectedMemberId

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Color.Transparent else Color.White,
                shadowElevation = if (isSelected) 2.dp else 1.dp,
                modifier = Modifier
                    .height(42.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectMember(member.id) }
            ) {
                val bgModifier = if (isSelected) {
                    Modifier.background(
                        Brush.horizontalGradient(
                            listOf(WarmPrimaryTeal, WarmPrimaryHover)
                        )
                    )
                } else {
                    Modifier.background(Color.White)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = bgModifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color.White.copy(alpha = 0.25f) else WarmPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initials.ifBlank { member.fullName.take(1).uppercase() },
                            color = if (isSelected) Color.White else WarmPrimaryTeal,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = member.fullName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

/**
 * Thanh tab gạt pill-tab bo tròn liền khối có nền xám nhạt #F1F5F9.
 * Tab được chọn nổi nền trắng rõ ràng với bóng đổ nhẹ 2dp và text/icon xanh #2563EB.
 */
@Composable
fun DualSegmentedBar(
    selectedTab: HealthScheduleTab,
    medicineCount: Int,
    appointmentCount: Int,
    onTabSelect: (HealthScheduleTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF1F5F9),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 1: [ 💊 Lịch Thuốc (X) ]
            val isMedication = selectedTab == HealthScheduleTab.MEDICATION
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isMedication) Color.White else Color.Transparent,
                shadowElevation = if (isMedication) 2.5.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelect(HealthScheduleTab.MEDICATION) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = if (isMedication) WarmPrimaryTeal else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lịch Thuốc ($medicineCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isMedication) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMedication) WarmPrimaryTeal else Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tab 2: [ 🩺 Lịch Khám Bệnh (Y) ]
            val isAppointment = selectedTab == HealthScheduleTab.APPOINTMENT
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isAppointment) Color.White else Color.Transparent,
                shadowElevation = if (isAppointment) 2.5.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onTabSelect(HealthScheduleTab.APPOINTMENT) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.EventNote,
                        contentDescription = null,
                        tint = if (isAppointment) WarmPrimaryTeal else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lịch Khám ($appointmentCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isAppointment) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAppointment) WarmPrimaryTeal else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * Nội dung Tab 1: Quản lý Thuốc với 2 chế độ xem:
 * - Theo Cữ Giờ (Chia thuốc theo cữ Sáng / Trưa / Tối / Đêm như hộp chia thuốc thông minh)
 * - Tủ Thuốc Gia Đình (Quản lý toàn bộ danh mục thuốc A-Z)
 */
@Composable
fun MedicationTabContent(
    medicines: List<Medicine>,
    todayLogs: List<MedicationLogEntity>,
    viewMode: MedicationViewMode,
    onSelectViewMode: (MedicationViewMode) -> Unit,
    onAddSession: () -> Unit,
    onAddSingleMedicine: () -> Unit,
    onScanAi: () -> Unit,
    onTakeMedication: (Medicine, String) -> Unit,
    onUndoTakeMedication: ((Medicine, String) -> Unit)? = null,
    onTakeSession: (String, List<Medicine>) -> Unit,
    onUndoTakeSession: (String, List<Medicine>) -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Thanh chuyển đổi 2 chế độ xem thuốc: [ ⏰ Theo Cữ Giờ ] vs [ 💊 Tủ Thuốc Gia Đình ]
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFFF1F5F9),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isSessions = viewMode == MedicationViewMode.SESSIONS
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (isSessions) Color.White else Color.Transparent,
                    shadowElevation = if (isSessions) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { onSelectViewMode(MedicationViewMode.SESSIONS) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = if (isSessions) WarmPrimaryTeal else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Theo Cữ Giờ",
                            fontSize = 13.sp,
                            fontWeight = if (isSessions) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSessions) WarmPrimaryTeal else Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                val isCabinet = viewMode == MedicationViewMode.CABINET
                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = if (isCabinet) Color.White else Color.Transparent,
                    shadowElevation = if (isCabinet) 2.dp else 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(11.dp))
                        .clickable { onSelectViewMode(MedicationViewMode.CABINET) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pill),
                            contentDescription = null,
                            tint = if (isCabinet) WarmPrimaryTeal else Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Tủ Thuốc (${medicines.size})",
                            fontSize = 13.sp,
                            fontWeight = if (isCabinet) FontWeight.Bold else FontWeight.Medium,
                            color = if (isCabinet) WarmPrimaryTeal else Color(0xFF64748B)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 2 nút hành động nhanh tương ứng theo chế độ xem
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (viewMode == MedicationViewMode.SESSIONS) {
                FilledTonalButton(
                    onClick = onAddSession,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WarmPrimaryLight,
                        contentColor = WarmPrimaryTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Thêm theo cữ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                FilledTonalButton(
                    onClick = onAddSingleMedicine,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = WarmPrimaryLight,
                        contentColor = WarmPrimaryTeal
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Thêm thuốc lẻ", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = onScanAi,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = WarmPrimaryTeal,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.DocumentScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Quét đơn AI", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(WarmPrimaryLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Medication,
                                contentDescription = null,
                                tint = WarmPrimaryTeal,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Chưa có thuốc trong tủ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dùng 'Thêm theo cữ' để tạo nhanh một cữ nhiều loại thuốc hoặc dùng Quét đơn AI.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            when (viewMode) {
                MedicationViewMode.SESSIONS -> {
                    // Chế độ 1: Theo Cữ Giờ
                    val allTimes = remember(medicines) {
                        medicines.flatMap { it.reminderTimes }.distinct().sorted()
                    }
                    val asNeededMedicines = remember(medicines) {
                        medicines.filter { it.reminderTimes.isEmpty() }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        if (allTimes.isEmpty() && asNeededMedicines.isNotEmpty()) {
                            item {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Toàn bộ thuốc đang ở dạng 'Uống khi cần' (chưa đặt cữ giờ cố định).",
                                        fontSize = 12.5.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            }
                        }

                        items(allTimes, key = { it }) { sessionTime ->
                            val sessionMedicines = medicines.filter { sessionTime in it.reminderTimes }
                            MedicationSessionCard(
                                sessionTime = sessionTime,
                                medicines = sessionMedicines,
                                todayLogs = todayLogs.filter { it.scheduledTime == sessionTime },
                                onTakeSession = { onTakeSession(sessionTime, sessionMedicines) },
                                onUndoTakeSession = { onUndoTakeSession(sessionTime, sessionMedicines) },
                                onEditMedicine = onEditMedicine,
                                onDeleteMedicine = onDeleteMedicine
                            )
                        }

                        if (asNeededMedicines.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Thuốc uống khi cần (${asNeededMedicines.size})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF334155)
                                )
                            }
                            items(asNeededMedicines, key = { it.id }) { medicine ->
                                MedicationCard(
                                    medicine = medicine,
                                    todayLogs = todayLogs.filter { it.medicineId == medicine.id },
                                    onTakeMedication = { time -> onTakeMedication(medicine, time) },
                                    onUndoTakeMedication = { time -> onUndoTakeMedication?.invoke(medicine, time) },
                                    onEdit = { onEditMedicine(medicine) },
                                    onDelete = { onDeleteMedicine(medicine) }
                                )
                            }
                        }
                    }
                }

                MedicationViewMode.CABINET -> {
                    // Chế độ 2: Tủ Thuốc Gia Đình A-Z
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(medicines, key = { it.id }) { medicine ->
                            MedicationCard(
                                medicine = medicine,
                                todayLogs = todayLogs.filter { it.medicineId == medicine.id },
                                onTakeMedication = { time -> onTakeMedication(medicine, time) },
                                onUndoTakeMedication = { time -> onUndoTakeMedication?.invoke(medicine, time) },
                                onEdit = { onEditMedicine(medicine) },
                                onDelete = { onDeleteMedicine(medicine) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Thẻ Cữ Giờ Thuốc: Gom toàn bộ thuốc uống cùng giờ (Sáng / Trưa / Tối)
 * Thiết kế tinh gọn, hiển thị rõ giờ cữ, liệt kê thuốc + liều lượng, lời dặn chung,
 * và 1 nút bấm "Đã uống cả cữ".
 */
@Composable
fun MedicationSessionCard(
    sessionTime: String,
    medicines: List<Medicine>,
    todayLogs: List<MedicationLogEntity>,
    onTakeSession: () -> Unit,
    onUndoTakeSession: () -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit
) {
    val sessionName = getTimeSessionLabel(sessionTime)
    val allSessionTaken = medicines.isNotEmpty() && medicines.all { med ->
        todayLogs.any { it.medicineId == med.id && it.scheduledTime == sessionTime && it.isTaken }
    }

    val commonInstruction = remember(medicines) {
        medicines.map { it.instruction.trim() }.firstOrNull { it.isNotBlank() } ?: ""
    }

    // Tự động gom các thuốc có cùng liều lượng: ví dụ "Thuốc A, B (1 viên)"
    val medicineSummary = remember(medicines) {
        formatMedicineSummaryList(medicines.map { Pair(it.name, it.dosage) })
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (allSessionTaken) Color(0xFFBBF7D0) else Color(0xFFE2E6EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Giờ cữ + Tên buổi + Tóm tắt thuốc tự động gom + Nút 1 chạm uống cả cữ
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (allSessionTaken) Color(0xFFDCFCE7) else WarmPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = if (allSessionTaken) R.drawable.ic_pill else R.drawable.ic_pill),
                            contentDescription = null,
                            tint = if (allSessionTaken) Color(0xFF16A34A) else WarmPrimaryTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$sessionTime ($sessionName)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (allSessionTaken) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "✅ Đã xong cữ",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF15803D),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hoàn tác",
                            fontSize = 11.5.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onUndoTakeSession() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Button(
                        onClick = onTakeSession,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmPrimaryTeal,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Đã uống", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Danh sách các loại thuốc tự động gộp theo liều lượng: ví dụ "Thuốc A, B: 1 viên"
            val groupedMedicines = remember(medicines) {
                medicines.groupBy { formatDosageString(it.dosage) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                groupedMedicines.forEach { (dosage, medsInGroup) ->
                    var showGroupMenu by remember { mutableStateOf(false) }
                    val names = medsInGroup.map {
                        val clean = it.name.trim()
                        if (clean.startsWith("Thuốc ", ignoreCase = true)) {
                            clean.substring(6).trim()
                        } else if (clean.startsWith("thuốc ", ignoreCase = true)) {
                            clean.substring(6).trim()
                        } else clean
                    }.filter { it.isNotBlank() }.distinct().joinToString(", ")

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        ) {
                            Text(
                                text = "•",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (allSessionTaken) Color(0xFF16A34A) else WarmPrimaryTeal,
                                modifier = Modifier.padding(top = 1.dp, end = 8.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Thuốc $names",
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Liều dùng: $dosage",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Box(modifier = Modifier.padding(top = 2.dp)) {
                            IconButton(
                                onClick = { showGroupMenu = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Thao tác",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showGroupMenu,
                                onDismissRequest = { showGroupMenu = false }
                            ) {
                                medsInGroup.forEach { med ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (medsInGroup.size == 1) "Chỉnh sửa" else "Sửa ${med.name}",
                                                fontSize = 13.5.sp
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        },
                                        onClick = {
                                            showGroupMenu = false
                                            onEditMedicine(med)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (medsInGroup.size == 1) "Xóa thuốc" else "Xóa ${med.name}",
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                        },
                                        onClick = {
                                            showGroupMenu = false
                                            onDeleteMedicine(med)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Ghi chú / Lời dặn chung cho cả cữ
            if (commonInstruction.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarmPrimaryLight.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = WarmPrimaryTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lời dặn chung: $commonInstruction",
                            fontSize = 12.sp,
                            color = WarmTextPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thẻ thuốc tối ưu giao diện:
 * - Nền trắng (#FFFFFF), bo góc 20dp, border #E2E6EC, đổ bóng nhẹ 1.5dp
 */
@Composable
fun MedicationCard(
    medicine: Medicine,
    todayLogs: List<MedicationLogEntity>,
    onTakeMedication: (time: String) -> Unit,
    onUndoTakeMedication: ((time: String) -> Unit)? = null,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Hàng 1 (Header thẻ): Icon thuốc + Tên thuốc + Tag công dụng + Nút 3 chấm
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(WarmPrimaryLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = WarmPrimaryTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = medicine.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (medicine.purpose.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarmPrimaryLight,
                            border = BorderStroke(1.dp, WarmPrimaryTeal.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = medicine.purpose,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmPrimaryTeal,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Thao tác",
                            tint = Color(0xFF64748B)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa thuốc", fontSize = 14.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            // Hàng 2: Thông tin liều dùng
            Spacer(modifier = Modifier.height(8.dp))
            val dosageDisplay = if (medicine.dosage.isNotBlank()) medicine.dosage else "1 gói"
            Text(
                text = "Liều dùng: $dosageDisplay / lần",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF64748B)
            )

            if (medicine.instruction.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Lời dặn: ${medicine.instruction}",
                    fontSize = 12.5.sp,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Hàng 3: Các cữ uống trong ngày
            if (medicine.reminderTimes.isEmpty()) {
                Text(
                    text = "Chưa đặt cữ giờ (Uống khi cần)",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    medicine.reminderTimes.forEach { time ->
                        val sessionName = getTimeSessionLabel(time)
                        val log = todayLogs.firstOrNull { it.scheduledTime == time }
                        val isTaken = log?.isTaken == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isTaken) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = if (isTaken) Color(0xFF16A34A) else Color(0xFF64748B),
                                    modifier = Modifier.size(17.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$time ($sessionName)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (isTaken) Color(0xFF15803D) else Color(0xFF334155)
                                )
                            }

                            if (isTaken) {
                                val takenAtStr = log?.actualTakenTime?.let {
                                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(it))
                                } ?: time
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFDCFCE7)
                                    ) {
                                        Text(
                                            text = "✅ Đã uống lúc $takenAtStr",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF15803D),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    if (onUndoTakeMedication != null) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Hoàn tác",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onUndoTakeMedication(time) }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { onTakeMedication(time) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = WarmPrimaryTeal
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    Text(text = "Uống ngay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Nội dung Tab 2: Lịch Khám Bệnh
 */
@Composable
fun AppointmentTabContent(
    upcomingAppointments: List<Appointment>,
    pastAppointments: List<Appointment>,
    onAddAppointment: () -> Unit,
    onCompleteAppointment: (Appointment) -> Unit,
    onEditAppointment: (Appointment) -> Unit,
    onDeleteAppointment: (Appointment) -> Unit
) {
    var isPastExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddAppointment,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = WarmPrimaryTeal,
                contentColor = Color.White
            )
        ) {
            Icon(imageVector = Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Thêm lịch hẹn khám", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Khối Lịch hẹn sắp tới
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lịch hẹn sắp tới (${upcomingAppointments.size})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }

            if (upcomingAppointments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(WarmPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EventAvailable,
                                    contentDescription = null,
                                    tint = WarmPrimaryTeal,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Không có lịch khám sắp tới",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }
            } else {
                items(upcomingAppointments, key = { it.id }) { appointment ->
                    UpcomingAppointmentCard(
                        appointment = appointment,
                        onComplete = { onCompleteAppointment(appointment) },
                        onEdit = { onEditAppointment(appointment) },
                        onDelete = { onDeleteAppointment(appointment) }
                    )
                }
            }

            // Khối Lịch sử khám cũ
            if (pastAppointments.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPastExpanded = !isPastExpanded }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Lịch sử khám cũ (${pastAppointments.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Icon(
                                imageVector = if (isPastExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF64748B)
                            )
                        }
                    }
                }

                if (isPastExpanded) {
                    items(pastAppointments, key = { it.id }) { pastAppointment ->
                        PastAppointmentCard(
                            appointment = pastAppointment,
                            onDelete = { onDeleteAppointment(pastAppointment) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thẻ Lịch hẹn sắp tới
 */
@Composable
fun UpcomingAppointmentCard(
    appointment: Appointment,
    onComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val formattedDateTime = SimpleDateFormat("HH:mm - EEEE, dd/MM/yyyy", Locale.forLanguageTag("vi-VN"))
        .format(Date(appointment.appointmentTime))

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header ngày giờ nổi bật
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(WarmPrimaryLight.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = WarmPrimaryTeal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDateTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarmTextPrimary
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Thao tác",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Chỉnh sửa", fontSize = 14.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Xóa lịch hẹn", fontSize = 14.sp, color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bệnh viện / Phòng khám
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appointment.hospitalName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            // Khoa & Bác sĩ phụ trách
            if (appointment.department.isNotBlank() || appointment.doctorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val deptDoctorText = listOf(
                        appointment.department.takeIf { it.isNotBlank() },
                        appointment.doctorName.takeIf { it.isNotBlank() }?.let { "BS. $it" }
                    ).filterNotNull().joinToString(" • ")

                    Text(
                        text = deptDoctorText,
                        fontSize = 13.sp,
                        color = Color(0xFF475569)
                    )
                }
            }

            // Ghi chú / Dặn dò
            if (appointment.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEF9C3),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFCA8A04),
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 1.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Lời dặn: ${appointment.notes}",
                            fontSize = 12.sp,
                            color = Color(0xFF854D0E),
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Nút hoàn thành lịch khám
            Button(
                onClick = onComplete,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Đánh dấu đã đi khám xong", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Thẻ Lịch sử khám cũ (Thu gọn)
 */
@Composable
fun PastAppointmentCard(
    appointment: Appointment,
    onDelete: () -> Unit
) {
    val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        .format(Date(appointment.appointmentTime))

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.hospitalName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ngày khám: $formattedDate",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                if (appointment.doctorName.isNotBlank()) {
                    Text(
                        text = "Bác sĩ: ${appointment.doctorName}",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (appointment.isCompleted) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                ) {
                    Text(
                        text = if (appointment.isCompleted) "Đã khám" else "Đã qua",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (appointment.isCompleted) Color(0xFF15803D) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog thêm/sửa thuốc thủ công
 */
@Composable
fun AddEditMedicineDialog(
    initialMedicine: Medicine?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, dosage: String, purpose: String, instruction: String, times: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialMedicine?.name.orEmpty()) }
    var purpose by remember { mutableStateOf(initialMedicine?.purpose.orEmpty()) }
    var dosage by remember {
        mutableStateOf(
            if (initialMedicine != null) {
                val parts = initialMedicine.dosage.split(" ")
                parts.firstOrNull().orEmpty().ifBlank { initialMedicine.dosage }
            } else "1"
        )
    }
    var unit by remember {
        mutableStateOf(
            if (initialMedicine != null) {
                val parts = initialMedicine.dosage.split(" ")
                if (parts.size > 1) parts.drop(1).joinToString(" ") else "viên"
            } else "viên"
        )
    }
    var instruction by remember { mutableStateOf(initialMedicine?.instruction.orEmpty()) }
    val timesList = remember {
        mutableStateListOf<String>().apply {
            if (initialMedicine != null && initialMedicine.reminderTimes.isNotEmpty()) {
                addAll(initialMedicine.reminderTimes)
            }
        }
    }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = if (initialMedicine != null) "Chỉnh Sửa Thuốc" else "Thêm Thuốc Mới",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
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
                    onValueChange = { if (it.length <= 35) name = it },
                    label = { Text("Tên thuốc *") },
                    placeholder = { Text("Ví dụ: Bột sủi thanh nhiệt Livecool") },
                    singleLine = true,
                    maxLines = 1,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = purpose,
                    onValueChange = { purpose = it },
                    label = { Text("Công dụng (nhãn 2–4 từ)") },
                    placeholder = { Text("Ví dụ: Mát gan, thanh nhiệt") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Liều dùng *") },
                        placeholder = { Text("1") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Đơn vị") },
                        placeholder = { Text("gói, viên...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.width(100.dp)
                    )
                }

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Hướng dẫn sử dụng / Lời dặn") },
                    placeholder = { Text("Ví dụ: Uống sau khi ăn no 30 phút") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Cữ giờ uống trong ngày:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )

                if (timesList.isEmpty()) {
                    Text(
                        text = "Chưa đặt cữ giờ (Uống khi cần)",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        timesList.forEach { t ->
                            InputChip(
                                selected = true,
                                onClick = { },
                                label = { Text(t, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa giờ",
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { timesList.remove(t) }
                                    )
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val formatted = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                                    if (!timesList.contains(formatted)) {
                                        timesList.add(formatted)
                                    }
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WarmPrimaryLight,
                            contentColor = WarmPrimaryTeal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Tùy chọn giờ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    val presets = listOf("08:00", "12:00", "18:00", "20:00")
                    presets.forEach { preset ->
                        if (!timesList.contains(preset)) {
                            SuggestionChip(
                                onClick = { timesList.add(preset) },
                                label = { Text("+$preset", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên thuốc", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val effectiveDosage = dosage.trim().ifBlank { "1" }
                    val fullDosage = if (unit.isNotBlank() && !effectiveDosage.contains(unit)) "$effectiveDosage $unit" else effectiveDosage
                    val finalTimes = timesList.toList()
                    onConfirm(name.trim(), fullDosage.trim(), purpose.trim(), instruction.trim(), finalTimes)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFF64748B))
            }
        }
    )
}

/**
 * Dialog thêm/sửa lịch khám bệnh
 */
@Composable
fun AddEditAppointmentDialog(
    initialAppointment: Appointment?,
    onDismiss: () -> Unit,
    onConfirm: (hospital: String, department: String, doctor: String, time: Long, notes: String) -> Unit
) {
    var hospital by remember { mutableStateOf(initialAppointment?.hospitalName.orEmpty()) }
    var department by remember { mutableStateOf(initialAppointment?.department.orEmpty()) }
    var doctor by remember { mutableStateOf(initialAppointment?.doctorName.orEmpty()) }
    var notes by remember { mutableStateOf(initialAppointment?.notes.orEmpty()) }
    var selectedCalendar by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                if (initialAppointment != null) {
                    timeInMillis = initialAppointment.appointmentTime
                } else {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9)
                    set(Calendar.MINUTE, 0)
                }
            }
        )
    }

    val context = LocalContext.current
    val dateTimeFormat = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Text(
                text = if (initialAppointment != null) "Chỉnh Sửa Lịch Khám" else "Thêm Lịch Khám Bệnh",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Bệnh viện / Phòng khám *") },
                    placeholder = { Text("Ví dụ: Bệnh viện Bạch Mai") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = department,
                        onValueChange = { department = it },
                        label = { Text("Khoa khám") },
                        placeholder = { Text("Tim mạch...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = doctor,
                        onValueChange = { doctor = it },
                        label = { Text("Bác sĩ") },
                        placeholder = { Text("BS. Nam") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Chọn Ngày & Giờ khám
                OutlinedCard(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val c = selectedCalendar
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    val newCal = Calendar.getInstance().apply {
                                        timeInMillis = selectedCalendar.timeInMillis
                                        set(Calendar.YEAR, y)
                                        set(Calendar.MONTH, m)
                                        set(Calendar.DAY_OF_MONTH, d)
                                    }
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute ->
                                            newCal.set(Calendar.HOUR_OF_DAY, hour)
                                            newCal.set(Calendar.MINUTE, minute)
                                            selectedCalendar = newCal
                                        },
                                        c.get(Calendar.HOUR_OF_DAY),
                                        c.get(Calendar.MINUTE),
                                        true
                                    ).show()
                                },
                                c.get(Calendar.YEAR),
                                c.get(Calendar.MONTH),
                                c.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Thời gian khám *", fontSize = 11.sp, color = Color(0xFF64748B))
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dateTimeFormat.format(selectedCalendar.time),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WarmTextPrimary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Chọn ngày giờ",
                            tint = WarmPrimaryTeal
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lời dặn / Ghi chú") },
                    placeholder = { Text("Ví dụ: Nhịn ăn sáng để xét nghiệm máu, mang sổ khám cũ...") },
                    minLines = 2,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (hospital.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên bệnh viện/phòng khám", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onConfirm(hospital, department, doctor, selectedCalendar.timeInMillis, notes)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFF64748B))
            }
        }
    )
}

/**
 * Dialog thêm cữ thuốc trực tiếp (Cách 1):
 * Chọn giờ cữ (Sáng / Trưa / Tối / Giờ tùy chỉnh), nhập lời dặn chung,
 * thêm danh sách nhiều loại thuốc vào cùng một cữ giờ, bấm 1 nút lưu toàn bộ.
 */
@Composable
fun AddMedicineSessionDialog(
    onDismiss: () -> Unit,
    onConfirm: (sessionTime: String, commonInstruction: String, items: List<Pair<String, String>>) -> Unit
) {
    val context = LocalContext.current
    var sessionTime by remember { mutableStateOf("08:00") }
    var commonInstruction by remember { mutableStateOf("Sau khi ăn no 30 phút") }

    // Danh sách các cặp (Tên thuốc, Liều dùng)
    val items = remember {
        mutableStateListOf(
            Pair("", "1 viên"),
            Pair("", "1 viên")
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(WarmPrimaryLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_pill),
                            contentDescription = null,
                            tint = WarmPrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Thêm Cữ Thuốc Mới",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thêm cùng lúc nhiều loại thuốc uống trong một cữ giờ",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. CHỌN GIỜ CỮ
                Text(
                    text = "1. Chọn giờ cữ uống:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                // Chips cữ giờ preset
                val presetTimes = listOf(
                    Pair("08:00", "Sáng"),
                    Pair("12:00", "Trưa"),
                    Pair("18:30", "Tối"),
                    Pair("21:30", "Đêm")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetTimes.forEach { (time, label) ->
                        val isSelected = sessionTime == time
                        FilterChip(
                            selected = isSelected,
                            onClick = { sessionTime = time },
                            label = { Text("$time ($label)", fontSize = 12.sp) }
                        )
                    }

                    FilledTonalButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    sessionTime = String.format(Locale.ROOT, "%02d:%02d", hour, minute)
                                },
                                now.get(Calendar.HOUR_OF_DAY),
                                now.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = WarmPrimaryLight,
                            contentColor = WarmPrimaryTeal
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "+ Giờ khác", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarmPrimaryLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⏰ Cữ đã chọn: $sessionTime (${getTimeSessionLabel(sessionTime)})",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = WarmPrimaryTeal,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // 2. LỜI DẶN / HƯỚNG DẪN UỐNG CHUNG
                Text(
                    text = "2. Lời dặn uống chung cho cữ này:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )

                OutlinedTextField(
                    value = commonInstruction,
                    onValueChange = { commonInstruction = it },
                    placeholder = { Text("Ví dụ: Sau khi ăn no 30 phút, uống nhiều nước...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Chips gợi ý lời dặn
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Sau ăn 30p", "Trước ăn 15p", "Uống nhiều nước", "Khi bụng đói").forEach { suggestion ->
                        SuggestionChip(
                            onClick = { commonInstruction = suggestion },
                            label = { Text(suggestion, fontSize = 11.sp) }
                        )
                    }
                }

                // 3. DANH SÁCH THUỐC
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3. Danh sách thuốc trong cữ (${items.size}):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                items.forEachIndexed { index, (name, dosage) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E6EC)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(WarmPrimaryLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WarmPrimaryTeal
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            OutlinedTextField(
                                value = name,
                                onValueChange = { newName ->
                                    if (newName.length <= 35) {
                                        items[index] = Pair(newName, dosage)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "Tên thuốc",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedTextField(
                                value = dosage,
                                onValueChange = { newDosage ->
                                    if (newDosage.length <= 15) {
                                        items[index] = Pair(name, newDosage)
                                    }
                                },
                                placeholder = {
                                    Text(
                                        text = "1 viên",
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )
                                },
                                singleLine = true,
                                maxLines = 1,
                                textStyle = LocalTextStyle.current.copy(fontSize = 13.sp, textAlign = TextAlign.Center),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.width(82.dp)
                            )

                            if (items.size > 1) {
                                IconButton(
                                    onClick = { items.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa",
                                        tint = Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Nút thêm hàng thuốc
                OutlinedButton(
                    onClick = { items.add(Pair("", "1 viên")) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Thêm viên nữa vào cữ này", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            val validItems = items.filter { it.first.isNotBlank() }
            Button(
                onClick = {
                    if (validItems.isEmpty()) {
                        Toast.makeText(context, "Vui lòng nhập ít nhất 1 tên thuốc", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    onConfirm(sessionTime, commonInstruction.trim(), validItems)
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = WarmPrimaryTeal)
            ) {
                Text("Lưu toàn bộ cữ (${validItems.size} thuốc)")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy", color = Color(0xFF64748B))
            }
        }
    )
}

/**
 * Trả về nhãn cữ buổi trong ngày dựa vào giờ (Sáng / Trưa / Chiều / Tối)
 */
fun getTimeSessionLabel(time: String): String {
    val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: 8
    return when (hour) {
        in 5..10 -> "Sáng"
        in 11..13 -> "Trưa"
        in 14..17 -> "Chiều"
        else -> "Tối"
    }
}

/**
 * Chuẩn hóa chuỗi liều lượng (ví dụ "1" -> "1 viên", "2 viên" -> "2 viên")
 */
fun formatDosageString(rawDosage: String): String {
    val trimmed = rawDosage.trim()
    if (trimmed.isBlank()) return "1 viên"
    return if (trimmed.all { it.isDigit() || it == '.' || it == ',' || it == '/' }) {
        "$trimmed viên"
    } else {
        trimmed
    }
}

/**
 * Gom nhóm thuốc tự động theo liều lượng:
 * Ví dụ: Thuốc A (1 viên), Thuốc B (1 viên) -> "Thuốc A, B (1 viên)"
 * Ví dụ có thêm C (2 viên) -> "Thuốc A, B (1 viên) · C (2 viên)"
 */
fun formatMedicineSummaryList(items: List<Pair<String, String>>): String {
    if (items.isEmpty()) return ""
    val grouped = items.groupBy { formatDosageString(it.second) }
    return grouped.entries.mapIndexed { index, (dosage, list) ->
        val names = list.map { (name, _) ->
            val clean = name.trim()
            if (clean.startsWith("Thuốc ", ignoreCase = true)) {
                clean.substring(6).trim()
            } else if (clean.startsWith("thuốc ", ignoreCase = true)) {
                clean.substring(6).trim()
            } else clean
        }.filter { it.isNotBlank() }.distinct().joinToString(", ")

        val displayNames = if (names.isBlank()) "nhiều loại" else names
        if (index == 0) "Thuốc $displayNames ($dosage)" else "$displayNames ($dosage)"
    }.joinToString(" · ")
}
