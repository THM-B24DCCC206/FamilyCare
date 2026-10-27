package com.familycare.app.presentation.emergency

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.core.emergency.EmergencySirenController
import com.familycare.app.ui.theme.FamilyCareTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Activity báo động khẩn cấp hiển thị đè lên màn hình khóa trên thiết bị Người thân (Máy B).
 */
class EmergencyAlertReceiverActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SENDER_NAME = "senderName"
        const val EXTRA_SENDER_PHONE = "senderPhone"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_BLOOD_TYPE = "bloodType"
        const val EXTRA_CONDITIONS = "medicalConditions"
        const val EXTRA_ALLERGIES = "allergies"
        const val EXTRA_TIMESTAMP = "timestamp"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cấu hình cửa sổ hiển thị đè lên màn hình khóa (Bypass Lock Screen & Turn Screen On)
        setupLockscreenFlags()

        val senderName = intent.getStringExtra(EXTRA_SENDER_NAME) ?: "Người thân"
        val senderPhone = intent.getStringExtra(EXTRA_SENDER_PHONE) ?: ""
        val latitude = intent.getDoubleExtra(EXTRA_LATITUDE, 0.0)
        val longitude = intent.getDoubleExtra(EXTRA_LONGITUDE, 0.0)
        val address = intent.getStringExtra(EXTRA_ADDRESS) ?: "Vị trí không xác định"
        val bloodType = intent.getStringExtra(EXTRA_BLOOD_TYPE) ?: "--"
        val conditions = intent.getStringExtra(EXTRA_CONDITIONS) ?: "Không có ghi chú"
        val allergies = intent.getStringExtra(EXTRA_ALLERGIES) ?: "Không có ghi chú"
        val timestamp = intent.getLongExtra(EXTRA_TIMESTAMP, System.currentTimeMillis())

        setContent {
            FamilyCareTheme {
                EmergencyAlertReceiverScreen(
                    senderName = senderName,
                    senderPhone = senderPhone,
                    latitude = latitude,
                    longitude = longitude,
                    address = address,
                    bloodType = bloodType,
                    conditions = conditions,
                    allergies = allergies,
                    timestamp = timestamp,
                    onDismissSound = {
                        // Tắt còi hú và rung trên Máy B
                        EmergencySirenController.stopSiren(this)
                        Toast.makeText(this, "Đã tắt còi hú báo động", Toast.LENGTH_SHORT).show()
                    },
                    onCloseScreen = {
                        EmergencySirenController.stopSiren(this)
                        finish()
                    }
                )
            }
        }
    }

    private fun setupLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Đảm bảo không phát còi rò rỉ khi activity bị destroy
        EmergencySirenController.stopSiren(this)
    }
}

@Composable
fun EmergencyAlertReceiverScreen(
    senderName: String,
    senderPhone: String,
    latitude: Double,
    longitude: Double,
    address: String,
    bloodType: String,
    conditions: String,
    allergies: String,
    timestamp: Long,
    onDismissSound: () -> Unit,
    onCloseScreen: () -> Unit
) {
    val context = LocalContext.current
    var isSoundSilenced by remember { mutableStateOf(!EmergencySirenController.isSirenPlaying()) }

    val formattedTime = remember(timestamp) {
        val sdf = SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    }

    // Hiệu ứng nhấp nháy báo động nổi bật
    val infiniteTransition = rememberInfiniteTransition(label = "alertBlink")
    val scaleAnim by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleAnim"
    )

    Scaffold(
        containerColor = Color(0xFFFEF2F2),
        bottomBar = {
            // Nút Tắt Tiếng Cục Bộ Tại Chân Trang (Giữ nguyên màn hình cứu hộ)
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            isSoundSilenced = true
                            onDismissSound()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSoundSilenced) Color(0xFF475569) else Color(0xFFDC2626)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(
                            imageVector = if (isSoundSilenced) Icons.Default.VolumeOff else Icons.Default.VolumeMute,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isSoundSilenced) "CÒI HÚ ĐÃ TẮT (GIỮ MÀN HÌNH CỨU TRỢ)" else "🔕 TẮT SOS",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                    }

                    TextButton(
                        onClick = onCloseScreen,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Đóng màn hình báo động",
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER CẢNH BÁO CẤP CỨU NHẤP NHÁY
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFDC2626)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .scale(scaleAnim)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Emergency,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "CẢNH BÁO CẤP CỨU GIA ĐÌNH",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "$senderName đang gặp nguy hiểm!",
                        color = Color(0xFFFEF08A),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Thời điểm kích hoạt: $formattedTime",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }
            }

            // 2. KHỐI VỊ TRÍ THỰC TẾ & NÚT DẪN ĐƯỜNG TỚI NGAY
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vị trí thực tế của người thân",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = address,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color(0xFF1E293B),
                        lineHeight = 20.sp
                    )

                    if (latitude != 0.0 && longitude != 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "GPS: $latitude, $longitude",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nút to bản: Mở Bản Đồ Dẫn Đường Tới Ngay
                    Button(
                        onClick = {
                            if (latitude != 0.0 && longitude != 0.0) {
                                val uri = Uri.parse("google.navigation:q=$latitude,$longitude")
                                val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$latitude,$longitude")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            } else {
                                Toast.makeText(context, "Chưa có tọa độ GPS hợp lệ", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D4ED8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Navigation, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🗺️ Mở Bản Đồ Dẫn Đường Tới Ngay",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                    }
                }
            }

            // 3. KHỐI TÓM TẮT Y TẾ (MEDICAL ID)
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MedicalServices,
                            contentDescription = null,
                            tint = Color(0xFFB91C1C),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Hồ sơ y tế khẩn cấp (Medical ID)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MedicalInfoBadge(
                            label = "Nhóm máu",
                            value = bloodType.ifBlank { "--" },
                            modifier = Modifier.weight(1f),
                            highlight = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    MedicalDetailRow(
                        icon = Icons.Default.Favorite,
                        title = "Bệnh nền mãn tính",
                        content = conditions.ifBlank { "Chưa ghi nhận bệnh nền" }
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    MedicalDetailRow(
                        icon = Icons.Default.WarningAmber,
                        title = "Dị ứng thuốc & thực phẩm",
                        content = allergies.ifBlank { "Không có ghi chú dị ứng" }
                    )
                }
            }

            // 4. KHỐI LIÊN LẠC NHANH
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (senderPhone.isNotBlank()) {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${senderPhone.replace(".", "")}"))
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, "Chưa có số điện thoại của người thân", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gọi Người Thân", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }

                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:115"))
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gọi Cấp Cứu 115", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun MedicalInfoBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) Color(0xFFFEF2F2) else Color(0xFFF1F5F9),
        shape = RoundedCornerShape(10.dp),
        border = if (highlight) ButtonDefaults.outlinedButtonBorder else null,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Bloodtype,
                contentDescription = null,
                tint = if (highlight) Color(0xFFDC2626) else Color(0xFF64748B),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: ",
                fontSize = 13.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) Color(0xFFDC2626) else Color(0xFF0F172A)
            )
        }
    }
}

@Composable
private fun MedicalDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF64748B)
            )
            Text(
                text = content,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B)
            )
        }
    }
}
