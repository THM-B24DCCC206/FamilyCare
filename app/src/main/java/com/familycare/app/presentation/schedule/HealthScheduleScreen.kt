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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.domain.model.Appointment
import com.familycare.app.domain.model.Medicine
import com.familycare.app.presentation.home.Member
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

    var showAddMedicineDialog by remember { mutableStateOf(false) }
    var editingMedicine by remember { mutableStateOf<Medicine?>(null) }

    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<Appointment?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
            ) {
                // Tiêu đề lớn "Kế Hoạch Sức Khỏe" - Bỏ hết các icon trùng lặp ở góc phải
                Text(
                    text = "Kế Hoạch Sức Khỏe",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
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
                        onAddManual = { showAddMedicineDialog = true },
                        onScanAi = { onNavigateToOcr(uiState.selectedMemberId) },
                        onTakeMedication = { medicine, time ->
                            viewModel.markMedicationTaken(medicine, time)
                            Toast.makeText(context, "Đã ghi nhận uống thuốc ${medicine.name}", Toast.LENGTH_SHORT).show()
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
            onConfirm = { name, dosage, instruction, times ->
                if (editingMedicine != null) {
                    viewModel.updateMedicine(editingMedicine!!, name, dosage, instruction, times)
                    Toast.makeText(context, "Đã cập nhật thuốc", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.addMedicine(name, dosage, instruction, times)
                    Toast.makeText(context, "Đã thêm thuốc mới", Toast.LENGTH_SHORT).show()
                }
                showAddMedicineDialog = false
                editingMedicine = null
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(members, key = { it.id }) { member ->
            val isSelected = member.id == selectedMemberId
            val borderColor by animateColorAsState(
                if (isSelected) Color(0xFF2563EB) else Color(0xFFE2E8F0),
                label = "border"
            )
            val containerColor by animateColorAsState(
                if (isSelected) Color(0xFFEFF6FF) else MaterialTheme.colorScheme.surface,
                label = "bg"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = containerColor,
                border = BorderStroke(if (isSelected) 1.8.dp else 1.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onSelectMember(member.id) }
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    // Avatar tròn hoặc initials
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = member.initials.ifBlank { member.fullName.take(1).uppercase() },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = member.fullName,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF1E40AF) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Thanh tab gạt kép (Sub-tabs / Segmented Bar)
 */
@Composable
fun DualSegmentedBar(
    selectedTab: HealthScheduleTab,
    medicineCount: Int,
    appointmentCount: Int,
    onTabSelect: (HealthScheduleTab) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF1F5F9),
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
                shape = RoundedCornerShape(10.dp),
                color = if (isMedication) Color(0xFF2563EB) else Color.Transparent,
                shadowElevation = if (isMedication) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
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
                        tint = if (isMedication) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lịch Thuốc ($medicineCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isMedication) FontWeight.Bold else FontWeight.Medium,
                        color = if (isMedication) Color.White else Color(0xFF475569)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Tab 2: [ 🩺 Lịch Khám Bệnh (Y) ]
            val isAppointment = selectedTab == HealthScheduleTab.APPOINTMENT
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isAppointment) Color(0xFF2563EB) else Color.Transparent,
                shadowElevation = if (isAppointment) 2.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTabSelect(HealthScheduleTab.APPOINTMENT) }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EventNote,
                        contentDescription = null,
                        tint = if (isAppointment) Color.White else Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Lịch Khám ($appointmentCount)",
                        fontSize = 13.sp,
                        fontWeight = if (isAppointment) FontWeight.Bold else FontWeight.Medium,
                        color = if (isAppointment) Color.White else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

/**
 * Nội dung Tab 1: Quản lý Thuốc
 */
@Composable
fun MedicationTabContent(
    medicines: List<Medicine>,
    todayLogs: List<MedicationLogEntity>,
    onAddManual: () -> Unit,
    onScanAi: () -> Unit,
    onTakeMedication: (Medicine, String) -> Unit,
    onEditMedicine: (Medicine) -> Unit,
    onDeleteMedicine: (Medicine) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // 2 nút hành động nhanh: [ + Thêm thủ công ] và [ 📷 Quét đơn thuốc AI ]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilledTonalButton(
                onClick = onAddManual,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = Color(0xFFEFF6FF),
                    contentColor = Color(0xFF1E40AF)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Thêm thủ công", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onScanAi,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2563EB),
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
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Medication,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Chưa có lịch uống thuốc",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Thêm thủ công hoặc dùng tính năng Quét đơn thuốc AI để tự động tạo lịch uống thuốc.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
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
                        onEdit = { onEditMedicine(medicine) },
                        onDelete = { onDeleteMedicine(medicine) }
                    )
                }
            }
        }
    }
}

/**
 * Thẻ thuốc tối ưu: Tên + hàm lượng + đơn vị + tách từng cữ uống trong ngày + nút 3 chấm
 */
@Composable
fun MedicationCard(
    medicine: Medicine,
    todayLogs: List<MedicationLogEntity>,
    onTakeMedication: (time: String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Hàng tiêu đề: Tên thuốc in đậm kèm hàm lượng + nút 3 chấm
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon viên thuốc
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEFF6FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = medicine.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (medicine.dosage.isNotBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = medicine.dosage,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    if (medicine.instruction.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = medicine.instruction,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Nút ba chấm góc thẻ
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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(10.dp))

            // Tách từng cữ uống trong ngày: 08:00 (Sáng) -> [ Uống ngay ] / [ ✅ Đã uống lúc ... ]
            if (medicine.reminderTimes.isEmpty()) {
                Text(
                    text = "Chưa thiết lập cữ uống trong ngày",
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
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isTaken) Color(0xFFF0FDF4) else Color(0xFFF8FAFC))
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Outlined.AccessTime,
                                    contentDescription = null,
                                    tint = if (isTaken) Color(0xFF16A34A) else Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
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
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "✅ Đã uống lúc $takenAtStr",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            } else {
                                Button(
                                    onClick = { onTakeMedication(time) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2563EB)
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
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
        // Nút hành động nhanh: [ + Thêm lịch hẹn khám ]
        Button(
            onClick = onAddAppointment,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
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
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            if (upcomingAppointments.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventAvailable,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Không có lịch khám sắp tới",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
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

            // Khối Lịch sử khám cũ (Thu gọn / Mở rộng)
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
 * Thẻ Lịch hẹn sắp tới nổi bật: Ngày giờ khám, Bệnh viện, Bác sĩ, Lý do, Lời dặn
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header ngày giờ nổi bật
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEFF6FF))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = formattedDateTime,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E40AF)
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
                    color = MaterialTheme.colorScheme.onSurface
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
                    .height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Đánh dấu đã đi khám xong", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appointment.hospitalName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
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
                        fontSize = 11.sp,
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
                        fontWeight = FontWeight.SemiBold,
                        color = if (appointment.isCompleted) Color(0xFF166534) else Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
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
    onConfirm: (name: String, dosage: String, instruction: String, times: List<String>) -> Unit
) {
    var name by remember { mutableStateOf(initialMedicine?.name.orEmpty()) }
    var dosage by remember { mutableStateOf(initialMedicine?.dosage.orEmpty()) }
    var unit by remember { mutableStateOf("viên") }
    var instruction by remember { mutableStateOf(initialMedicine?.instruction.orEmpty()) }
    val timesList = remember {
        mutableStateListOf<String>().apply {
            if (initialMedicine != null && initialMedicine.reminderTimes.isNotEmpty()) {
                addAll(initialMedicine.reminderTimes)
            } else {
                add("08:00")
            }
        }
    }

    var customTimeInput by remember { mutableStateOf("") }
    var showTimePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMedicine != null) "Chỉnh Sửa Thuốc" else "Thêm Thuốc Mới",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
                    onValueChange = { name = it },
                    label = { Text("Tên thuốc *") },
                    placeholder = { Text("Ví dụ: Amlodipine") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Hàm lượng *") },
                        placeholder = { Text("5mg, 500mg...") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Đơn vị") },
                        placeholder = { Text("viên, gói...") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp)
                    )
                }

                OutlinedTextField(
                    value = instruction,
                    onValueChange = { instruction = it },
                    label = { Text("Hướng dẫn sử dụng") },
                    placeholder = { Text("Ví dụ: Uống sau khi ăn no 30 phút") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Cữ giờ uống trong ngày:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Danh sách giờ đã chọn dạng Chip
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

                // Gợi ý các giờ phổ biến
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val presets = listOf("08:00", "12:00", "18:00", "20:00")
                    presets.forEach { preset ->
                        if (!timesList.contains(preset)) {
                            SuggestionChip(
                                onClick = { timesList.add(preset) },
                                label = { Text("+ $preset", fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || dosage.isBlank()) {
                        Toast.makeText(context, "Vui lòng nhập tên thuốc và hàm lượng", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val fullDosage = if (unit.isNotBlank() && !dosage.contains(unit)) "$dosage $unit" else dosage
                    val finalTimes = if (timesList.isEmpty()) listOf("08:00") else timesList.toList()
                    onConfirm(name, fullDosage, instruction, finalTimes)
                }
            ) {
                Text("Lưu")
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
        title = {
            Text(
                text = if (initialAppointment != null) "Chỉnh Sửa Lịch Khám" else "Thêm Lịch Khám Bệnh",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = doctor,
                        onValueChange = { doctor = it },
                        label = { Text("Bác sĩ") },
                        placeholder = { Text("BS. Nam") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Chọn Ngày & Giờ khám
                OutlinedCard(
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
                                color = Color(0xFF1E40AF)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = "Chọn ngày giờ",
                            tint = Color(0xFF2563EB)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Lời dặn / Ghi chú") },
                    placeholder = { Text("Ví dụ: Nhịn ăn sáng để xét nghiệm máu, mang sổ khám cũ...") },
                    minLines = 2,
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
                }
            ) {
                Text("Lưu")
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
