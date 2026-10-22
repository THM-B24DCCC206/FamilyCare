
package com.familycare.app.presentation.medication

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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

    fun startEditing(medicine: com.familycare.app.domain.model.Medicine) {
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
                }
            )
        },
        floatingActionButton = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FloatingActionButton(
                    onClick = {
                        resetForm()
                        editingMedicine = null
                        showAddDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm thuốc")
                }

                ExtendedFloatingActionButton(
                    onClick = onNavigateToOcr,
                    icon = {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = null
                        )
                    },
                    text = { Text("Quét đơn thuốc AI") }
                )
            }
        }
    ) { paddingValues ->
        if (medicines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                Text("Chưa có thuốc. Nhấn nút + để thêm thuốc.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(medicines, key = { it.id }) { medicine ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        medicine.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Liều lượng: ${medicine.dosage}")
                                    Text(
                                        "Giờ uống: ${
                                            medicine.reminderTimes.joinToString(", ")
                                                .ifBlank { "Chưa thiết lập" }
                                        }"
                                    )

                                    if (medicine.instruction.isNotBlank()) {
                                        Text("Hướng dẫn: ${medicine.instruction}")
                                    }
                                }

                                Column {
                                    IconButton(
                                        onClick = { startEditing(medicine) }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Sửa thuốc"
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            medicationViewModel.deleteMedicine(medicine.id)
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Xóa thuốc"
                                        )
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
                Text(if (isEditing) "Sửa thông tin thuốc" else "Thêm thuốc")
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
                                        Locale.US, "%02d:%02d", hour, minute
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
                        Icon(Icons.Default.Add, contentDescription = null)
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "🕒  $time",
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
                                Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(it)
                            }

                        if (name.isNotBlank() &&
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
                                    reminderTimes = reminderTimes.distinct().sorted()
                                )
                            } else {
                                medicationViewModel.addMedicine(
                                    name = name,
                                    dosage = dosage,
                                    instruction = instruction,
                                    reminderTimes = reminderTimes.distinct().sorted()
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
