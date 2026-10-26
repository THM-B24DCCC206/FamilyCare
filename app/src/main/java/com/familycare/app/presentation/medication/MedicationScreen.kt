package com.familycare.app.presentation.medication

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onNavigateToOcr: () -> Unit,
    medicationViewModel: MedicationViewModel = viewModel()
) {
    val medicines by medicationViewModel.medicines.collectAsState()
    val todayLogs by medicationViewModel.todayLogs.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMedicine by remember {
        mutableStateOf<com.familycare.app.domain.model.Medicine?>(null)
    }

    var name by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var instruction by remember { mutableStateOf("") }
    var reminderTimes by remember { mutableStateOf(listOf("08:00")) }
    var validationError by remember { mutableStateOf(false) }

    fun resetForm() {
        name = ""
        dosage = ""
        instruction = ""
        reminderTimes = listOf("08:00")
        validationError = false
    }

    fun startEditing(
        medicine: com.familycare.app.domain.model.Medicine
    ) {
        editingMedicine = medicine
        name = medicine.name
        dosage = medicine.dosage
        instruction = medicine.instruction
        reminderTimes = medicine.reminderTimes.distinct()
        validationError = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quản Lý Thuốc & Lịch Uống",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToOcr) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Quét OCR đơn thuốc"
                        )
                    }
                    IconButton(onClick = {
                        resetForm()
                        editingMedicine = null
                        showAddDialog = true
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Thêm thuốc"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->

        if (medicines.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MedicalServices,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Chưa có thuốc trong danh sách",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Thêm thuốc thủ công hoặc chụp ảnh đơn thuốc để AI tự động trích xuất lịch uống.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            resetForm()
                            editingMedicine = null
                            showAddDialog = true
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Thêm thuốc")
                    }
                    OutlinedButton(
                        onClick = onNavigateToOcr
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Quét OCR AI")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                resetForm()
                                editingMedicine = null
                                showAddDialog = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Thêm thuốc")
                        }
                        OutlinedButton(
                            onClick = onNavigateToOcr,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Quét OCR AI")
                        }
                    }
                }
                items(medicines, key = { it.id }) { medicine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = medicine.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text("Liều lượng: ${medicine.dosage}")

                                    Text(
                                        text = "Giờ uống: ${
                                            medicine.reminderTimes
                                                .joinToString(", ")
                                                .ifBlank { "Chưa thiết lập" }
                                        }"
                                    )

                                    if (medicine.instruction.isNotBlank()) {
                                        Text(
                                            "Hướng dẫn: ${medicine.instruction}"
                                        )
                                    }
                                }

                                Column {
                                    IconButton(
                                        onClick = {
                                            startEditing(medicine)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Sửa thuốc"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            medicationViewModel.deleteMedicine(
                                                medicine.id
                                            )
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Xóa thuốc"
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Text(
                                text = "Xác nhận uống thuốc hôm nay",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )


                            if (medicine.reminderTimes.isEmpty()) {
                                Text(
                                    text = "Thuốc này chưa có giờ nhắc uống.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                OutlinedButton(
                                    onClick = { startEditing(medicine) },
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Text("Thiết lập giờ uống")
                                }
                            } else {
                                medicine.reminderTimes
                                    .distinct()
                                    .sorted()
                                    .forEach { time ->

                                        val log = todayLogs.firstOrNull {
                                            it.medicineId == medicine.id &&
                                                it.scheduledTime.substringAfterLast(" ") == time
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .padding(vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "🕒 $time",
                                                    style = MaterialTheme.typography.bodyLarge
                                                )

                                                Text(
                                                    text = when {
                                                        log?.isTaken == true -> "Đã xác nhận uống"
                                                        log == null -> "Chưa có nhật ký uống thuốc"
                                                        else -> "Chưa xác nhận"
                                                    },
                                                    color = if (log?.isTaken == true) {
                                                        Color(0xFF2E7D32)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    },
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }

                                            if (log?.isTaken == true) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Đã uống",
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.padding(
                                                        top = 12.dp,
                                                        end = 12.dp
                                                    )
                                                )
                                            } else {
                                                Button(
                                                    onClick = {
                                                        medicationViewModel.markTimeAsTaken(
                                                            medicine = medicine,
                                                            time = time
                                                        )
                                                    }
                                                ) {
                                                    Text("Đã uống")
                                                }
                                            }
                                        }
                                    }
                            

                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog || editingMedicine != null) {
        val isEditing = editingMedicine != null
        val context = androidx.compose.ui.platform.LocalContext.current

        AlertDialog(
            onDismissRequest = {
                showAddDialog = false
                editingMedicine = null
                resetForm()
            },
            title = {
                Text(
                    if (isEditing) "Sửa thông tin thuốc"
                    else "Thêm thuốc"
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên thuốc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("Liều lượng (ví dụ: 500mg)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "Giờ nhắc uống thuốc",
                        style = MaterialTheme.typography.labelLarge
                    )

                    OutlinedButton(
                        onClick = {
                            val calendar = Calendar.getInstance()

                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    val selectedTime = String.format(
                                        Locale.US,
                                        "%02d:%02d",
                                        hour,
                                        minute
                                    )

                                    if (selectedTime !in reminderTimes) {
                                        reminderTimes =
                                            (reminderTimes + selectedTime).sorted()
                                    }
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true
                            ).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Chọn giờ bằng đồng hồ")
                    }

                    if (reminderTimes.isEmpty()) {
                        Text(
                            "Chưa chọn giờ uống thuốc",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        reminderTimes.forEach { time ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "🕒 $time",
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                TextButton(
                                    onClick = {
                                        reminderTimes = reminderTimes - time
                                    }
                                ) {
                                    Text("Xóa")
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = instruction,
                        onValueChange = { instruction = it },
                        label = { Text("Hướng dẫn uống thuốc") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError) {
                        Text(
                            "Vui lòng nhập tên, liều lượng và chọn ít nhất một giờ hợp lệ.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val validTimes = reminderTimes.isNotEmpty() &&
                            reminderTimes.all {
                                Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
                                    .matches(it)
                            }

                        if (
                            name.isNotBlank() &&
                            dosage.isNotBlank() &&
                            validTimes
                        ) {
                            val currentMedicine = editingMedicine

                            if (currentMedicine != null) {
                                medicationViewModel.updateMedicine(
                                    medicine = currentMedicine,
                                    name = name,
                                    dosage = dosage,
                                    instruction = instruction,
                                    reminderTimes = reminderTimes
                                        .distinct()
                                        .sorted()
                                )
                            } else {
                                medicationViewModel.addMedicine(
                                    name = name,
                                    dosage = dosage,
                                    instruction = instruction,
                                    reminderTimes = reminderTimes
                                        .distinct()
                                        .sorted()
                                )
                            }

                            showAddDialog = false
                            editingMedicine = null
                            resetForm()
                        } else {
                            validationError = true
                        }
                    }
                ) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        editingMedicine = null
                        resetForm()
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}
