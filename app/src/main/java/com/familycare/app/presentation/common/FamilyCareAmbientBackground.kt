package com.familycare.app.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.familycare.app.ui.theme.WarmCreamBackground
import com.familycare.app.ui.theme.WarmCreamSecondary
import com.familycare.app.ui.theme.WarmPrimaryLight

/**
 * Nền Warm Cream Editorial Wellness dùng chung cho toàn bộ FamilyCare.
 * Nền chính: WarmCreamBackground (#F8F5EC) ấm áp như giấy ngà ivory,
 * phối hợp quầng sáng mờ WarmCreamSecondary (#F3EEDF) và WarmPrimaryLight (#E5EEE2).
 */
@Composable
fun FamilyCareAmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCreamBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Quầng sáng ấm góc trên trái
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WarmPrimaryLight.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.15f, height * 0.05f),
                    radius = width * 0.75f
                ),
                radius = width * 0.75f,
                center = Offset(width * 0.15f, height * 0.05f)
            )

            // Quầng kem ấm góc trên phải
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        WarmCreamSecondary.copy(alpha = 0.5f),
                        Color.Transparent
                    ),
                    center = Offset(width * 0.85f, height * 0.1f),
                    radius = width * 0.7f
                ),
                radius = width * 0.7f,
                center = Offset(width * 0.85f, height * 0.1f)
            )
        }

        content()
    }
}
