package com.familycare.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Dark Theme Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueDark,
    onPrimary = MedicalBlueOnContainer,
    primaryContainer = MedicalBluePrimary,
    onPrimaryContainer = MedicalBlueOnPrimary,
    secondary = MedicalMintDark,
    onSecondary = MedicalMintOnContainer,
    secondaryContainer = MedicalMintSecondary,
    onSecondaryContainer = MedicalMintOnSecondary,
    error = AlertRedDark,
    onError = AlertRedOnContainer,
    errorContainer = AlertRed,
    onErrorContainer = AlertRedOnError,
    background = MedicalBackgroundDark,
    surface = MedicalSurfaceDark,
    onBackground = MedicalSurfaceLight,
    onSurface = MedicalSurfaceLight
)

// Light Theme Color Scheme (Medical Blue & Mint Palette - Material 3)
private val LightColorScheme = lightColorScheme(
    primary = MedicalBluePrimary,
    onPrimary = MedicalBlueOnPrimary,
    primaryContainer = MedicalBlueContainer,
    onPrimaryContainer = MedicalBlueOnContainer,
    secondary = MedicalMintSecondary,
    onSecondary = MedicalMintOnSecondary,
    secondaryContainer = MedicalMintContainer,
    onSecondaryContainer = MedicalMintOnContainer,
    error = AlertRed,
    onError = AlertRedOnError,
    errorContainer = AlertRedContainer,
    onErrorContainer = AlertRedOnContainer,
    background = MedicalSurfaceLight,
    surface = MedicalSurfaceLight,
    surfaceVariant = MedicalSurfaceVariant,
    onBackground = MedicalOnSurface,
    onSurface = MedicalOnSurface,
    onSurfaceVariant = MedicalOnSurfaceVariant
)

@Composable
fun FamilyCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Đặt false mặc định để áp dụng chuẩn bảng màu Y tế đồng bộ thay vì đổi theo hình nền Android 12
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
