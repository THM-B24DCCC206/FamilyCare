package com.familycare.app.presentation.biometrics.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.domain.model.MetricType
import java.text.SimpleDateFormat
import java.util.*

data class ChartPoint(
    val timestamp: Long,
    val value: Float,
    val valueSecondary: Float? = null,
    val label: String
)

@Composable
fun BiometricsLineChart(
    metrics: List<HealthMetricEntity>,
    modifier: Modifier = Modifier
) {
    var selectedMetricType by remember { mutableStateOf(MetricType.HEART_RATE) }
    var selectedTimeRange by remember { mutableIntStateOf(7) } // 7 ngày hoặc 30 ngày
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    // Lọc dữ liệu theo thời gian & loại chỉ số
    val cutoffTime = remember(selectedTimeRange) {
        System.currentTimeMillis() - (selectedTimeRange * 24 * 60 * 60 * 1000L)
    }

    val filteredMetrics = remember(metrics, cutoffTime, selectedMetricType) {
        metrics.filter { it.timestamp >= cutoffTime && it.type == selectedMetricType.name }
            .sortedBy { it.timestamp }
    }

    // Chuyển đổi dữ liệu sang điểm biểu đồ
    val points = remember(filteredMetrics) {
        if (filteredMetrics.isEmpty()) {
            emptyList()
        } else {
            filteredMetrics.map {
                ChartPoint(
                    timestamp = it.timestamp,
                    value = it.valuePrimary.toFloat(),
                    valueSecondary = it.valueSecondary?.toFloat(),
                    label = dateFormat.format(Date(it.timestamp))
                )
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFECEFF1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. Tab chuyển loại chỉ số biểu đồ (Nhịp tim / Huyết áp / Đường huyết)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMetricType == MetricType.HEART_RATE,
                    onClick = {
                        selectedMetricType = MetricType.HEART_RATE
                        selectedPointIndex = null
                    },
                    label = { Text("❤️ Nhịp tim", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = selectedMetricType == MetricType.BLOOD_PRESSURE,
                    onClick = {
                        selectedMetricType = MetricType.BLOOD_PRESSURE
                        selectedPointIndex = null
                    },
                    label = { Text("🩺 Huyết áp", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                )
                FilterChip(
                    selected = selectedMetricType == MetricType.BLOOD_SUGAR,
                    onClick = {
                        selectedMetricType = MetricType.BLOOD_SUGAR
                        selectedPointIndex = null
                    },
                    label = { Text("🩸 Đường huyết", fontSize = 12.sp, maxLines = 1, softWrap = false) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Tiêu đề + Nút lọc 7 ngày / 30 ngày
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = when (selectedMetricType) {
                            MetricType.HEART_RATE -> "Xu hướng Nhịp Tim"
                            MetricType.BLOOD_PRESSURE -> "Xu hướng Huyết Áp (Tâm thu / Tâm trương)"
                            MetricType.BLOOD_SUGAR -> "Xu hướng Đường Huyết"
                            else -> "Biểu đồ chỉ số"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (selectedMetricType) {
                            MetricType.HEART_RATE -> "Đơn vị: Nhịp / phút (BPM)"
                            MetricType.BLOOD_PRESSURE -> "Đơn vị: mmHg • Đỏ: Tâm thu | Xanh: Tâm trương"
                            MetricType.BLOOD_SUGAR -> "Đơn vị: mg/dL"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = selectedTimeRange == 7,
                        onClick = {
                            selectedTimeRange = 7
                            selectedPointIndex = null
                        },
                        label = { Text("7N", fontSize = 11.sp) }
                    )
                    FilterChip(
                        selected = selectedTimeRange == 30,
                        onClick = {
                            selectedTimeRange = 30
                            selectedPointIndex = null
                        },
                        label = { Text("30N", fontSize = 11.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Nếu chưa có dữ liệu, hiển thị thông báo hướng dẫn
            if (points.size < 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (points.isEmpty()) "Chưa có dữ liệu đo nào để vẽ biểu đồ.\nHãy thực hiện hoặc nhập lần đo đầu tiên!"
                        else "Đã có 1 lần đo.\nCần thêm ít nhất 1 lần đo nữa để vẽ đường xu hướng!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // 4. Biểu đồ đường Canvas chuyên nghiệp
                val isBp = selectedMetricType == MetricType.BLOOD_PRESSURE

                val allValues = if (isBp) {
                    points.map { it.value } + points.mapNotNull { it.valueSecondary }
                } else {
                    points.map { it.value }
                }

                val minVal = when (selectedMetricType) {
                    MetricType.HEART_RATE -> (allValues.minOrNull() ?: 60f - 10).coerceAtLeast(40f)
                    MetricType.BLOOD_PRESSURE -> (allValues.minOrNull() ?: 60f - 10).coerceAtLeast(40f)
                    MetricType.BLOOD_SUGAR -> (allValues.minOrNull() ?: 70f - 15).coerceAtLeast(40f)
                    else -> 0f
                }

                val maxVal = when (selectedMetricType) {
                    MetricType.HEART_RATE -> (allValues.maxOrNull() ?: 100f + 15).coerceAtLeast(120f)
                    MetricType.BLOOD_PRESSURE -> (allValues.maxOrNull() ?: 140f + 20).coerceAtLeast(160f)
                    MetricType.BLOOD_SUGAR -> (allValues.maxOrNull() ?: 140f + 25).coerceAtLeast(180f)
                    else -> 200f
                }

                val range = (maxVal - minVal).coerceAtLeast(10f)

                Box(modifier = Modifier.fillMaxWidth()) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181A20))
                            .pointerInput(points) {
                                detectTapGestures { offset ->
                                    val stepX = size.width / (points.size - 1)
                                    val tappedIndex = (offset.x / stepX).toInt()
                                        .coerceIn(0, points.size - 1)
                                    selectedPointIndex = tappedIndex
                                }
                            }
                    ) {
                        val width = size.width
                        val height = size.height
                        val paddingBottom = 30.dp.toPx()
                        val paddingTop = 25.dp.toPx()
                        val plotHeight = height - paddingBottom - paddingTop

                        // A. Dải an toàn y tế (Safe Zone)
                        when (selectedMetricType) {
                            MetricType.HEART_RATE -> {
                                val safeMinY = height - paddingBottom - ((60f - minVal) / range * plotHeight)
                                val safeMaxY = height - paddingBottom - ((100f - minVal) / range * plotHeight)
                                if (safeMinY in 0f..height && safeMaxY in 0f..height) {
                                    drawRect(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                        topLeft = Offset(0f, safeMaxY),
                                        size = androidx.compose.ui.geometry.Size(width, safeMinY - safeMaxY)
                                    )
                                }
                            }
                            MetricType.BLOOD_PRESSURE -> {
                                val safeMinY = height - paddingBottom - ((80f - minVal) / range * plotHeight)
                                val safeMaxY = height - paddingBottom - ((120f - minVal) / range * plotHeight)
                                if (safeMinY in 0f..height && safeMaxY in 0f..height) {
                                    drawRect(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                        topLeft = Offset(0f, safeMaxY),
                                        size = androidx.compose.ui.geometry.Size(width, safeMinY - safeMaxY)
                                    )
                                }
                            }
                            MetricType.BLOOD_SUGAR -> {
                                val safeMinY = height - paddingBottom - ((70f - minVal) / range * plotHeight)
                                val safeMaxY = height - paddingBottom - ((140f - minVal) / range * plotHeight)
                                if (safeMinY in 0f..height && safeMaxY in 0f..height) {
                                    drawRect(
                                        color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                        topLeft = Offset(0f, safeMaxY),
                                        size = androidx.compose.ui.geometry.Size(width, safeMinY - safeMaxY)
                                    )
                                }
                            }
                            else -> {}
                        }

                        // B. Đường lưới ngang
                        val gridLevels = when (selectedMetricType) {
                            MetricType.HEART_RATE -> listOf(60f, 80f, 100f, 120f)
                            MetricType.BLOOD_PRESSURE -> listOf(70f, 90f, 120f, 140f, 160f)
                            MetricType.BLOOD_SUGAR -> listOf(70f, 100f, 140f, 180f)
                            else -> listOf(50f, 100f, 150f)
                        }

                        gridLevels.forEach { level ->
                            if (level in minVal..maxVal) {
                                val y = height - paddingBottom - ((level - minVal) / range * plotHeight)
                                drawLine(
                                    color = Color(0xFF2E323D),
                                    start = Offset(0f, y),
                                    end = Offset(width, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                                drawContext.canvas.nativeCanvas.drawText(
                                    "${level.toInt()}",
                                    10f,
                                    y - 5f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.GRAY
                                        textSize = 24f
                                    }
                                )
                            }
                        }

                        // C. Vẽ đường đồ thị (Line Path)
                        val stepX = width / (points.size - 1)
                        val primaryColor = when (selectedMetricType) {
                            MetricType.HEART_RATE -> Color(0xFFE91E63)
                            MetricType.BLOOD_PRESSURE -> Color(0xFFE53935) // Đỏ Tâm thu
                            MetricType.BLOOD_SUGAR -> Color(0xFFFB8C00) // Vàng cam Đường huyết
                            else -> Color(0xFF2196F3)
                        }

                        val linePath = Path()
                        val fillPath = Path()

                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = height - paddingBottom - ((pt.value - minVal) / range * plotHeight)

                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, height - paddingBottom)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }

                        fillPath.lineTo(width, height - paddingBottom)
                        fillPath.close()

                        // Đổ bóng Gradient dưới đường chính
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.0f)
                                )
                            )
                        )

                        // Vẽ nét đường chính
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // NẾU LÀ HUYẾT ÁP: Vẽ thêm đường thứ hai cho Tâm Trương (Diastolic - Xanh lam)
                        if (isBp) {
                            val diaColor = Color(0xFF1E88E5)
                            val diaLinePath = Path()
                            points.forEachIndexed { i, pt ->
                                val dia = pt.valueSecondary ?: 80f
                                val x = i * stepX
                                val y = height - paddingBottom - ((dia - minVal) / range * plotHeight)
                                if (i == 0) diaLinePath.moveTo(x, y) else diaLinePath.lineTo(x, y)
                            }
                            drawPath(
                                path = diaLinePath,
                                color = diaColor,
                                style = Stroke(width = 2.5.dp.toPx())
                            )
                        }

                        // D. Vẽ các điểm tròn dữ liệu
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = height - paddingBottom - ((pt.value - minVal) / range * plotHeight)
                            val isSelected = selectedPointIndex == i

                            // Điểm đường chính
                            drawCircle(
                                color = if (isSelected) Color.White else primaryColor,
                                radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }

                            // Điểm đường tâm trương (nếu có)
                            if (isBp && pt.valueSecondary != null) {
                                val diaY = height - paddingBottom - ((pt.valueSecondary - minVal) / range * plotHeight)
                                val diaColor = Color(0xFF1E88E5)
                                drawCircle(
                                    color = if (isSelected) Color.White else diaColor,
                                    radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                                    center = Offset(x, diaY)
                                )
                            }

                            // Nhãn ngày tháng dưới trục X
                            if (i == 0 || i == points.size - 1 || i == points.size / 2) {
                                drawContext.canvas.nativeCanvas.drawText(
                                    pt.label,
                                    x - 20f,
                                    height - 5f,
                                    android.graphics.Paint().apply {
                                        color = android.graphics.Color.LTGRAY
                                        textSize = 24f
                                    }
                                )
                            }
                        }
                    }

                    // Bong bóng Tooltip khi chạm vào điểm
                    selectedPointIndex?.let { index ->
                        if (index in points.indices) {
                            val pt = points[index]
                            val tooltipText = when (selectedMetricType) {
                                MetricType.HEART_RATE -> "${pt.label}: ${pt.value.toInt()} BPM"
                                MetricType.BLOOD_PRESSURE -> "${pt.label}: ${pt.value.toInt()} / ${pt.valueSecondary?.toInt() ?: "--"} mmHg"
                                MetricType.BLOOD_SUGAR -> "${pt.label}: ${pt.value.toInt()} mg/dL"
                                else -> "${pt.label}: ${pt.value}"
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (selectedMetricType) {
                                    MetricType.HEART_RATE -> Color(0xFFE91E63)
                                    MetricType.BLOOD_PRESSURE -> Color(0xFFD32F2F)
                                    MetricType.BLOOD_SUGAR -> Color(0xFFE65100)
                                    else -> Color.DarkGray
                                },
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = tooltipText,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 5. Thống kê tóm tắt các chỉ số
                if (isBp) {
                    val avgSys = points.map { it.value }.average().toInt()
                    val avgDia = points.mapNotNull { it.valueSecondary }.let {
                        if (it.isNotEmpty()) it.average().toInt() else 80
                    }
                    val totalRecords = points.size

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCard(title = "TB Tâm thu", value = "$avgSys mmHg", color = Color(0xFFE53935), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(title = "TB Tâm trương", value = "$avgDia mmHg", color = Color(0xFF1E88E5), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(title = "Tổng số lần", value = "$totalRecords lần", color = Color(0xFF43A047), modifier = Modifier.weight(1f))
                    }
                } else {
                    val avgVal = points.map { it.value }.average().toInt()
                    val minRecorded = points.minOf { it.value }.toInt()
                    val maxRecorded = points.maxOf { it.value }.toInt()
                    val unit = if (selectedMetricType == MetricType.HEART_RATE) "BPM" else "mg/dL"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatCard(title = "Trung bình", value = "$avgVal $unit", color = Color(0xFF1976D2), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(title = "Thấp nhất", value = "$minRecorded $unit", color = Color(0xFF388E3C), modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        StatCard(title = "Cao nhất", value = "$maxRecorded $unit", color = Color(0xFFE65100), modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = color
            )
        }
    }
}
