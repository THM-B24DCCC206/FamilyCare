package com.familycare.app.presentation.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.familycare.app.core.common.SessionManager
import com.familycare.app.presentation.ai_assistant.AiAssistantScreen
import com.familycare.app.presentation.alert.AlertListScreen
import com.familycare.app.presentation.appointment.AppointmentScreen
import com.familycare.app.presentation.auth.FamilySetupScreen
import com.familycare.app.presentation.auth.LoginScreen
import com.familycare.app.presentation.biometrics.BiometricsScreen
import com.familycare.app.presentation.biometrics.ppg.CameraPpgScreen
import com.familycare.app.presentation.emergency.EmergencyScreen
import com.familycare.app.presentation.home.HomeScreen
import com.familycare.app.presentation.medication.MedicationScreen
import com.familycare.app.presentation.ocr.OcrPrescriptionScreen
import com.familycare.app.presentation.profile.PreviewMedicalReportScreen
import com.familycare.app.presentation.profile.ProfileScreen
import com.familycare.app.presentation.schedule.HealthScheduleScreen

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

    val mainTabRoutes = listOf(
        Screen.Dashboard.route,
        Screen.Biometrics.route,
        Screen.AiAssistant.route,
        Screen.Medication.route,
        Screen.Profile.route
    )

    val showBottomBar = currentRoute in mainTabRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                FamilyCareBottomBar(
                    currentRoute = currentRoute,
                    onNavigateToTab = { targetRoute ->
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
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
                    },
                    onBackToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // 2. Tab 1: Trang chủ (HomeScreen chuẩn MVVM, State-driven UI)
            composable(Screen.Dashboard.route) {
                HomeScreen(
                    onNavigateToVitals = { navController.navigate(Screen.Biometrics.route) },
                    onNavigateToMedication = { navController.navigate(Screen.Medication.route) },
                    onNavigateToSos = { navController.navigate(Screen.SosPanic.route) },
                    onNavigateToSmartwatch = { navController.navigate(Screen.Smartwatch.route) },
                    onNavigateToNotifications = { navController.navigate(Screen.EmergencyAlerts.route) }
                )
            }

            // Tab 2: Chỉ số sinh tồn
            composable(Screen.Biometrics.route) {
                BiometricsScreen(
                    onNavigateToCameraPpg = { navController.navigate(Screen.CameraPpg.route) },
                    onNavigateToSmartwatch = { navController.navigate(Screen.Smartwatch.route) }
                )
            }

            // Tab 3 (Trung tâm): Trợ lý AI Copilot
            composable(Screen.AiAssistant.route) {
                AiAssistantScreen(
                    onNavigateBack = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToVitals = {
                        navController.navigate(Screen.Biometrics.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSchedule = {
                        navController.navigate(Screen.Medication.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            // Tab 4: Kế hoạch Điều trị & Lịch khám (HealthScheduleScreen)
            composable(Screen.Medication.route) {
                HealthScheduleScreen(
                    onNavigateToOcr = { memberId ->
                        navController.navigate("${Screen.OcrPrescription.route}?memberId=$memberId")
                    }
                )
            }

            // Tab 5: Hồ sơ
            composable(Screen.Profile.route) {
                ProfileScreen(
                    onNavigateToPdfReport = { memberId ->
                        navController.navigate("${Screen.PdfReport.route}?memberId=$memberId")
                    },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Màn hình Xem Trước Báo Cáo Y Tế (Ẩn BottomBar)
            composable(
                route = "${Screen.PdfReport.route}?memberId={memberId}",
                arguments = listOf(
                    navArgument("memberId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
                PreviewMedicalReportScreen(
                    memberId = memberId,
                    onBack = { navController.popBackStack() }
                )
            }

            // Các màn hình con & phụ trợ
            composable(Screen.CameraPpg.route) {
                CameraPpgScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "${Screen.OcrPrescription.route}?memberId={memberId}",
                arguments = listOf(
                    navArgument("memberId") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val memberId = backStackEntry.arguments?.getString("memberId").orEmpty()
                OcrPrescriptionScreen(
                    memberId = memberId,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Appointments.route) {
                AppointmentScreen()
            }

            composable(Screen.EmergencyAlerts.route) {
                AlertListScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToSos = { navController.navigate(Screen.SosPanic.route) }
                )
            }

            composable(Screen.SosPanic.route) {
                EmergencyScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Smartwatch.route) {
                com.familycare.app.presentation.smartwatch.SmartwatchScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Thanh điều hướng đáy 5 tab với nút Trợ lý AI (Sparkles/AutoAwesome) nhô cao ở trung tâm
 */
@Composable
fun FamilyCareBottomBar(
    currentRoute: String?,
    onNavigateToTab: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Nền thanh điều hướng
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Trang chủ
                BottomTabItem(
                    title = "Trang chủ",
                    icon = Icons.Default.Home,
                    isSelected = currentRoute == Screen.Dashboard.route,
                    onClick = { onNavigateToTab(Screen.Dashboard.route) },
                    modifier = Modifier.weight(1f)
                )

                // 2. Chỉ số
                BottomTabItem(
                    title = "Chỉ số",
                    icon = Icons.Default.Favorite,
                    isSelected = currentRoute == Screen.Biometrics.route,
                    onClick = { onNavigateToTab(Screen.Biometrics.route) },
                    modifier = Modifier.weight(1f)
                )

                // 3. Vị trí trung tâm (dành chỗ cho nút AI nhô cao)
                Spacer(modifier = Modifier.weight(1f))

                // 4. Kế hoạch
                BottomTabItem(
                    title = "Kế hoạch",
                    icon = Icons.Default.EventNote,
                    isSelected = currentRoute == Screen.Medication.route,
                    onClick = { onNavigateToTab(Screen.Medication.route) },
                    modifier = Modifier.weight(1f)
                )

                // 5. Hồ sơ
                BottomTabItem(
                    title = "Hồ sơ",
                    icon = Icons.Default.Person,
                    isSelected = currentRoute == Screen.Profile.route,
                    onClick = { onNavigateToTab(Screen.Profile.route) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Nút Trợ lý AI nhô cao ở chính giữa (Màu xanh #2563EB, icon lấp lánh AutoAwesome)
        val isAiSelected = currentRoute == Screen.AiAssistant.route
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
                .clickable { onNavigateToTab(Screen.AiAssistant.route) }
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF2563EB),
                shadowElevation = 8.dp,
                border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
                modifier = Modifier.size(54.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Trợ lý AI Copilot",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Trợ lý AI",
                fontSize = 11.sp,
                fontWeight = if (isAiSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isAiSelected) Color(0xFF2563EB) else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BottomTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )
    }
}
