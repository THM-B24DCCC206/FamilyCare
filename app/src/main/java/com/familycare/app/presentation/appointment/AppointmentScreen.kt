package com.familycare.app.presentation.appointment

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.domain.model.Appointment
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    appointmentViewModel: AppointmentViewModel = viewModel()
) {
    val appointments by appointmentViewModel.appointments.collectAsState()
    val context = LocalContext.current

    // Xin quyền thông báo trên Android 13 trở lên.
    val notificationPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) {
            // Nếu người dùng từ chối, màn hình lịch khám vẫn hoạt động.
        }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS

            if (
                ContextCompat.checkSelfPermission(
                    context,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(permission)
            }
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    var appointmentToEdit by remember {
        mutableStateOf<Appointment?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch khám") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("＋ Thêm lịch khám")
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (appointments.isEmpty()) {
                Text("Chưa có lịch khám. Hãy thêm lịch khám mới.")
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = appointments,
                        key = { it.id }
                    ) { appointment ->
                        AppointmentCard(
                            appointment = appointment,
                            onComplete = {
                                appointmentViewModel.markAsCompleted(
                                    appointment
                                )
                            },
                            onDelete = {
                                appointmentViewModel.deleteAppointment(
                                    appointment.id
                                )
                            },
                            onEdit = {
                                appointmentToEdit = appointment
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddAppointmentDialog(
            onDismiss = { showAddDialog = false },
            onSave = { hospital, department, doctor, time, notes ->
                appointmentViewModel.addAppointment(
                    hospitalName = hospital,
                    department = department,
                    doctorName = doctor,
                    appointmentTime = time,
                    notes = notes
                )
                showAddDialog = false
            }
        )
    }

    val editingAppointment = appointmentToEdit
    if (editingAppointment != null) {
        AddAppointmentDialog(
            initialAppointment = editingAppointment,
            dialogTitle = "Sửa lịch khám",
            saveButtonText = "Lưu thay đổi",
            onDismiss = { appointmentToEdit = null },
            onSave = { hospital, department, doctor, time, notes ->
                appointmentViewModel.updateAppointment(
                    editingAppointment.copy(
                        hospitalName = hospital,
                        department = department,
                        doctorName = doctor,
                        appointmentTime = time,
                        notes = notes
                    )
                )
                appointmentToEdit = null
            }
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val formatter = remember {
        SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = appointment.hospitalName,
                style = androidx.compose.material3.MaterialTheme
                    .typography.titleMedium
            )

            if (appointment.department.isNotBlank()) {
                Text("Chuyên khoa: ${appointment.department}")
            }

            if (appointment.doctorName.isNotBlank()) {
                Text("Bác sĩ: ${appointment.doctorName}")
            }

            Text(
                text = "Thời gian: ${
                    formatter.format(Date(appointment.appointmentTime))
                }"
            )

            if (appointment.notes.isNotBlank()) {
                Text("Ghi chú: ${appointment.notes}")
            }

            Text(
                text = if (appointment.isCompleted) {
                    "Trạng thái: Đã khám"
                } else {
                    "Trạng thái: Chưa khám"
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!appointment.isCompleted) {
                    Button(onClick = onComplete) {
                        Text("Đã khám")
                    }
                }

                TextButton(onClick = onEdit) {
                    Text("Sửa")
                }

                TextButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Xóa lịch khám"
                    )
                    Text(" Xóa")
                }
            }
        }
    }
}

@Composable
private fun AddAppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (
        hospital: String,
        department: String,
        doctor: String,
        time: Long,
        notes: String
    ) -> Unit,
    initialAppointment: Appointment? = null,
    dialogTitle: String = "Thêm lịch khám",
    saveButtonText: String = "Lưu"
) {
    val context = LocalContext.current

    var hospital by remember(initialAppointment?.id) {
        mutableStateOf(initialAppointment?.hospitalName.orEmpty())
    }
    var department by remember(initialAppointment?.id) {
        mutableStateOf(initialAppointment?.department.orEmpty())
    }
    var doctor by remember(initialAppointment?.id) {
        mutableStateOf(initialAppointment?.doctorName.orEmpty())
    }
    var notes by remember(initialAppointment?.id) {
        mutableStateOf(initialAppointment?.notes.orEmpty())
    }

    var selectedTime by remember(initialAppointment?.id) {
        mutableStateOf(
            initialAppointment?.appointmentTime
                ?: Calendar.getInstance().timeInMillis
        )
    }

    val formatter = remember {
        SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = hospital,
                    onValueChange = { hospital = it },
                    label = { Text("Bệnh viện / phòng khám *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Chuyên khoa") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = doctor,
                    onValueChange = { doctor = it },
                    label = { Text("Tên bác sĩ") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text(
                    "Thời gian: ${formatter.format(Date(selectedTime))}"
                )

                Button(
                    onClick = {
                        val current = Calendar.getInstance().apply {
                            timeInMillis = selectedTime
                        }

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                val updated = Calendar.getInstance().apply {
                                    timeInMillis = selectedTime
                                    set(Calendar.YEAR, year)
                                    set(Calendar.MONTH, month)
                                    set(Calendar.DAY_OF_MONTH, day)
                                }

                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        updated.set(
                                            Calendar.HOUR_OF_DAY,
                                            hour
                                        )
                                        updated.set(
                                            Calendar.MINUTE,
                                            minute
                                        )
                                        updated.set(Calendar.SECOND, 0)
                                        updated.set(Calendar.MILLISECOND, 0)

                                        selectedTime = updated.timeInMillis
                                    },
                                    current.get(Calendar.HOUR_OF_DAY),
                                    current.get(Calendar.MINUTE),
                                    true
                                ).show()
                            },
                            current.get(Calendar.YEAR),
                            current.get(Calendar.MONTH),
                            current.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Chọn ngày và giờ")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        hospital.isNotBlank() &&
                        selectedTime > 0L
                    ) {
                        onSave(
                            hospital.trim(),
                            department.trim(),
                            doctor.trim(),
                            selectedTime,
                            notes.trim()
                        )
                    }
                },
                enabled = hospital.isNotBlank()
            ) {
                Text(saveButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy")
            }
        }
    )
}
