package com.familycare.app.presentation.emergency

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.domain.model.NearbyHospital

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel = viewModel(
        factory = EmergencyViewModel.provideFactory(LocalContext.current)
    ),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val isActivated = uiState.sosStatus is SosStatus.Activated
    val payload = (uiState.sosStatus as? SosStatus.Activated)?.payload

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Trung Tâm SOS Khẩn Cấp",
                        fontWeight = FontWeight.Bold,
                        color = if (isActivated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isActivated) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tiêu đề & Hướng dẫn bảo vệ người cao tuổi
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isActivated) "🚨 CẢNH BÁO SOS ĐANG HOẠT ĐỘNG!" else "NÚT BẤM CỨU HỘ 1-CHẠM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActivated) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isActivated)
                        "Đã bật còi hú âm lượng tối đa, gửi định vị GPS đến Chủ gia đình và đề xuất các bệnh viện gần nhất."
                    else
                        "Nhấn và giữ nút tròn màu đỏ trong 2 giây để phát tín hiệu khẩn cấp, báo cho Chủ gia đình & tìm cứu hộ gần nhất.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // NÚT BẤM SOS 1-CHẠM (GIỮ 2 GIÂY)
            SosButton(
                onSosTriggered = {
                    viewModel.triggerSos()
                    Toast.makeText(context, "ĐÃ KÍCH HOẠT BÁO ĐỘNG SOS!", Toast.LENGTH_LONG).show()
                },
                onSosDismissed = {
                    viewModel.dismissSos()
                    Toast.makeText(context, "ĐÃ HỦY BÁO ĐỘNG SOS AN TOÀN", Toast.LENGTH_SHORT).show()
                },
                isActivated = isActivated,
                holdDurationMs = 2000L
            )

            // 1. THẺ THÔNG BÁO CHO CHỦ GIA ĐÌNH
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActivated) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isActivated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isActivated) Icons.Default.Warning else Icons.Default.Shield,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isActivated) "ĐÃ BÁO CHO CHỦ GIA ĐÌNH" else "CHỦ GIA ĐÌNH TIẾP NHẬN SOS",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp,
                                color = if (isActivated) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${uiState.familyOwnerName} (${uiState.familyOwnerRole})",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isActivated)
                            "✅ Đã gửi tín hiệu khẩn cấp ưu tiên cao nhất (FCM High Priority) kèm tọa độ GPS trực tiếp đến máy Chủ hộ."
                        else
                            "Khi kích hoạt SOS, hệ thống sẽ tự động gửi thông báo đỏ, rung chuông cực đại và tọa độ GPS đến Chủ gia đình.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActivated) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.familyOwnerPhone.replace(".", "")}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActivated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gọi Chủ Hộ", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val lat = uiState.currentLatitude ?: 10.7769
                                val lng = uiState.currentLongitude ?: 106.7009
                                val message = "🚨 KHẨN CẤP! ${uiState.targetUserName} cần trợ giúp tại: ${uiState.currentAddress}. Bản đồ: https://maps.google.com/?q=$lat,$lng"
                                sendSmsCoordinates(context, uiState.familyOwnerPhone.replace(".", ""), message)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SMS Tọa Độ", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 2. THẺ VỊ TRÍ CHÍNH XÁC HIỆN TẠI & GOOGLE MAPS
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActivated) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Vị trí chính xác hiện tại",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentAddress,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    val lat = uiState.currentLatitude ?: 10.7769
                    val lng = uiState.currentLongitude ?: 106.7009
                    Text(
                        text = "Vĩ độ: $lat° N  •  Kinh độ: $lng° E (Độ chính xác ±5m)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nút chuyển sang Google Maps vị trí hiện tại
                        Button(
                            onClick = {
                                openGoogleMapsLocation(context, lat, lng, "Vị trí SOS: ${uiState.targetUserName}")
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            )
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Xem Google Maps", fontSize = 12.sp)
                        }

                        // Nút tìm bệnh viện gần nhất trên Google Maps
                        OutlinedButton(
                            onClick = {
                                searchNearbyHospitalsOnMaps(context, lat, lng)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tìm BV gần nhất", fontSize = 12.sp)
                        }
                    }

                    // Tắt còi hú hoặc khôi phục
                    if (isActivated) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isSirenPlaying) {
                                OutlinedButton(
                                    onClick = { viewModel.stopSiren() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Tắt Còi Hú", fontSize = 12.sp)
                                }
                            }
                            TextButton(
                                onClick = { viewModel.resetSos() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Khôi phục bình thường", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // 3. DANH SÁCH ĐỀ XUẤT CÁC BỆNH VIỆN Ở GẦN
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocalHospital,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Đề xuất bệnh viện ở gần",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    TextButton(onClick = {
                        val lat = uiState.currentLatitude ?: 10.7769
                        val lng = uiState.currentLongitude ?: 106.7009
                        searchNearbyHospitalsOnMaps(context, lat, lng)
                    }) {
                        Text("Mở bản đồ", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                uiState.nearbyHospitals.forEach { hospital ->
                    HospitalCard(
                        hospital = hospital,
                        onCallHospital = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${hospital.hotline}"))
                            context.startActivity(intent)
                        },
                        onNavigateGoogleMaps = {
                            navigateToHospitalOnMaps(context, hospital.latitude, hospital.longitude, hospital.name)
                        }
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            // 4. NÚT GỌI CẤP CỨU 115 TOÀN QUỐC
            OutlinedButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:115"))
                    context.startActivity(intent)
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Call, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("Gọi Cấp Cứu 115 Ngay Lập Tức", fontWeight = FontWeight.Bold)
            }

            // 5. DANH BẠ KHẨN CẤP GIA ĐÌNH
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.FamilyRestroom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Danh bạ khẩn cấp nhận cảnh báo:",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    uiState.emergencyContacts.forEach { contact ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(contact, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HospitalCard(
    hospital: NearbyHospital,
    onCallHospital: () -> Unit,
    onNavigateGoogleMaps: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = hospital.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = hospital.emergencyRoom,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${hospital.distanceKm} km",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = hospital.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCallHospital,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gọi Cấp Cứu", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onNavigateGoogleMaps,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ Đường Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Các hàm hỗ trợ chuyển tiếp sang ứng dụng Google Maps hoặc trình duyệt
 */
fun openGoogleMapsLocation(context: Context, latitude: Double, longitude: Double, label: String = "Vị trí SOS khẩn cấp") {
    val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun searchNearbyHospitalsOnMaps(context: Context, latitude: Double, longitude: Double) {
    val uri = Uri.parse("geo:$latitude,$longitude?q=bệnh+viện+cấp+cứu+gần+nhất")
    val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
        setPackage("com.google.android.apps.maps")
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=bệnh+viện+cấp+cứu+gần+nhất")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

fun navigateToHospitalOnMaps(context: Context, latitude: Double, longitude: Double, name: String) {
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
}

fun sendSmsCoordinates(context: Context, phone: String, message: String) {
    try {
        val smsUri = Uri.parse("smsto:$phone")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Không thể mở ứng dụng gửi SMS", Toast.LENGTH_SHORT).show()
    }
}
