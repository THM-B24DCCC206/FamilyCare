package com.familycare.app.presentation.biometrics.ppg

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun PpgWaveformView(
    waveformPoints: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E1E2C)) // Nền tối phong cách y tế chuyên nghiệp
    ) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        // Vẽ đường lưới tọa độ mờ (Grid lines)
        val gridColor = Color(0xFF2C2C3E)
        for (i in 1..4) {
            val y = height * (i / 5f)
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        if (waveformPoints.size < 2) {
            // Khi chưa có tín hiệu, vẽ một đường thẳng ở giữa
            drawLine(
                color = Color(0xFFE91E63).copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 3f
            )
            return@Canvas
        }

        // Tính min/max để chuẩn hóa độ cao sóng trong khung vẽ
        val minVal = waveformPoints.minOrNull() ?: 0f
        val maxVal = waveformPoints.maxOrNull() ?: 1f
        val range = (maxVal - minVal).coerceAtLeast(1f)

        val path = Path()
        val fillPath = Path()
        val stepX = width / (waveformPoints.size - 1)

        waveformPoints.forEachIndexed { index, value ->
            val x = index * stepX
            // Chuẩn hóa Y, chừa biên trên dưới 15dp
            val normalizedY = (value - minVal) / range
            val y = height - (normalizedY * (height - 30.dp.toPx()) + 15.dp.toPx())

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }

        // Hoàn thành đường fill gradient
        fillPath.lineTo(width, height)
        fillPath.close()

        // 1. Vẽ vùng đổ bóng Gradient dưới sóng
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE91E63).copy(alpha = 0.35f),
                    Color(0xFFE91E63).copy(alpha = 0.0f)
                )
            )
        )

        // 2. Vẽ đường sóng nhịp tim màu đỏ neon
        drawPath(
            path = path,
            color = Color(0xFFE91E63),
            style = Stroke(width = 4f)
        )
    }
}
