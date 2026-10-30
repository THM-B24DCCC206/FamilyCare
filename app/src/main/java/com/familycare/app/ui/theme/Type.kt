package com.familycare.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// =========================================================================
// DESIGN SYSTEM TYPOGRAPHY (DESIGN.md)
// Uses system SansSerif with generous lineHeights (1.3 - 1.7) for low
// cognitive load, high contrast and excellent readability for seniors.
// =========================================================================

val Typography = Typography(
    // H1 equivalent: 26sp, Bold 700, lineHeight 33sp
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.3).sp
    ),
    // H2 equivalent: 21sp, SemiBold 600, lineHeight 28sp
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 21.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    // H3 equivalent: 18sp, SemiBold 600, lineHeight 24sp
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    // Body Large: 17sp, Normal 400, generous lineHeight 27sp (1.6x)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.2.sp
    ),
    // Body Medium: 15.5sp, Normal 400, lineHeight 24sp
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.5.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.2.sp
    ),
    // Body Small / Secondary: 13.5sp, Normal 400, lineHeight 20sp
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp
    ),
    // Button labels / Primary actions: 15.5sp, SemiBold 600
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.5.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    // Secondary action / Filter chip: 13.5sp, Medium 500
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    // Captions / Badges: 12sp, Medium 500
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
)
