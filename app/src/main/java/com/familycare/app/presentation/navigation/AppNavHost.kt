package com.familycare.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.familycare.app.presentation.ai_assistant.AiAssistantScreen
import com.familycare.app.presentation.biometrics.BiometricsScreen
import com.familycare.app.presentation.dashboard.DashboardScreen
import com.familycare.app.presentation.medication.MedicationScreen
import com.familycare.app.presentation.profile.ProfileScreen

data class BottomNavItem(
    val title: String,
    val route: String,
    val icon: ImageVector
)

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("Trang chủ", Screen.Dashboard.route, Icons.Default.Home),
        BottomNavItem("Chỉ số", Screen.Biometrics.route, Icons.Default.Favorite),
        BottomNavItem("Thuốc", Screen.Medication.route, Icons.Default.Medication),
        BottomNavItem("Trợ lý AI", Screen.AiAssistant.route, Icons.Default.AutoAwesome),
        BottomNavItem("Hồ sơ", Screen.Profile.route, Icons.Default.Person)
    )

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute == item.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToPpg = { navController.navigate(Screen.Biometrics.route) },
                    onNavigateToAi = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToOcr = { navController.navigate(Screen.Medication.route) },
                    onNavigateToEmergency = { /* Navigate to SOS */ }
                )
            }
            composable(Screen.Biometrics.route) {
                BiometricsScreen(
                    onNavigateToCameraPpg = { /* Handled in feature_biometrics */ }
                )
            }
            composable(Screen.Medication.route) {
                MedicationScreen(
                    onNavigateToOcr = { /* Handled in feature_medication */ }
                )
            }
            composable(Screen.AiAssistant.route) {
                AiAssistantScreen()
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToPdfReport = { /* Handled in feature_report */ }
                )
            }
        }
    }
}
