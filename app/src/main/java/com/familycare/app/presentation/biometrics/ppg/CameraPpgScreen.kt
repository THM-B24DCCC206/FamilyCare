package com.familycare.app.presentation.biometrics.ppg

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraPpgScreen(
    onBack: () -> Unit,
    viewModel: CameraPpgViewModel = viewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Biến điều khiển Flash
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Tự động bật đèn Flash khi đo, và tắt khi thoát màn hình
    DisposableEffect(cameraInstance) {
        cameraInstance?.cameraControl?.enableTorch(true)
        onDispose {
            cameraInstance?.cameraControl?.enableTorch(false)
        }
    }

    // Hiệu ứng tim đập theo BPM
    val infiniteTransition = rememberInfiniteTransition(label = "HeartbeatAnimation")
    val heartScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (uiState.isFingerDetected) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HeartScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đo Nhịp Tim Quang Học (PPG)", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasCameraPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Cần cấp quyền Camera để bật Flash và đo nhịp tim",
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("Cấp quyền Camera")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. Khung xem Camera nhỏ + Đèn báo trạng thái ngón tay
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isFingerDetected) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Khung tròn Camera Preview (kích thước 60x60dp)
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(Color.Black)
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx)
                                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                    val cameraExecutor = Executors.newSingleThreadExecutor()

                                    cameraProviderFuture.addListener({
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }

                                        val imageAnalysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                            .build()
                                            .also {
                                                it.setAnalyzer(
                                                    cameraExecutor,
                                                    CameraPpgAnalyzer { detected, intensity, bpm ->
                                                        viewModel.onFrameProcessed(detected, intensity, bpm)
                                                    }
                                                )
                                            }

                                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                        try {
                                            cameraProvider.unbindAll()
                                            val camera = cameraProvider.bindToLifecycle(
                                                lifecycleOwner,
                                                cameraSelector,
                                                preview,
                                                imageAnalysis
                                            )
                                            cameraInstance = camera
                                        } catch (exc: Exception) {
                                            exc.printStackTrace()
                                        }
                                    }, ContextCompat.getMainExecutor(ctx))

                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (uiState.isFingerDetected) "Đã nhận diện ngón tay" else "Chưa nhận diện ngón tay",
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isFingerDetected) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                            Text(
                                text = uiState.statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Banner hỗ trợ kiểm thử nếu đang chạy trên Máy ảo (Emulator)
                if (uiState.isEmulatorMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🧪 Môi trường máy ảo (Emulator)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF334155)
                                )
                                Text(
                                    text = "Camera máy ảo không có xung mạch thật. Bấm nút để mô phỏng tín hiệu PPG sinh học.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            FilledTonalButton(
                                onClick = { viewModel.startEmulatorSimulation() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Mô phỏng", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // 2. Trái tim đập + Chỉ số BPM to đùng ở trung tâm + Phân loại mức độ thời gian thực
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Nhịp tim",
                        tint = Color(0xFFE91E63),
                        modifier = Modifier
                            .size(72.dp)
                            .scale(heartScale)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (uiState.currentBpm > 0) "${uiState.currentBpm}" else "--",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFE91E63)
                    )
                    Text(
                        text = "NHỊP / PHÚT (BPM)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Phân loại mức độ nhịp tim real-time khi có tín hiệu
                    val liveLevel = HeartRateClassifier.classify(uiState.currentBpm)
                    if (uiState.currentBpm > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = liveLevel.containerColor,
                            border = BorderStroke(1.dp, liveLevel.borderColor)
                        ) {
                            Text(
                                text = "Mức độ: ${liveLevel.title} (${liveLevel.rangeText})",
                                color = liveLevel.color,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.5.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 3. Đồ thị sóng xung tim chạy thời gian thực (Custom Canvas Waveform)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sóng quang học mao mạch (PPG Waveform)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    PpgWaveformView(waveformPoints = uiState.waveformPoints)
                }

                // 4. Thanh tiến trình đo 15 giây
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Tiến trình phân tích:",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "${(uiState.progress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { uiState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFE91E63),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // 5. Bảng dải phân loại mức nhịp tim y tế (Tham khảo trực quan)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Bảng phân loại mức nhịp tim nghỉ ngơi (AHA):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = Color(0xFF1E293B)
                        )

                        HeartRateClassifier.referenceLevels.forEach { level ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(level.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = level.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = level.containerColor,
                                    border = BorderStroke(0.5.dp, level.borderColor)
                                ) {
                                    Text(
                                        text = level.rangeText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = level.color,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // Dialog thông báo kết quả khi đo hoàn tất 15 giây
    if (uiState.isCompleted) {
        if (uiState.hasMeasurementFailed || uiState.finalBpm <= 0) {
            // Trường hợp đo không thành công (ngón tay rung hoặc bị nhấc ra)
            AlertDialog(
                onDismissRequest = { /* Không tắt khi bấm ra ngoài */ },
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Tín Hiệu Chưa Đủ Ổn Định",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Không nhận diện đủ số nhịp tim rõ ràng trong 15 giây vừa qua.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vui lòng giữ ngón tay che kín đèn flash và camera sau, không xê dịch và giữ yên lặng khi đo.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.restart() }) {
                        Text("Đo lại ngay")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = onBack) {
                        Text("Quay lại")
                    }
                }
            )
        } else {
            // Trường hợp đo thành công với chỉ số nhịp tim thực tế
            val bpm = uiState.finalBpm
            val finalLevel = HeartRateClassifier.classify(bpm)

            AlertDialog(
                onDismissRequest = { /* Không tắt khi bấm ra ngoài */ },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = finalLevel.color,
                        modifier = Modifier.size(48.dp)
                    )
                },
                title = {
                    Text(
                        "Đo Nhịp Tim Hoàn Tất",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$bpm BPM",
                            fontSize = 46.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFE91E63)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = finalLevel.containerColor,
                            border = BorderStroke(1.dp, finalLevel.borderColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${finalLevel.title} (${finalLevel.rangeText})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = finalLevel.color
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = finalLevel.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = finalLevel.color,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Chỉ số này đã được tính toán từ trung bình các nhịp đập quang học mao mạch qua 15 giây.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val sessionManager = SessionManager(context)
                            viewModel.saveResult(sessionManager.userId.ifEmpty { "user_me" }) {
                                onBack()
                            }
                        }
                    ) {
                        Text("Lưu kết quả & Đóng")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { viewModel.restart() }) {
                        Text("Đo lại")
                    }
                }
            )
        }
    }
}
