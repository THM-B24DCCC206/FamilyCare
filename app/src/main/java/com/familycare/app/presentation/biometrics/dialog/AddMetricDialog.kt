package com.familycare.app.presentation.biometrics.dialog

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import com.familycare.app.presentation.biometrics.ppg.HeartRateClassifier
import java.util.UUID

/**
 * Kết quả đánh giá chỉ số y khoa phục vụ hiển thị trực quan
 */
data class HealthEvaluation(
    val title: String,
    val containerColor: Color,
    val borderColor: Color,
    val contentColor: Color
)

/**
 * Đánh giá Huyết áp theo tiêu chuẩn Hội Tim Mạch Việt Nam / AHA
 */
fun evaluateBloodPressure(sys: Int, dia: Int): HealthEvaluation {
    return when {
        sys < 90 || dia < 60 -> HealthEvaluation(
            title = "Huyết áp thấp",
            containerColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFDE68A),
            contentColor = Color(0xFFB45309)
        )
        sys >= 140 || dia >= 90 -> HealthEvaluation(
            title = "Tăng huyết áp",
            containerColor = Color(0xFFFEE2E2),
            borderColor = Color(0xFFFCA5A5),
            contentColor = Color(0xFFB91C1C)
        )
        // Mức 120/80 mmHg tuyệt đối không được báo Tiền tăng huyết áp
        (sys in 130..139) || (dia in 81..89) -> HealthEvaluation(
            title = "Tiền tăng huyết áp",
            containerColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFDE68A),
            contentColor = Color(0xFFB45309)
        )
        sys in 120..129 || (sys <= 120 && dia == 80) -> HealthEvaluation(
            title = "Huyết áp bình thường",
            containerColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFFBBF7D0),
            contentColor = Color(0xFF2E7D32)
        )
        sys < 120 && dia < 80 -> HealthEvaluation(
            title = "Huyết áp tối ưu",
            containerColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFFBBF7D0),
            contentColor = Color(0xFF2E7D32)
        )
        else -> HealthEvaluation(
            title = "Huyết áp bình thường",
            containerColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFFBBF7D0),
            contentColor = Color(0xFF2E7D32)
        )
    }
}

/**
 * Đánh giá Đường huyết (Lúc đói / Sau ăn)
 */
fun evaluateBloodSugar(sugar: Double, timing: String): HealthEvaluation {
    return if (timing == "Lúc đói") {
        when {
            sugar < 70.0 -> HealthEvaluation(
                title = "Hạ đường huyết (Nguy hiểm)",
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5),
                contentColor = Color(0xFFB91C1C)
            )
            sugar in 70.0..99.0 -> HealthEvaluation(
                title = "Đường huyết bình thường",
                containerColor = Color(0xFFF0FDF4),
                borderColor = Color(0xFFBBF7D0),
                contentColor = Color(0xFF2E7D32)
            )
            sugar in 100.0..125.0 -> HealthEvaluation(
                title = "Tiền tiểu đường (Hơi cao)",
                containerColor = Color(0xFFFFFBEB),
                borderColor = Color(0xFFFDE68A),
                contentColor = Color(0xFFB45309)
            )
            else -> HealthEvaluation(
                title = "Đường huyết cao (Nguy cơ đái tháo đường)",
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5),
                contentColor = Color(0xFFB91C1C)
            )
        }
    } else {
        when {
            sugar < 70.0 -> HealthEvaluation(
                title = "Hạ đường huyết",
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5),
                contentColor = Color(0xFFB91C1C)
            )
            sugar in 70.0..139.0 -> HealthEvaluation(
                title = "Đường huyết sau ăn bình thường",
                containerColor = Color(0xFFF0FDF4),
                borderColor = Color(0xFFBBF7D0),
                contentColor = Color(0xFF2E7D32)
            )
            sugar in 140.0..199.0 -> HealthEvaluation(
                title = "Tiền tiểu đường (Sau ăn)",
                containerColor = Color(0xFFFFFBEB),
                borderColor = Color(0xFFFDE68A),
                contentColor = Color(0xFFB45309)
            )
            else -> HealthEvaluation(
                title = "Đường huyết sau ăn cao",
                containerColor = Color(0xFFFEE2E2),
                borderColor = Color(0xFFFCA5A5),
                contentColor = Color(0xFFB91C1C)
            )
        }
    }
}

/**
 * Đánh giá Thân nhiệt
 */
fun evaluateTemperature(temp: Double): HealthEvaluation {
    return when {
        temp < 36.0 -> HealthEvaluation(
            title = "Thân nhiệt thấp",
            containerColor = Color(0xFFFFFBEB),
            borderColor = Color(0xFFFDE68A),
            contentColor = Color(0xFFB45309)
        )
        temp in 36.0..37.5 -> HealthEvaluation(
            title = "Thân nhiệt bình thường",
            containerColor = Color(0xFFF0FDF4),
            borderColor = Color(0xFFBBF7D0),
            contentColor = Color(0xFF2E7D32)
        )
        else -> HealthEvaluation(
            title = "Sốt (Cần theo dõi / Hạ sốt)",
            containerColor = Color(0xFFFEE2E2),
            borderColor = Color(0xFFFCA5A5),
            contentColor = Color(0xFFB91C1C)
        )
    }
}

/**
 * Đánh giá Nhịp tim theo 5 dải mức độ chuẩn y tế
 */
fun evaluateHeartRate(bpm: Int): HealthEvaluation {
    val level = HeartRateClassifier.classify(bpm)
    return HealthEvaluation(
        title = "${level.title} (${level.rangeText})",
        containerColor = level.containerColor,
        borderColor = level.borderColor,
        contentColor = level.color
    )
}

@Composable
fun AddMetricDialog(
    userId: String = "user_me",
    initialType: MetricType = MetricType.BLOOD_PRESSURE,
    targetTimestamp: Long = System.currentTimeMillis(),
    initialSystolic: String = "",
    initialDiastolic: String = "",
    initialPulse: String = "",
    initialBloodSugar: String = "",
    initialTemperature: String = "",
    onDismiss: () -> Unit,
    onSave: (HealthMetricEntity, HealthMetricEntity?) -> Unit
) {
    var selectedType by remember { mutableStateOf(initialType) }

    // State Huyết áp (mặc định để trống "")
    var systolicText by remember { mutableStateOf(initialSystolic) }
    var diastolicText by remember { mutableStateOf(initialDiastolic) }
    var pulseText by remember { mutableStateOf(initialPulse) }

    // State Đường huyết
    var bloodSugarText by remember { mutableStateOf(initialBloodSugar) }
    var sugarTiming by remember { mutableStateOf("Lúc đói") }

    // State Thân nhiệt
    var tempText by remember { mutableStateOf(initialTemperature) }

    // State Nhịp tim thủ công
    var manualHeartRateText by remember { mutableStateOf(initialPulse) }

    // Ghi chú
    var notesText by remember { mutableStateOf("") }

    // Kiểm tra điều kiện có thể lưu
    val canSave = when (selectedType) {
        MetricType.BLOOD_PRESSURE -> {
            val sys = systolicText.trim().toIntOrNull()
            val dia = diastolicText.trim().toIntOrNull()
            sys != null && dia != null && sys > 0 && dia > 0
        }
        MetricType.BLOOD_SUGAR -> {
            val sugar = bloodSugarText.trim().toDoubleOrNull()
            sugar != null && sugar > 0
        }
        MetricType.TEMPERATURE -> {
            val temp = tempText.trim().toDoubleOrNull()
            temp != null && temp > 0
        }
        MetricType.HEART_RATE -> {
            val bpm = manualHeartRateText.trim().toIntOrNull()
            bpm != null && bpm > 0
        }
    }

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
                    label = {
                        Text(
                            text = "Ghi chú (Tùy chọn)",
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
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
                            if (!canSave) return@Button
                            val now = targetTimestamp
                            val metricId = UUID.randomUUID().toString()

                            when (selectedType) {
                                MetricType.BLOOD_PRESSURE -> {
                                    val sys = systolicText.trim().toDoubleOrNull() ?: return@Button
                                    val dia = diastolicText.trim().toDoubleOrNull() ?: return@Button
                                    val pulse = pulseText.trim().toDoubleOrNull()

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
                                    val sugar = bloodSugarText.trim().toDoubleOrNull() ?: return@Button
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
                                    val temp = tempText.trim().toDoubleOrNull() ?: return@Button
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
                                    val bpm = manualHeartRateText.trim().toDoubleOrNull() ?: return@Button
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
                        enabled = canSave,
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
    val sysVal = systolic.trim().toIntOrNull()
    val diaVal = diastolic.trim().toIntOrNull()

    // Chỉ hiển thị khung đánh giá khi ĐÃ nhập đủ cả 2 thông số hợp lệ
    val evaluation = if (sysVal != null && diaVal != null && sysVal > 0 && diaVal > 0) {
        evaluateBloodPressure(sysVal, diaVal)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = systolic,
                onValueChange = { if (it.length <= 3) onSystolicChange(it) },
                label = {
                    Text(
                        text = "Chỉ số trên (SYS)",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                suffix = { Text("mmHg", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = diastolic,
                onValueChange = { if (it.length <= 3) onDiastolicChange(it) },
                label = {
                    Text(
                        text = "Chỉ số dưới (DIA)",
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
                suffix = { Text("mmHg", fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            )
        }

        OutlinedTextField(
            value = pulse,
            onValueChange = { if (it.length <= 3) onPulseChange(it) },
            label = {
                Text(
                    text = "Nhịp tim (BPM)",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            suffix = { Text("BPM", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        // Khung đánh giá trực quan theo chuẩn y khoa (chỉ hiển thị khi có dữ liệu)
        if (evaluation != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = evaluation.containerColor,
                border = BorderStroke(1.dp, evaluation.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = evaluation.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đánh giá: ${evaluation.title}",
                        color = evaluation.contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
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
    val sugarVal = value.trim().toDoubleOrNull()

    // Chỉ hiển thị khung đánh giá khi người dùng đã nhập số hợp lệ
    val evaluation = if (sugarVal != null && sugarVal > 0) {
        evaluateBloodSugar(sugarVal, timing)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = {
                Text(
                    text = "Chỉ số đường huyết",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
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

        // Khung đánh giá trực quan (chỉ hiển thị khi có dữ liệu)
        if (evaluation != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = evaluation.containerColor,
                border = BorderStroke(1.dp, evaluation.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = evaluation.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đánh giá: ${evaluation.title}",
                        color = evaluation.contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
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
    val tempVal = temp.trim().toDoubleOrNull()

    // Chỉ hiển thị khung đánh giá khi có dữ liệu
    val evaluation = if (tempVal != null && tempVal > 0) {
        evaluateTemperature(tempVal)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = temp,
            onValueChange = { onTempChange(it) },
            label = {
                Text(
                    text = "Nhiệt độ cơ thể",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            suffix = { Text("°C", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (evaluation != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = evaluation.containerColor,
                border = BorderStroke(1.dp, evaluation.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = evaluation.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đánh giá: ${evaluation.title}",
                        color = evaluation.contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
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
    val bpmVal = bpm.trim().toIntOrNull()

    // Chỉ hiển thị khung đánh giá khi có dữ liệu
    val evaluation = if (bpmVal != null && bpmVal > 0) {
        evaluateHeartRate(bpmVal)
    } else null

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = bpm,
            onValueChange = { if (it.length <= 3) onBpmChange(it) },
            label = {
                Text(
                    text = "Nhịp tim máy đo (BPM)",
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            placeholder = { Text("--", color = Color(0xFF94A3B8), fontSize = 14.sp) },
            suffix = { Text("BPM", fontSize = 12.sp) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (evaluation != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = evaluation.containerColor,
                border = BorderStroke(1.dp, evaluation.borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = evaluation.contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đánh giá: ${evaluation.title}",
                        color = evaluation.contentColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
