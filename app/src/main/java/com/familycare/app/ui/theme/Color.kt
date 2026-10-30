package com.familycare.app.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// DESIGN SYSTEM: WARM CREAM EDITORIAL WELLNESS
// Bảng màu chuẩn mực: Vàng kem ấm, xanh lá trầm, bề mặt trắng ngà
// =========================================================================

// 1. Nền & Bề mặt (Warm Cream & Ivory Surfaces)
val WarmCreamBackground = Color(0xFFF8F5EC)      // Background chính: vàng kem ấm
val WarmCreamSecondary = Color(0xFFF3EEDF)       // Background phụ: kem đậm nhẹ
val WarmCreamSurface = Color(0xFFFFFEFA)         // Surface trắng ấm tinh tế
val WarmDivider = Color(0xFFE3DED2)              // Đường kẻ phân cách be xám nhạt

// 2. Màu Thương hiệu (Deep Sage & Forest Teal)
val WarmPrimaryTeal = Color(0xFF176B55)          // Primary: xanh lá trầm thương hiệu
val WarmPrimaryHover = Color(0xFF125644)
val WarmPrimaryLight = Color(0xFFE5EEE2)         // Primary light: xanh nhạt sage
val WarmPrimaryDark = Color(0xFF0E4A3B)

// 3. Màu chữ (Warm Botanical Charcoal & Slate)
val WarmTextPrimary = Color(0xFF253B32)          // Text chính: rêu than đậm sắc nét
val WarmTextSecondary = Color(0xFF68766B)        // Text phụ: rêu xám êm dịu
val WarmTextMuted = Color(0xFF9EA9A1)            // Text mờ: đơn vị đo, timestamp

// 4. Màu Nhấn Chỉ số Y tế (Metric Accents - Tự nhiên, không chói)
val WarmAccent = Color(0xFFD9A56C)               // Accent ấm (Huyết áp)
val HeartRateAccent = Color(0xFFB85D62)          // Accent nhịp tim (Đỏ hồng dịu)
val TemperatureAccent = Color(0xFFC18443)        // Accent nhiệt độ (Cam đất)
val BloodGlucoseAccent = Color(0xFF6485B6)       // Accent đường huyết (Xanh biển dịu)

// 5. Cấp cứu khẩn cấp SOS & Trạng thái Semantic
val SosRed = Color(0xFFC53030)                   // Đỏ khẩn cấp
val SosRedLight = Color(0xFFFDEEEF)              // Nền SOS nhạt
val SosRedBorder = Color(0xFFFAD2D4)             // Viền SOS

val HealthSuccess = Color(0xFF2F855A)
val HealthSuccessLight = Color(0xFFF0FFF4)
val HealthSuccessBorder = Color(0xFFC6F6D5)

val HealthWarning = Color(0xFFC05621)
val HealthWarningLight = Color(0xFFFFFAF0)
val HealthWarningBorder = Color(0xFFFEEBC8)

val HealthError = SosRed
val HealthErrorLight = SosRedLight
val HealthErrorBorder = SosRedBorder

val HealthInfo = Color(0xFF2B6CB0)
val HealthInfoLight = Color(0xFFEBF8FF)
val HealthInfoBorder = Color(0xFFBEE3F8)

// =========================================================================
// MAPPING & BACKWARD-COMPATIBLE ALIASES
// =========================================================================
val HealthTealPrimary = WarmPrimaryTeal
val HealthTealHover = WarmPrimaryHover
val HealthTealLight = WarmPrimaryLight
val HealthTealDark = WarmPrimaryDark

val HealthBackground = WarmCreamBackground
val HealthSurface = WarmCreamSurface
val HealthSurfaceWarm = WarmCreamSecondary
val HealthBorder = WarmDivider
val HealthBorderStrong = Color(0xFFD2CBC0)

val HealthTextPrimary = WarmTextPrimary
val HealthTextSecondary = WarmTextSecondary
val HealthTextMuted = WarmTextMuted

val FamilyBluePrimary = HealthTealPrimary
val FamilyBlueDark = HealthTealDark
val FamilyBlueOnPrimary = Color(0xFFFFFFFF)
val FamilyBlueContainer = HealthTealLight
val FamilyBlueLight = HealthTealLight
val FamilyBlueOnContainer = HealthTealDark

val FamilySageSecondary = HealthSuccess
val FamilySageOnSecondary = Color(0xFFFFFFFF)
val FamilySageContainer = HealthSuccessLight
val FamilySageOnContainer = HealthSuccess

val FamilyBackgroundLight = HealthBackground
val FamilySurfaceLight = HealthSurface
val FamilySurfaceVariant = HealthSurfaceWarm
val FamilyOutline = HealthBorder
val FamilyOutlineVariant = HealthBorderStrong

val FamilyTextPrimary = HealthTextPrimary
val FamilyTextSecondary = HealthTextSecondary
val FamilyTextMuted = HealthTextMuted

val HealthStatusPositive = HealthSuccess
val HealthStatusPositiveBg = HealthSuccessLight
val HealthStatusPositiveBorder = HealthSuccessBorder

val HealthStatusWarning = HealthWarning
val HealthStatusWarningBg = HealthWarningLight
val HealthStatusWarningBorder = HealthWarningBorder

val HealthStatusUrgent = HealthError
val HealthStatusUrgentBg = HealthErrorLight
val HealthStatusUrgentBorder = HealthErrorBorder

val MedicalBluePrimary = FamilyBluePrimary
val MedicalBlueOnPrimary = FamilyBlueOnPrimary
val MedicalBlueContainer = FamilyBlueContainer
val MedicalBlueOnContainer = FamilyBlueOnContainer

val MedicalMintSecondary = FamilySageSecondary
val MedicalMintOnSecondary = FamilySageOnSecondary
val MedicalMintContainer = FamilySageContainer
val MedicalMintOnContainer = FamilySageOnContainer

val AlertRed = HealthStatusUrgent
val AlertRedOnError = Color.White
val AlertRedContainer = HealthStatusUrgentBg
val AlertRedOnContainer = Color(0xFF7F1D1D)

val MedicalSurfaceLight = FamilyBackgroundLight
val MedicalOnSurface = FamilyTextPrimary
val MedicalSurfaceVariant = FamilySurfaceVariant
val MedicalOnSurfaceVariant = FamilyTextSecondary

// Dark Mode Colors
val MedicalBlueDark = Color(0xFF4DB6AC)
val MedicalMintDark = Color(0xFF81C784)
val AlertRedDark = Color(0xFFE57373)
val MedicalBackgroundDark = Color(0xFF141A22)
val MedicalSurfaceDark = Color(0xFF1C2430)
