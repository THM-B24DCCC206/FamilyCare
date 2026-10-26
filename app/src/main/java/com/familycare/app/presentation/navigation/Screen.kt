package com.familycare.app.presentation.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login : Screen("login")
    object Register : Screen("register")
    object FamilySetup : Screen("family_setup")

    // Main Tabs
    object Dashboard : Screen("dashboard")
    object Biometrics : Screen("biometrics")
    object Medication : Screen("medication")
    object AiAssistant : Screen("ai_assistant")
    object Profile : Screen("profile")

    // Sub-screens & Advanced Features
    object CameraPpg : Screen("camera_ppg")             // [Thành viên 1] Đo nhịp tim Camera
    object OcrPrescription : Screen("ocr_scan")         // [Thành viên 3] Quét đơn thuốc ML Kit
    object Appointments : Screen("appointments")         // [Thành viên 3] Lịch khám
    object PdfReport : Screen("pdf_report")             // [Thành viên 3] Xuất báo cáo PDF
    object EmergencyAlerts : Screen("emergency_alerts") // [Thành viên 2] Danh sách cảnh báo
    object SosPanic : Screen("sos_panic")               // [Thành viên 2] Màn hình nút SOS & Còi hú
    object Smartwatch : Screen("smartwatch")           // Kết nối đồng hồ thông minh BLE
}
