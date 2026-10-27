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
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Nút bấm SOS tròn lớn 2 trạng thái hoạt động:
 * - Trạng thái 1 (Chưa phát SOS): Giữ 2 giây để kích hoạt còi hú.
 * - Trạng thái 2 (Đang phát còi hú liên tục): Giữ 2 giây để hủy báo động & tắt còi hú (Chống thao tác bấm nhầm).
 */
@Composable
fun SosButton(
    onSosTriggered: () -> Unit,
    onSosDismissed: () -> Unit = {},
    modifier: Modifier = Modifier,
    isActivated: Boolean = false,
    holdDurationMs: Long = 2000L
) {
    val coroutineScope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var holdJob by remember { mutableStateOf<Job?>(null) }
    var isHolding by remember { mutableStateOf(false) }

    // Hiệu ứng sóng lan tỏa nhấp nháy liên tục khi đang phát còi
    val infiniteTransition = rememberInfiniteTransition(label = "sosPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
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
                    if (isActivated) Color(0xFFDC2626).copy(alpha = haloAlpha)
                    else Color(0x1AE53935)
                )
        )

        // Vòng tiến trình 2 giây (Circular Progress Arc)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 10.dp.toPx()

            // Vòng nền mờ
            drawCircle(
                color = if (isActivated) Color(0x44FFFFFF) else Color(0x22E53935),
                style = Stroke(width = strokeWidth)
            )

            // Vòng tiến trình khi nhấn giữ
            if (progress.value > 0f) {
                drawArc(
                    color = if (isActivated) Color.White else Color(0xFFD32F2F),
                    startAngle = -90f,
                    sweepAngle = progress.value * 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Nút tròn trung tâm tiếp nhận cử chỉ nhấn giữ 2 giây
        Box(
            modifier = Modifier
                .size(190.dp)
                .shadow(
                    elevation = if (isHolding) 6.dp else 16.dp,
                    shape = CircleShape,
                    spotColor = if (isActivated) Color(0xFFB91C1C) else Color(0xFFE53935)
                )
                .clip(CircleShape)
                .background(
                    if (isActivated) {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFEF4444), Color(0xFF991B1B))
                        )
                    } else {
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFE53935), Color(0xFFB71C1C))
                        )
                    }
                )
                .pointerInput(isActivated) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            holdJob = coroutineScope.launch {
                                // Bắt đầu chạy tiến trình từ 0f lên 1f trong đúng 2000ms
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
                                    if (isActivated) {
                                        // Đang phát còi -> Giữ 2 giây để TẮT
                                        onSosDismissed()
                                    } else {
                                        // Chưa phát -> Giữ 2 giây để BẬT
                                        onSosTriggered()
                                    }
                                    progress.snapTo(0f)
                                }
                            }

                            // Chờ người dùng thả ngón tay
                            tryAwaitRelease()
                            isHolding = false
                            holdJob?.cancel()

                            // Nếu nhấc tay ra trước 2 giây: Tự động thu hồi tiến trình về 0 trong 150ms
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
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = if (isActivated) Icons.Default.VolumeOff else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "SOS",
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        isActivated && isHolding -> "GIỮ YÊN ĐỂ TẮT..."
                        isActivated -> "ĐANG PHÁT CÒI\nGIỮ 2 GIÂY ĐỂ TẮT"
                        isHolding -> "GIỮ YÊN..."
                        else -> "NHẤN GIỮ 2 GIÂY"
                    },
                    color = Color.White.copy(alpha = 0.95f),
                    fontSize = if (isActivated) 10.sp else 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 13.sp
                )
            }
        }
    }
}
