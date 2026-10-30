package com.familycare.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Dark Theme Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = MedicalBlueDark,
    onPrimary = MedicalBlueOnContainer,
    primaryContainer = FamilyBluePrimary,
    onPrimaryContainer = FamilyBlueOnPrimary,
    secondary = MedicalMintDark,
    onSecondary = MedicalMintOnContainer,
    secondaryContainer = FamilySageSecondary,
    onSecondaryContainer = FamilySageOnSecondary,
    error = AlertRedDark,
    onError = AlertRedOnContainer,
    errorContainer = AlertRed,
    onErrorContainer = AlertRedOnError,
    background = MedicalBackgroundDark,
    surface = MedicalSurfaceDark,
    surfaceVariant = Color(0xFF26303E),
    outline = Color(0xFF3B485A),
    onBackground = FamilySurfaceLight,
    onSurface = FamilySurfaceLight,
    onSurfaceVariant = Color(0xFFB0B9C6)
)

// Light Theme Color Scheme (Warm, Trustworthy, Accessible Material 3 Palette)
private val LightColorScheme = lightColorScheme(
    primary = FamilyBluePrimary,
    onPrimary = FamilyBlueOnPrimary,
    primaryContainer = FamilyBlueContainer,
    onPrimaryContainer = FamilyBlueOnContainer,
    secondary = FamilySageSecondary,
    onSecondary = FamilySageOnSecondary,
    secondaryContainer = FamilySageContainer,
    onSecondaryContainer = FamilySageOnContainer,
    error = HealthStatusUrgent,
    onError = AlertRedOnError,
    errorContainer = HealthStatusUrgentBg,
    onErrorContainer = AlertRedOnContainer,
    background = FamilyBackgroundLight,
    surface = FamilySurfaceLight,
    surfaceVariant = FamilySurfaceVariant,
    outline = FamilyOutline,
    outlineVariant = FamilyOutlineVariant,
    onBackground = FamilyTextPrimary,
    onSurface = FamilyTextPrimary,
    onSurfaceVariant = FamilyTextSecondary
)

@Composable
fun FamilyCareTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
