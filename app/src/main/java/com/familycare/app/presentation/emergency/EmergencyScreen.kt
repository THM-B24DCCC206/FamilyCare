package com.familycare.app.presentation.emergency

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.domain.model.NearbyHospital
import java.util.Locale

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
    val lifecycleOwner = LocalLifecycleOwner.current
    val isActivated = uiState.sosStatus is SosStatus.Activated

    // 1. Quản lý yêu cầu cấp quyền vị trí Runtime tự động
    val locationPermissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            viewModel.refreshLocation()
        }
    }

    // Tự động kích hoạt hộp thoại xin quyền vị trí nếu chưa được cấp
    LaunchedEffect(Unit) {
        val fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            permissionLauncher.launch(locationPermissions)
        } else {
            viewModel.refreshLocation()
        }
    }

    // Tự động kiểm tra và refresh vị trí khi người dùng quay lại màn hình từ Cài đặt hệ thống (ON_RESUME)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLocation()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
            // 1. TIÊU ĐỀ & DÒNG HƯỚNG DẪN CĂN GIỮA DƯỚI TIÊU ĐỀ
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isActivated) "🚨 CẢNH BÁO SOS ĐANG HOẠT ĐỘNG!" else "NÚT BẤM CỨU HỘ 1-CHẠM",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isActivated) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isActivated)
                        "Đã bật còi hú âm lượng tối đa, gửi định vị GPS đến người thân và đề xuất các bệnh viện gần nhất."
                    else
                        "Nhấn và giữ nút tròn màu đỏ trong 2 giây để phát tín hiệu khẩn cấp, báo cho người thân & tìm cứu hộ gần nhất.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFF64748B),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            // 2. NÚT BẤM SOS TRÒN LỚN 2 TRẠNG THÁI (GIỮ 2 GIÂY ĐỂ BẬT / GIỮ 2 GIÂY ĐỂ TẮT)
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

            // 3. THẺ THÔNG BÁO CHO NGƯỜI TIẾP NHẬN
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
                                text = if (isActivated) uiState.recipientCardTitleActive else uiState.recipientCardTitle,
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
                        text = if (isActivated) uiState.recipientDescriptionActive else uiState.recipientDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActivated) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nút Gọi Người Thân / Gọi Chủ Hộ (Gọn gàng 1 dòng không gãy chữ)
                        Button(
                            onClick = {
                                if (uiState.familyOwnerPhone.isNotBlank()) {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${uiState.familyOwnerPhone.replace(".", "")}"))
                                    context.startActivity(intent)
                                } else {
                                    Toast.makeText(context, "Chưa có số điện thoại", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isActivated) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.recipientCallButtonText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Nút SMS Tọa Độ
                        OutlinedButton(
                            onClick = {
                                val lat = uiState.currentLatitude
                                val lng = uiState.currentLongitude
                                val mapLink = if (lat != null && lng != null) "https://maps.google.com/?q=$lat,$lng" else "Chưa có tọa độ GPS"
                                val message = "🚨 KHẨN CẤP! ${uiState.targetUserName} cần trợ giúp tại: ${uiState.currentAddress}. Bản đồ: $mapLink"
                                sendSmsCoordinates(context, uiState.familyOwnerPhone.replace(".", ""), message)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SMS Tọa Độ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // 4. THẺ VỊ TRÍ CHÍNH XÁC HIỆN TẠI (TỰ ĐỘNG ĐỊNH VỊ NGẦM, XÓA BỎ HOÀN TOÀN TỌA ĐỘ ẢO)
            val isMissingPermission = !uiState.isLocationPermissionGranted
            val isGpsOff = !uiState.isGpsEnabled

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isActivated) {
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    } else if (isMissingPermission || isGpsOff) {
                        Color(0xFFFFF7ED)
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder(),
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isMissingPermission || isGpsOff) {
                            Modifier.clickable {
                                if (isMissingPermission) {
                                    permissionLauncher.launch(locationPermissions)
                                } else if (isGpsOff) {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Vui lòng mở Cài đặt để bật Vị trí", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } else Modifier
                    )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (isMissingPermission || isGpsOff) Color(0xFFEA580C) else Color(0xFFD32F2F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Vị trí chính xác hiện tại",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        if (isMissingPermission) {
                            TextButton(
                                onClick = { permissionLauncher.launch(locationPermissions) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Cấp quyền", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                            }
                        } else if (isGpsOff) {
                            TextButton(
                                onClick = {
                                    try {
                                        context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "Vui lòng mở Cài đặt để bật Vị trí", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                            ) {
                                Text("Bật GPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEA580C))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.currentAddress,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isMissingPermission || isGpsOff) Color(0xFF9A3412) else MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )

                    // Chỉ hiển thị tọa độ GPS khi đã định vị ngầm thành công, không dùng tọa độ ảo
                    val lat = uiState.currentLatitude
                    val lng = uiState.currentLongitude
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tọa độ GPS: %.5f° N • %.5f° E (Độ chính xác cao)".format(Locale.US, lat, lng),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Nút chuyển sang Google Maps
                        Button(
                            onClick = {
                                if (lat != null && lng != null) {
                                    openGoogleMapsLocation(context, lat, lng, "Vị trí SOS: ${uiState.targetUserName}")
                                } else {
                                    val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(uiState.currentAddress)}")
                                    context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E88E5)
                            )
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Xem Google Maps",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Nút tìm bệnh viện gần nhất (Gọn gàng 1 dòng không gãy chữ)
                        OutlinedButton(
                            onClick = {
                                searchNearbyHospitalsOnMaps(context, lat, lng)
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Tìm BV gần nhất",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Nút tắt còi hú hoặc khôi phục khi đang báo động
                    if (isActivated) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (uiState.isSirenPlaying) {
                                OutlinedButton(
                                    onClick = { viewModel.stopSiren() },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Tắt Còi Hú", fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            TextButton(
                                onClick = { viewModel.resetSos() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Khôi phục bình thường", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }

            // 5. DANH SÁCH ĐỀ XUẤT CÁC BỆNH VIỆN Ở GẦN
            if (uiState.nearbyHospitals.isNotEmpty()) {
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
                            searchNearbyHospitalsOnMaps(context, uiState.currentLatitude, uiState.currentLongitude)
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
            }

            // 6. NÚT GỌI CẤP CỨU 115 TOÀN QUỐC
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
                Text("Gọi Cấp Cứu 115 Ngay Lập Tức", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // 7. DANH BẠ KHẨN CẤP GIA ĐÌNH
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

            // 8. KHOẢNG ĐỆM AN TOÀN CHÂN TRANG (BOTTOM SPACING: 32.dp)
            Spacer(modifier = Modifier.height(32.dp))
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gọi Cấp Cứu", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }

                Button(
                    onClick = onNavigateGoogleMaps,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5))
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chỉ Đường Maps", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
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

fun searchNearbyHospitalsOnMaps(context: Context, latitude: Double? = null, longitude: Double? = null) {
    val geoUriString = if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
        "geo:$latitude,$longitude?q=bệnh+viện+cấp+cứu+gần+nhất"
    } else {
        "geo:0,0?q=bệnh+viện+cấp+cứu+gần+nhất"
    }
    val uri = Uri.parse(geoUriString)
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
        val cleanPhone = phone.replace(".", "").trim()
        val smsUri = Uri.parse("smsto:$cleanPhone")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "Không thể mở ứng dụng gửi SMS", Toast.LENGTH_SHORT).show()
    }
}
