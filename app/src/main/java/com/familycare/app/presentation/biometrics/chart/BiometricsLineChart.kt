package com.familycare.app.presentation.biometrics.chart

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

data class ChartPoint(
    val timestamp: Long,
    val value: Float,
    val label: String
)

@Composable
fun BiometricsLineChart(
    metrics: List<HealthMetricEntity>,
    modifier: Modifier = Modifier
) {
    var selectedTimeRange by remember { mutableIntStateOf(7) } // 7 ngày hoặc 30 ngày
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val dateFormat = remember { SimpleDateFormat("dd/MM", Locale.getDefault()) }

    // Lọc dữ liệu theo thời gian
    val cutoffTime = remember(selectedTimeRange) {
        System.currentTimeMillis() - (selectedTimeRange * 24 * 60 * 60 * 1000L)
    }

    val filteredMetrics = remember(metrics, cutoffTime) {
        metrics.filter { it.timestamp >= cutoffTime && it.type == "HEART_RATE" }
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
                    label = dateFormat.format(Date(it.timestamp))
                )
            }
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // 1. Tiêu đề + Nút lọc 7 ngày / 30 ngày
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Xu hướng Nhịp Tim",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Đơn vị: Nhịp / phút (BPM)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedTimeRange == 7,
                        onClick = {
                            selectedTimeRange = 7
                            selectedPointIndex = null
                        },
                        label = { Text("7 Ngày", fontSize = 12.sp) }
                    )
                    FilterChip(
                        selected = selectedTimeRange == 30,
                        onClick = {
                            selectedTimeRange = 30
                            selectedPointIndex = null
                        },
                        label = { Text("30 Ngày", fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Nếu chưa có dữ liệu, hiển thị thông báo hướng dẫn
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
                        text = if (points.isEmpty()) "Chưa có dữ liệu đo nào để vẽ biểu đồ.\nHãy thực hiện lần đo đầu tiên!"
                        else "Đã có 1 lần đo (${points.first().value.toInt()} BPM).\nCần thêm ít nhất 1 lần đo nữa để vẽ đường xu hướng!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // 3. Biểu đồ đường Canvas chuyên nghiệp
                val minVal = (points.minOf { it.value } - 10).coerceAtLeast(40f)
                val maxVal = (points.maxOf { it.value } + 15).coerceAtLeast(120f)
                val range = maxVal - minVal

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

                        // A. Vẽ dải vùng nhịp tim an toàn y tế (Safe Zone 60 - 100 BPM)
                        val safeMinY = height - paddingBottom - ((60f - minVal) / range * plotHeight)
                        val safeMaxY = height - paddingBottom - ((100f - minVal) / range * plotHeight)

                        if (safeMinY in 0f..height && safeMaxY in 0f..height) {
                            drawRect(
                                color = Color(0xFF2E7D32).copy(alpha = 0.12f),
                                topLeft = Offset(0f, safeMaxY),
                                size = androidx.compose.ui.geometry.Size(width, safeMinY - safeMaxY)
                            )
                        }

                        // B. Vẽ các đường lưới ngang mờ
                        val gridLevels = listOf(60f, 80f, 100f, 120f)
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

                        // C. Vẽ đường đồ thị (Line Path) và Gradient đổ bóng
                        val stepX = width / (points.size - 1)
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

                        // Đổ bóng Gradient dưới đường
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFE91E63).copy(alpha = 0.35f),
                                    Color(0xFFE91E63).copy(alpha = 0.0f)
                                )
                            )
                        )

                        // Vẽ đường nét đỏ
                        drawPath(
                            path = linePath,
                            color = Color(0xFFE91E63),
                            style = Stroke(width = 3.dp.toPx())
                        )

                        // D. Vẽ các điểm tròn dữ liệu
                        points.forEachIndexed { i, pt ->
                            val x = i * stepX
                            val y = height - paddingBottom - ((pt.value - minVal) / range * plotHeight)
                            val isSelected = selectedPointIndex == i

                            // Điểm được chọn vẽ to hơn
                            drawCircle(
                                color = if (isSelected) Color.White else Color(0xFFE91E63),
                                radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                            if (isSelected) {
                                drawCircle(
                                    color = Color(0xFFE91E63),
                                    radius = 4.dp.toPx(),
                                    center = Offset(x, y)
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

                    // Bong bóng hiển thị thông tin điểm đang chạm (Tooltip)
                    selectedPointIndex?.let { index ->
                        if (index in points.indices) {
                            val pt = points[index]
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFE91E63),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = "${pt.label}: ${pt.value.toInt()} BPM",
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

                // 4. Thống kê tóm tắt 3 chỉ số (Trung bình, Thấp nhất, Cao nhất)
                val avgVal = points.map { it.value }.average().toInt()
                val minRecorded = points.minOf { it.value }.toInt()
                val maxRecorded = points.maxOf { it.value }.toInt()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StatCard(title = "Trung bình", value = "$avgVal BPM", color = Color(0xFF1976D2))
                    StatCard(title = "Thấp nhất", value = "$minRecorded BPM", color = Color(0xFF388E3C))
                    StatCard(title = "Cao nhất", value = "$maxRecorded BPM", color = Color(0xFFE65100))
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
        modifier = modifier.width(100.dp)
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
                fontSize = 14.sp,
                color = color
            )
        }
    }
}
