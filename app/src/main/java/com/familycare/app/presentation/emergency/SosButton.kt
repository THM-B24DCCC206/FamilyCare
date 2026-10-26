package com.familycare.app.presentation.emergency

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Nút bấm SOS 1-chạm kích hoạt khi nhấn giữ liên tục trong 2 giây
 * (Tránh hoàn toàn tình trạng vô tình chạm nhầm của người cao tuổi)
 */
@Composable
fun SosButton(
    onSosTriggered: () -> Unit,
    modifier: Modifier = Modifier,
    isActivated: Boolean = false,
    holdDurationMs: Long = 2000L
) {
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var isHolding by remember { mutableStateOf(false) }

    // Hiệu ứng sóng lan tỏa (Ripple Pulse) cảnh báo
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(if (isActivated) pulseScale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Vòng tròn hào quang nền bên ngoài
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(if (isHolding || isActivated) 1.08f else 1.0f)
                .clip(CircleShape)
                .background(
                    if (isActivated) Color(0x33E53935) else Color(0x1AE53935)
                )
        )

        // Vòng tiến trình 2 giây (Circular Progress Arc)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()
            // Vòng nền mờ
            drawCircle(
                color = Color(0x22E53935),
                style = Stroke(width = strokeWidth)
            )

            // Vòng tiến trình màu đỏ tươi khi nhấn giữ
            if (progress.value > 0f) {
                drawArc(
                    color = Color(0xFFD32F2F),
                    startAngle = -90f,
                    sweepAngle = progress.value * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Nút bấm tròn trung tâm nhận cử chỉ nhấn giữ
        Box(
            modifier = Modifier
                .size(190.dp)
                .shadow(
                    elevation = if (isHolding) 4.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = Color(0xFFE53935)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            if (isActivated) Color(0xFFFF5252) else Color(0xFFE53935),
                            Color(0xFFB71C1C)
                        )
                    )
                )
                .pointerInput(isActivated) {
                    if (isActivated) return@pointerInput // Đã kích hoạt rồi thì không nhận thêm

                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            holdJob = coroutineScope.launch {
                                // Bắt đầu chạy tiến trình từ 0f lên 1f trong 2000ms
                                progress.snapTo(0f)
                                progress.animateTo(
                                    targetValue = 1f,
                                    animationSpec = tween(
                                        durationMillis = holdDurationMs.toInt(),
                                        easing = LinearEasing
                                    )
                                )
                                // Nếu giữ đủ 2 giây trọn vẹn:
                                if (progress.value >= 1f) {
                                    onSosTriggered()
                                }
                            }

                            // Chờ người dùng thả ngón tay hoặc hủy
                            val released = tryAwaitRelease()
                            isHolding = false
                            holdJob?.cancel()

                            // Nếu nhả tay ra trước 2 giây: Huỷ ngay và đưa tiến trình về 0
                            if (progress.value < 1f) {
                                coroutineScope.launch {
                                    progress.animateTo(0f, tween(150))
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "SOS",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Text(
                    text = if (isActivated) "ĐÃ PHÁT CÒI" else if (isHolding) "GIỮ YÊN..." else "NHẤN GIỮ 2 GIÂY",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
