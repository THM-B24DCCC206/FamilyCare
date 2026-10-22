package com.familycare.app.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.familycare.app.core.common.SessionManager
import com.familycare.app.presentation.ai_assistant.AiAssistantScreen
import com.familycare.app.presentation.auth.FamilySetupScreen
import com.familycare.app.presentation.auth.LoginScreen
import com.familycare.app.presentation.biometrics.BiometricsScreen
import com.familycare.app.presentation.biometrics.ppg.CameraPpgScreen
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
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Kiểm tra điểm khởi đầu dựa vào phiên đăng nhập
    val startDestination = remember {
        when {
            !sessionManager.isLoggedIn -> Screen.Login.route
            !sessionManager.hasFamily -> Screen.FamilySetup.route
            else -> Screen.Dashboard.route
        }
    }

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
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 1. Luồng Xác thực & Gia đình (Thành viên 1)
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { hasFamily ->
                        if (hasFamily) {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Screen.FamilySetup.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                )
            }

            composable(Screen.FamilySetup.route) {
                FamilySetupScreen(
                    onSetupComplete = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Các Tab Chính
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToPpg = { navController.navigate(Screen.CameraPpg.route) },
                    onNavigateToAi = { navController.navigate(Screen.AiAssistant.route) },
                    onNavigateToOcr = { navController.navigate(Screen.Medication.route) },
                    onNavigateToEmergency = { /* Navigate to SOS */ }
                )
            }

            composable(Screen.Biometrics.route) {
                BiometricsScreen(
                    onNavigateToCameraPpg = { navController.navigate(Screen.CameraPpg.route) }
                )
            }

            composable(Screen.CameraPpg.route) {
                CameraPpgScreen(
                    onBack = { navController.popBackStack() }
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
                    onNavigateToPdfReport = { /* Handled in feature_report */ },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
