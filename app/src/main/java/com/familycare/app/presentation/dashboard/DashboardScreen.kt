package com.familycare.app.presentation.dashboard

import androidx.compose.runtime.Composable
import com.familycare.app.presentation.home.HomeScreen

@Composable
fun DashboardScreen(
    onNavigateToPpg: () -> Unit,
    onNavigateToAi: () -> Unit = {},
    onNavigateToOcr: () -> Unit = {},
    onNavigateToEmergency: () -> Unit = {},
    onNavigateToSos: () -> Unit = onNavigateToEmergency,
    onNavigateToSmartwatch: () -> Unit = {}
) {
    HomeScreen(
        onNavigateToVitals = onNavigateToPpg,
        onNavigateToMedication = onNavigateToOcr,
        onNavigateToSos = onNavigateToSos,
        onNavigateToSmartwatch = onNavigateToSmartwatch,
        onNavigateToPpg = onNavigateToPpg
    )
}
