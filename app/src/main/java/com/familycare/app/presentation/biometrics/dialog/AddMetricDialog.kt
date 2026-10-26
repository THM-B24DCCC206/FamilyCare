package com.familycare.app.presentation.biometrics.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import java.util.UUID

@Composable
fun AddMetricDialog(
    userId: String = "user_me",
    initialType: MetricType = MetricType.BLOOD_PRESSURE,
    onDismiss: () -> Unit,
    onSave: (HealthMetricEntity, HealthMetricEntity?) -> Unit // Trả về metric chính và metric nhịp tim phụ (nếu có)
) {
    var selectedType by remember { mutableStateOf(initialType) }

    // State Huyết áp
    var systolicText by remember { mutableStateOf("120") }
    var diastolicText by remember { mutableStateOf("80") }
    var pulseText by remember { mutableStateOf("75") }

    // State Đường huyết
    var bloodSugarText by remember { mutableStateOf("100") }
    var sugarTiming by remember { mutableStateOf("Lúc đói") } // "Lúc đói", "Sau ăn 2h", "Bất kỳ"

    // State Thân nhiệt
    var tempText by remember { mutableStateOf("36.8") }

    // State Nhịp tim thủ công
    var manualHeartRateText by remember { mutableStateOf("75") }

    // Ghi chú
    var notesText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Tiêu đề
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Nhập Chỉ Số Sức Khỏe",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thanh chọn loại chỉ số (Segmented Chips)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTypeChip(
                        label = "Huyết áp",
                        icon = Icons.Default.Speed,
                        selected = selectedType == MetricType.BLOOD_PRESSURE,
                        onClick = { selectedType = MetricType.BLOOD_PRESSURE },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTypeChip(
                        label = "Đường huyết",
                        icon = Icons.Default.WaterDrop,
                        selected = selectedType == MetricType.BLOOD_SUGAR,
                        onClick = { selectedType = MetricType.BLOOD_SUGAR },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricTypeChip(
                        label = "Thân nhiệt",
                        icon = Icons.Default.Thermostat,
                        selected = selectedType == MetricType.TEMPERATURE,
                        onClick = { selectedType = MetricType.TEMPERATURE },
                        modifier = Modifier.weight(1f)
                    )
                    MetricTypeChip(
                        label = "Nhịp tim",
                        icon = Icons.Default.Favorite,
                        selected = selectedType == MetricType.HEART_RATE,
                        onClick = { selectedType = MetricType.HEART_RATE },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Nội dung form tương ứng với từng loại
                when (selectedType) {
                    MetricType.BLOOD_PRESSURE -> {
                        BloodPressureForm(
                            systolic = systolicText,
                            onSystolicChange = { systolicText = it },
                            diastolic = diastolicText,
                            onDiastolicChange = { diastolicText = it },
                            pulse = pulseText,
                            onPulseChange = { pulseText = it }
                        )
                    }
                    MetricType.BLOOD_SUGAR -> {
                        BloodSugarForm(
                            value = bloodSugarText,
                            onValueChange = { bloodSugarText = it },
                            timing = sugarTiming,
                            onTimingChange = { sugarTiming = it }
                        )
                    }
                    MetricType.TEMPERATURE -> {
                        TemperatureForm(
                            temp = tempText,
                            onTempChange = { tempText = it }
                        )
                    }
                    MetricType.HEART_RATE -> {
                        ManualHeartRateForm(
                            bpm = manualHeartRateText,
                            onBpmChange = { manualHeartRateText = it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Ô nhập Ghi chú chung
                OutlinedTextField(
                    value = notesText,
                    onValueChange = { notesText = it },
                    label = { Text("Ghi chú (Tùy chọn)") },
                    placeholder = { Text("VD: Đo sau ngủ dậy, vừa uống thuốc...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Nút Lưu / Hủy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val metricId = UUID.randomUUID().toString()

                            when (selectedType) {
                                MetricType.BLOOD_PRESSURE -> {
                                    val sys = systolicText.toDoubleOrNull() ?: 120.0
                                    val dia = diastolicText.toDoubleOrNull() ?: 80.0
                                    val pulse = pulseText.toDoubleOrNull()

                                    val bpMetric = HealthMetricEntity(
                                        id = metricId,
                                        userId = userId,
                                        type = MetricType.BLOOD_PRESSURE.name,
                                        valuePrimary = sys,
                                        valueSecondary = dia,
                                        unit = "mmHg",
                                        timestamp = now,
                                        notes = notesText,
                                        isFromCameraPpg = false
                                    )

                                    val pulseMetric = if (pulse != null && pulse > 0) {
                                        HealthMetricEntity(
                                            id = UUID.randomUUID().toString(),
                                            userId = userId,
                                            type = MetricType.HEART_RATE.name,
                                            valuePrimary = pulse,
                                            valueSecondary = null,
                                            unit = "BPM",
                                            timestamp = now,
                                            notes = "Kèm lần đo huyết áp",
                                            isFromCameraPpg = false
                                        )
                                    } else null

                                    onSave(bpMetric, pulseMetric)
                                }
                                MetricType.BLOOD_SUGAR -> {
                                    val sugar = bloodSugarText.toDoubleOrNull() ?: 100.0
                                    val fullNotes = if (notesText.isNotBlank()) "$sugarTiming - $notesText" else sugarTiming
                                    val sugarMetric = HealthMetricEntity(
                                        id = metricId,
                                        userId = userId,
                                        type = MetricType.BLOOD_SUGAR.name,
                                        valuePrimary = sugar,
                                        valueSecondary = null,
                                        unit = "mg/dL",
                                        timestamp = now,
                                        notes = fullNotes,
                                        isFromCameraPpg = false
                                    )
                                    onSave(sugarMetric, null)
                                }
                                MetricType.TEMPERATURE -> {
                                    val temp = tempText.toDoubleOrNull() ?: 36.8
                                    val tempMetric = HealthMetricEntity(
                                        id = metricId,
                                        userId = userId,
                                        type = MetricType.TEMPERATURE.name,
                                        valuePrimary = temp,
                                        valueSecondary = null,
                                        unit = "°C",
                                        timestamp = now,
                                        notes = notesText,
                                        isFromCameraPpg = false
                                    )
                                    onSave(tempMetric, null)
                                }
                                MetricType.HEART_RATE -> {
                                    val bpm = manualHeartRateText.toDoubleOrNull() ?: 75.0
                                    val hrMetric = HealthMetricEntity(
                                        id = metricId,
                                        userId = userId,
                                        type = MetricType.HEART_RATE.name,
                                        valuePrimary = bpm,
                                        valueSecondary = null,
                                        unit = "BPM",
                                        timestamp = now,
                                        notes = notesText.ifBlank { "Nhập thủ công" },
                                        isFromCameraPpg = false
                                    )
                                    onSave(hrMetric, null)
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Lưu Chỉ Số", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricTypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------------- 1. Form Huyết Áp ----------------
@Composable
private fun BloodPressureForm(
    systolic: String,
    onSystolicChange: (String) -> Unit,
    diastolic: String,
    onDiastolicChange: (String) -> Unit,
    pulse: String,
    onPulseChange: (String) -> Unit
) {
    val sysVal = systolic.toIntOrNull() ?: 120
    val diaVal = diastolic.toIntOrNull() ?: 80

    // Phân loại y tế theo khuyến cáo Hội Tim Mạch Việt Nam / AHA
    val (statusText, statusColor) = when {
        sysVal < 90 || diaVal < 60 -> Pair("Huyết áp thấp", Color(0xFFFB8C00))
        sysVal < 120 && diaVal < 80 -> Pair("Huyết áp tối ưu (Bình thường)", Color(0xFF2E7D32))
        sysVal in 120..129 && diaVal < 80 -> Pair("Huyết áp bình thường cao", Color(0xFF43A047))
        sysVal in 130..139 || diaVal in 80..89 -> Pair("Tiền tăng huyết áp (Độ 1 nhẹ)", Color(0xFFE65100))
        sysVal in 140..159 || diaVal in 90..99 -> Pair("Tăng huyết áp Độ 1", Color(0xFFD32F2F))
        else -> Pair("Tăng huyết áp Độ 2 (Cần chú ý!)", Color(0xFFB71C1C))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = systolic,
                onValueChange = { if (it.length <= 3) onSystolicChange(it) },
                label = { Text("Tâm thu (SYS)") },
                placeholder = { Text("120") },
                suffix = { Text("mmHg", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = diastolic,
                onValueChange = { if (it.length <= 3) onDiastolicChange(it) },
                label = { Text("Tâm trương (DIA)") },
                placeholder = { Text("80") },
                suffix = { Text("mmHg", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = pulse,
            onValueChange = { if (it.length <= 3) onPulseChange(it) },
            label = { Text("Nhịp tim hiển thị trên máy (PULSE)") },
            placeholder = { Text("75") },
            suffix = { Text("BPM", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Đánh giá trực quan
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đánh giá: $statusText",
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------- 2. Form Đường Huyết ----------------
@Composable
private fun BloodSugarForm(
    value: String,
    onValueChange: (String) -> Unit,
    timing: String,
    onTimingChange: (String) -> Unit
) {
    val sugarVal = value.toDoubleOrNull() ?: 100.0

    val (statusText, statusColor) = when (timing) {
        "Lúc đói" -> when {
            sugarVal < 70 -> Pair("Hạ đường huyết (Nguy hiểm)", Color(0xFFD32F2F))
            sugarVal in 70.0..99.0 -> Pair("Đường huyết lúc đói bình thường", Color(0xFF2E7D32))
            sugarVal in 100.0..125.0 -> Pair("Tiền tiểu đường (Cần ăn uống kiêng)", Color(0xFFE65100))
            else -> Pair("Đường huyết cao (Nguy cơ đái tháo đường)", Color(0xFFD32F2F))
        }
        else -> when {
            sugarVal < 140.0 -> Pair("Đường huyết sau ăn bình thường", Color(0xFF2E7D32))
            sugarVal in 140.0..199.0 -> Pair("Tiền tiểu đường (Hơi cao)", Color(0xFFE65100))
            else -> Pair("Đường huyết sau ăn cao", Color(0xFFD32F2F))
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = { Text("Chỉ số đường huyết") },
            placeholder = { Text("100") },
            suffix = { Text("mg/dL", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Text("Thời điểm đo:", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Lúc đói", "Sau ăn 2h", "Trước ngủ").forEach { option ->
                FilterChip(
                    selected = timing == option,
                    onClick = { onTimingChange(option) },
                    label = { Text(option, fontSize = 12.sp) }
                )
            }
        }

        // Đánh giá trực quan
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đánh giá: $statusText",
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------- 3. Form Thân Nhiệt ----------------
@Composable
private fun TemperatureForm(
    temp: String,
    onTempChange: (String) -> Unit
) {
    val tempVal = temp.toDoubleOrNull() ?: 36.8

    val (statusText, statusColor) = when {
        tempVal < 36.0 -> Pair("Thân nhiệt thấp", Color(0xFF1976D2))
        tempVal in 36.0..37.5 -> Pair("Thân nhiệt bình thường", Color(0xFF2E7D32))
        tempVal in 37.6..38.5 -> Pair("Sốt nhẹ (Theo dõi thêm)", Color(0xFFFB8C00))
        else -> Pair("Sốt cao (Cần uống thuốc hạ sốt)", Color(0xFFD32F2F))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = temp,
            onValueChange = { onTempChange(it) },
            label = { Text("Nhiệt độ cơ thể") },
            placeholder = { Text("36.8") },
            suffix = { Text("°C", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đánh giá: $statusText",
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------- 4. Form Nhịp Tim Thủ Công ----------------
@Composable
private fun ManualHeartRateForm(
    bpm: String,
    onBpmChange: (String) -> Unit
) {
    val bpmVal = bpm.toIntOrNull() ?: 75

    val (statusText, statusColor) = when {
        bpmVal < 60 -> Pair("Nhịp tim chậm (< 60 BPM)", Color(0xFF1976D2))
        bpmVal in 60..100 -> Pair("Nhịp tim bình thường (60 - 100 BPM)", Color(0xFF2E7D32))
        else -> Pair("Nhịp tim nhanh (> 100 BPM)", Color(0xFFD32F2F))
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = bpm,
            onValueChange = { if (it.length <= 3) onBpmChange(it) },
            label = { Text("Nhịp tim đo bằng thiết bị ngoài") },
            placeholder = { Text("75") },
            suffix = { Text("BPM", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        Surface(
            shape = RoundedCornerShape(10.dp),
            color = statusColor.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Đánh giá: $statusText",
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}
