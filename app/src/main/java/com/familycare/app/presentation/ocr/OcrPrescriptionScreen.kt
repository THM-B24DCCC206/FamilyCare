package com.familycare.app.presentation.ocr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.MedicationLogEntity
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.data.repository.HealthAiRepositoryImpl
import com.familycare.app.presentation.medication.MedicationAlarmScheduler
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "ScanPrescriptionAI"

/**
 * Model biểu diễn thuốc sau khi được AI bóc tách & chuẩn hóa
 */
data class ScannedMedicineItem(
    val id: String = UUID.randomUUID().toString(),
    var isSelected: Boolean = true,
    var medicineName: String = "",
    var dosage: String = "",
    var unit: String = "viên",
    val times: MutableList<String> = mutableListOf("08:00"),
    var note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrPrescriptionScreen(
    memberId: String = "",
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sessionManager = remember { SessionManager(context) }
    val database = remember { AppDatabase.getInstance(context) }
    val alarmScheduler = remember { MedicationAlarmScheduler(context) }

    val targetMemberId = memberId.ifBlank { sessionManager.userId.ifBlank { "user_me" } }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val previewBitmap: Bitmap? = remember(selectedImageUri) {
        selectedImageUri?.let { uri ->
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisStepText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val scannedItems = remember { mutableStateListOf<ScannedMedicineItem>() }
    var showManualAddDialog by remember { mutableStateOf(false) }

    // Xử lý tạo Uri tạm cho Camera
    fun createTempCameraUri(): Uri {
        val cacheFile = File.createTempFile("rx_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
    }

    // Tiến hành OCR và phân tích bằng Gemini AI (kèm fallback từ điển dược phẩm)
    fun processImageUri(uri: Uri) {
        selectedImageUri = uri
        isAnalyzing = true
        errorMessage = ""
        analysisStepText = "Đang quét văn bản OCR từ hình ảnh..."

        coroutineScope.launch {
            try {
                // 1. Quét văn bản bằng Google ML Kit
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(inputImage)
                    .addOnSuccessListener { visionText ->
                        val rawText = visionText.text
                        if (rawText.isBlank()) {
                            isAnalyzing = false
                            errorMessage = "Không tìm thấy văn bản trong ảnh. Vui lòng chụp rõ nét hơn hoặc chọn ảnh khác."
                            return@addOnSuccessListener
                        }

                        // 2. Chuyển sang bước Gemini AI bóc tách & chuẩn hóa
                        analysisStepText = "AI đang đọc và chuẩn hóa tên đơn thuốc..."
                        coroutineScope.launch {
                            val items = parsePrescriptionWithAiOrFallback(rawText)
                            isAnalyzing = false
                            if (items.isEmpty()) {
                                errorMessage = "Chưa tách được danh mục thuốc tự động. Bạn có thể thêm thủ công bên dưới."
                            } else {
                                scannedItems.clear()
                                scannedItems.addAll(items)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        isAnalyzing = false
                        Log.e(TAG, "Lỗi nhận diện văn bản OCR", e)
                        errorMessage = "Không thể nhận diện hình ảnh: ${e.localizedMessage ?: "Lỗi không xác định"}"
                    }
                    .addOnCompleteListener {
                        recognizer.close()
                    }
            } catch (e: Exception) {
                isAnalyzing = false
                Log.e(TAG, "Lỗi mở file ảnh", e)
                errorMessage = "Không thể đọc tệp ảnh: ${e.localizedMessage ?: "Vui lòng thử lại"}"
            }
        }
    }

    // Launcher chụp ảnh từ Camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            processImageUri(tempCameraUri!!)
        } else {
            Log.d(TAG, "Hủy chụp ảnh bằng camera")
        }
    }

    // Launcher xin quyền Camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val newUri = createTempCameraUri()
            tempCameraUri = newUri
            takePictureLauncher.launch(newUri)
        } else {
            Toast.makeText(context, "Cần cấp quyền Camera để chụp đơn thuốc", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher chọn ảnh từ Thư viện
    val pickGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Quét Đơn Thuốc AI",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 2 NGUỒN NHẬP ẢNH TRỰC QUAN
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // 1. Chụp ảnh bằng Camera
                Button(
                    onClick = {
                        val hasCameraPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED

                        if (hasCameraPermission) {
                            val newUri = createTempCameraUri()
                            tempCameraUri = newUri
                            takePictureLauncher.launch(newUri)
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isAnalyzing
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chụp ảnh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Chọn ảnh từ Thư viện
                FilledTonalButton(
                    onClick = {
                        pickGalleryLauncher.launch("image/*")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFEFF6FF),
                        contentColor = Color(0xFF1E40AF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    enabled = !isAnalyzing
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chọn từ thư viện", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Thumbnail xem trước ảnh đã chụp/chọn
            if (selectedImageUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (previewBitmap != null) {
                            Image(
                                bitmap = previewBitmap.asImageBitmap(),
                                contentDescription = "Ảnh đơn thuốc",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Đã tải ảnh đơn thuốc",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Chạm nút phía trên để chụp hoặc chọn lại",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // MÀN HÌNH / KHỐI LOADING
            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF2563EB),
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "AI đang đọc và chuẩn hóa tên đơn thuốc...",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E40AF)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = analysisStepText,
                            fontSize = 12.sp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }

            // Thông báo lỗi nếu có
            if (errorMessage.isNotBlank() && !isAnalyzing) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage,
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // GIAO DIỆN XEM LẠI & XÁC NHẬN (REVIEW SCREEN)
            if (scannedItems.isNotEmpty() && !isAnalyzing) {
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val selectedCount = scannedItems.count { it.isSelected }
                    Text(
                        text = "Danh sách thuốc bóc tách ($selectedCount/${scannedItems.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row {
                        TextButton(
                            onClick = {
                                val allSelected = scannedItems.all { it.isSelected }
                                scannedItems.forEach { it.isSelected = !allSelected }
                            },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (scannedItems.all { it.isSelected }) "Bỏ chọn tất cả" else "Chọn tất cả",
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB)
                            )
                        }

                        TextButton(
                            onClick = { showManualAddDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(text = "Thêm", fontSize = 12.sp, color = Color(0xFF2563EB))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    itemsIndexed(scannedItems, key = { _, item -> item.id }) { index, item ->
                        ScannedMedicineReviewCard(
                            item = item,
                            onToggleSelect = { isChecked ->
                                scannedItems[index] = item.copy(isSelected = isChecked)
                            },
                            onUpdate = { updated ->
                                scannedItems[index] = updated
                            },
                            onDelete = {
                                scannedItems.removeAt(index)
                            }
                        )
                    }
                }

                // NÚT LƯU CÁC THUỐC ĐÃ CHỌN VÀO LỊCH UỐNG
                val selectedCount = scannedItems.count { it.isSelected }
                Button(
                    onClick = {
                        val toSave = scannedItems.filter { it.isSelected && it.medicineName.isNotBlank() }
                        if (toSave.isEmpty()) {
                            Toast.makeText(context, "Vui lòng chọn ít nhất 1 mục thuốc để lưu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        coroutineScope.launch(Dispatchers.IO) {
                            val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date())
                            for (item in toSave) {
                                val dosageWithUnit = if (item.unit.isNotBlank() && !item.dosage.contains(item.unit)) {
                                    "${item.dosage} ${item.unit}".trim()
                                } else {
                                    item.dosage
                                }

                                val medicineEntity = MedicineEntity(
                                    id = UUID.randomUUID().toString(),
                                    userId = targetMemberId,
                                    name = item.medicineName.trim(),
                                    dosage = dosageWithUnit.ifBlank { "Theo chỉ định" },
                                    instruction = item.note.trim(),
                                    reminderTimes = item.times.distinct().sorted(),
                                    startDate = System.currentTimeMillis(),
                                    scannedViaOcr = true
                                )

                                database.medicineDao.insertMedicine(medicineEntity)

                                // Tạo MedicationLog cho ngày hôm nay
                                for (time in medicineEntity.reminderTimes) {
                                    database.medicationLogDao.insertIfAbsent(
                                        MedicationLogEntity(
                                            id = UUID.randomUUID().toString(),
                                            medicineId = medicineEntity.id,
                                            medicineName = medicineEntity.name,
                                            userId = targetMemberId,
                                            scheduledDate = todayDate,
                                            scheduledTime = time,
                                            isTaken = false,
                                            isSkipped = false
                                        )
                                    )
                                }

                                // Đặt lịch nhắc báo thức
                                alarmScheduler.schedule(
                                    medicineEntity.id,
                                    medicineEntity.name,
                                    medicineEntity.dosage,
                                    medicineEntity.reminderTimes
                                )
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Đã lưu thành công ${toSave.size} thuốc vào lịch uống!",
                                    Toast.LENGTH_LONG
                                ).show()
                                onBack()
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF16A34A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(bottom = 4.dp),
                    enabled = selectedCount > 0
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Lưu $selectedCount thuốc đã chọn vào Lịch uống",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (!isAnalyzing && scannedItems.isEmpty() && selectedImageUri == null) {
                // Hướng dẫn ban đầu khi chưa chụp/chọn ảnh
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFEFF6FF),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.DocumentScanner,
                                    contentDescription = null,
                                    tint = Color(0xFF2563EB),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Quét đơn thuốc bằng AI",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chụp ảnh đơn thuốc của bác sĩ hoặc tải ảnh từ thư viện. Trợ lý AI sẽ tự động đọc, sửa lỗi chính tả tên thuốc và lên lịch uống từng cữ chuẩn y tế.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 18.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }

    // Dialog thêm thuốc thủ công vào danh sách review
    if (showManualAddDialog) {
        var addName by remember { mutableStateOf("") }
        var addDosage by remember { mutableStateOf("") }
        var addUnit by remember { mutableStateOf("viên") }
        var addNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Thêm thuốc vào danh sách", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Tên thuốc") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addDosage,
                            onValueChange = { addDosage = it },
                            label = { Text("Hàm lượng") },
                            placeholder = { Text("5mg...") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = addUnit,
                            onValueChange = { addUnit = it },
                            label = { Text("Đơn vị") },
                            modifier = Modifier.width(85.dp)
                        )
                    }
                    OutlinedTextField(
                        value = addNote,
                        onValueChange = { addNote = it },
                        label = { Text("Lời dặn") },
                        placeholder = { Text("Sau ăn...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addName.isNotBlank()) {
                            scannedItems.add(
                                ScannedMedicineItem(
                                    medicineName = addName.trim(),
                                    dosage = addDosage.trim(),
                                    unit = addUnit.trim(),
                                    times = mutableListOf("08:00"),
                                    note = addNote.trim(),
                                    isSelected = true
                                )
                            )
                        }
                        showManualAddDialog = false
                    }
                ) {
                    Text("Thêm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualAddDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

/**
 * Thẻ xem lại thuốc bóc tách: Checkbox + chỉnh sửa nhanh tên / hàm lượng / giờ / ghi chú + nút xóa
 */
@Composable
fun ScannedMedicineReviewCard(
    item: ScannedMedicineItem,
    onToggleSelect: (Boolean) -> Unit,
    onUpdate: (ScannedMedicineItem) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(item.id) { mutableStateOf(item.medicineName) }
    var dosage by remember(item.id) { mutableStateOf(item.dosage) }
    var note by remember(item.id) { mutableStateOf(item.note) }
    val timesList = remember(item.id) { mutableStateListOf<String>().apply { addAll(item.times) } }

    fun notifyChange() {
        onUpdate(
            item.copy(
                medicineName = name,
                dosage = dosage,
                note = note,
                times = timesList.toMutableList()
            )
        )
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isSelected) MaterialTheme.colorScheme.surface else Color(0xFFF8FAFC)
        ),
        border = BorderStroke(
            1.dp,
            if (item.isSelected) Color(0xFF93C5FD) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isSelected) 2.dp else 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Hàng Checkbox + Tiêu đề mục + Nút Xóa
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = item.isSelected,
                        onCheckedChange = { onToggleSelect(it) }
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thuốc: ${name.ifBlank { "Chưa đặt tên" }}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSelected) Color(0xFF1E40AF) else Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Xóa thuốc này",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ô nhập chỉnh sửa Tên thuốc
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    notifyChange()
                },
                label = { Text("Tên thuốc chuẩn hóa *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ô nhập Hàm lượng & Đơn vị
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = dosage,
                    onValueChange = {
                        dosage = it
                        notifyChange()
                    },
                    label = { Text("Hàm lượng") },
                    placeholder = { Text("5mg, 500mg...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = item.unit,
                    onValueChange = {
                        item.unit = it
                        notifyChange()
                    },
                    label = { Text("Đơn vị") },
                    singleLine = true,
                    modifier = Modifier.width(90.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Giờ uống (Dạng Chip)
            Text(
                text = "Giờ uống trong ngày:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                timesList.forEach { time ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = time, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF1E40AF))
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Xóa giờ",
                                tint = Color(0xFF64748B),
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable {
                                        timesList.remove(time)
                                        notifyChange()
                                    }
                            )
                        }
                    }
                }

                // Nút thêm giờ uống nhanh
                val quickTimes = listOf("08:00", "12:00", "18:00", "20:00")
                quickTimes.forEach { qTime ->
                    if (!timesList.contains(qTime)) {
                        SuggestionChip(
                            onClick = {
                                timesList.add(qTime)
                                notifyChange()
                            },
                            label = { Text("+$qTime", fontSize = 11.sp) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Ô nhập Lời dặn
            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                    notifyChange()
                },
                label = { Text("Lời dặn / Cách dùng") },
                placeholder = { Text("Ví dụ: Uống sau khi ăn no") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Xử lý bóc tách đơn thuốc bằng Google Gemini AI hoặc Fallback Dược thư Offline
 */
private suspend fun parsePrescriptionWithAiOrFallback(rawOcrText: String): List<ScannedMedicineItem> =
    withContext(Dispatchers.IO) {
        val apiKey = HealthAiRepositoryImpl.DEFAULT_API_KEY

        // Nếu có API key hợp lệ, gọi Gemini AI
        if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
            try {
                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-flash",
                    apiKey = apiKey,
                    generationConfig = generationConfig {
                        temperature = 0.2f
                        topK = 30
                        topP = 0.9f
                    }
                )

                val prompt = """
                    Bạn là Dược sĩ AI chuyên gia về đơn thuốc và y tế tại Việt Nam.
                    Nhiệm vụ: Phân tích nội dung văn bản OCR từ đơn thuốc bác sĩ sau:
                    ---
                    $rawOcrText
                    ---
                    Yêu cầu:
                    1. Chuẩn hóa tên thuốc về đúng tên biệt dược hoặc hoạt chất chuẩn y tế (Ví dụ: "Arnlodipine" -> "Amlodipine", "Paracetamon" -> "Paracetamol", "Panadol extra" -> "Panadol Extra", "Sucrate gel" -> "Sucrate Gel", "Glucophage xr" -> "Glucophage XR",...).
                    2. Tách chính xác hàm lượng (ví dụ: 5mg, 500mg, 10mg, 1g,...).
                    3. Xác định đơn vị (viên, gói, ống, lọ, tuýp, v.v.).
                    4. Bóc tách các cữ giờ uống trong ngày ở định dạng ["08:00", "12:00", "18:00", "20:00"] tương ứng với sáng, trưa, chiều, tối hoặc theo chỉ định bác sĩ.
                    5. Trích xuất lời dặn / hướng dẫn sử dụng (ví dụ: "Uống sau khi ăn no", "Uống trước ăn sáng 30 phút",...).
                    6. Xuất kết quả DUY NHẤT dưới dạng chuỗi JSON Array hợp lệ, KHÔNG viết bất kỳ văn bản nào khác:
                    [
                      {
                        "medicineName": "Amlodipine",
                        "dosage": "5mg",
                        "unit": "viên",
                        "times": ["08:00"],
                        "note": "Uống sau ăn sáng"
                      }
                    ]
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val reply = response.text?.trim().orEmpty()

                val jsonStart = reply.indexOf("[")
                val jsonEnd = reply.lastIndexOf("]")

                if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    val jsonStr = reply.substring(jsonStart, jsonEnd + 1)
                    val jsonArray = JSONArray(jsonStr)
                    val list = mutableListOf<ScannedMedicineItem>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val medName = obj.optString("medicineName", "").trim()
                        val dosage = obj.optString("dosage", "").trim()
                        val unit = obj.optString("unit", "viên").trim()
                        val note = obj.optString("note", "").trim()

                        val times = mutableListOf<String>()
                        val timesArray = obj.optJSONArray("times")
                        if (timesArray != null) {
                            for (j in 0 until timesArray.length()) {
                                val t = timesArray.optString(j, "").trim()
                                if (t.isNotBlank()) times.add(t)
                            }
                        }
                        if (times.isEmpty()) times.add("08:00")

                        if (medName.isNotBlank()) {
                            list.add(
                                ScannedMedicineItem(
                                    medicineName = medName,
                                    dosage = dosage,
                                    unit = unit.ifBlank { "viên" },
                                    times = times,
                                    note = note,
                                    isSelected = true
                                )
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Lỗi khi gọi Gemini AI, chuyển sang Fallback Offline", e)
            }
        }

        // SMART FALLBACK OFFLINE: Sử dụng Từ điển Dược phẩm Việt Nam phong phú
        fallbackParseMedicineOffline(rawOcrText)
    }

/**
 * Danh mục thuốc chuẩn offline cho Smart Fallback
 */
private data class LocalMedicineEntry(
    val standardName: String,
    val defaultDosage: String,
    val defaultUnit: String,
    val aliases: List<String>
)

private val localMedicineCatalog = listOf(
    LocalMedicineEntry("Amlodipine", "5mg", "viên", listOf("AMLODIPINE", "ARNLODIPINE", "AMLO", "AMORLODIPIN")),
    LocalMedicineEntry("Paracetamol", "500mg", "viên", listOf("PARACETAMOL", "PARACETAMON", "PARA", "EFFERALGAN", "PANADOL", "HAPACOL")),
    LocalMedicineEntry("Metformin", "500mg", "viên", listOf("METFORMIN", "GLUCOPHAGE", "DIAPHAGE")),
    LocalMedicineEntry("Losartan", "50mg", "viên", listOf("LOSARTAN", "COZAAR", "LOSATEC")),
    LocalMedicineEntry("Atorvastatin", "10mg", "viên", listOf("ATORVASTATIN", "LIPITOR", "ATOR")),
    LocalMedicineEntry("Esomeprazole", "40mg", "viên", listOf("ESOMEPRAZOLE", "ESOMEPRAZOL", "YESOM", "NEXIUM")),
    LocalMedicineEntry("Sucralfate", "1g", "gói", listOf("SUCRALFATE", "SUCRATE GEL", "SUCRATE")),
    LocalMedicineEntry("Trimebutine", "100mg", "viên", listOf("TRIMEBUTINE", "ARTHUR", "DEBRIDAT")),
    LocalMedicineEntry("Berberin", "100mg", "viên", listOf("BERBERIN", "BERBERINE")),
    LocalMedicineEntry("Amoxicillin", "500mg", "viên", listOf("AMOXICILLIN", "AMOX", "CLAMOCIL", "AUGMENTIN", "KLAMENTIN")),
    LocalMedicineEntry("Cefalexin", "500mg", "viên", listOf("CEFALEXIN", "CEPHALEXIN", "CEFALEX")),
    LocalMedicineEntry("Aspirin", "81mg", "viên", listOf("ASPIRIN", "ASPIRIN CARDIO", "ASPEC")),
    LocalMedicineEntry("Omeprazole", "20mg", "viên", listOf("OMEPRAZOLE", "OMEPRAZOL", "LOSEC"))
)

/**
 * Smart Fallback: Phân tích OCR bằng regex và so khớp danh mục thuốc
 */
private fun fallbackParseMedicineOffline(text: String): List<ScannedMedicineItem> {
    val result = mutableListOf<ScannedMedicineItem>()
    val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

    val dosageRegex = Regex("""(?i)\b\d+(?:[.,]\d+)?\s*(?:mg|mcg|g|ml|iu|ui|%)\b""")

    fun removeDiacritics(str: String): String {
        return Normalizer.normalize(str.uppercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace('Đ', 'D')
            .replace(Regex("[^A-Z0-9]+"), " ")
            .trim()
    }

    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val cleanLine = removeDiacritics(line)

        // Kiểm tra xem dòng này có chứa tên thuốc trong catalog không
        var matchedEntry: LocalMedicineEntry? = null
        for (entry in localMedicineCatalog) {
            for (alias in entry.aliases) {
                val cleanAlias = removeDiacritics(alias)
                if (cleanLine.contains(cleanAlias)) {
                    matchedEntry = entry
                    break
                }
            }
            if (matchedEntry != null) break
        }

        // Hoặc kiểm tra xem dòng có định dạng tên thuốc (ví dụ "1. Panadol 500mg" hoặc có dosage)
        val hasDosage = dosageRegex.containsMatchIn(line)

        if (matchedEntry != null || hasDosage) {
            val name = matchedEntry?.standardName ?: run {
                // Tách tên trước hàm lượng
                val before = if (hasDosage) line.substringBefore(dosageRegex.find(line)?.value ?: "").trim() else line
                before.replace(Regex("""^\d+[\s.)-]+"""), "").trim()
            }

            val dosage = dosageRegex.find(line)?.value ?: (matchedEntry?.defaultDosage ?: "1 viên")
            val unit = matchedEntry?.defaultUnit ?: "viên"

            // Đọc thêm 1-2 dòng tiếp theo để tìm giờ uống & lời dặn
            val times = mutableListOf<String>()
            val noteBuilder = StringBuilder()

            var lookAhead = 1
            while (i + lookAhead < lines.size && lookAhead <= 2) {
                val nextLine = lines[i + lookAhead]
                val nextClean = removeDiacritics(nextLine)

                if (nextClean.contains("SANG") || nextClean.contains("TRUA") || nextClean.contains("CHIEU") || nextClean.contains("TOI") || nextClean.contains("UONG")) {
                    if (nextClean.contains("SANG")) times.add("08:00")
                    if (nextClean.contains("TRUA")) times.add("12:00")
                    if (nextClean.contains("CHIEU") || nextClean.contains("TOI")) times.add("18:00")
                    if (noteBuilder.isNotEmpty()) noteBuilder.append(". ")
                    noteBuilder.append(nextLine)
                    i += lookAhead
                    break
                } else {
                    lookAhead++
                }
            }

            if (times.isEmpty()) {
                times.add("08:00")
            }

            if (name.length >= 3 && !name.contains("BỆNH", ignoreCase = true) && !name.contains("BÁC SĨ", ignoreCase = true)) {
                result.add(
                    ScannedMedicineItem(
                        medicineName = name,
                        dosage = dosage,
                        unit = unit,
                        times = times.distinct().toMutableList(),
                        note = noteBuilder.toString().ifBlank { "Uống sau khi ăn no" },
                        isSelected = true
                    )
                )
            }
        }
        i++
    }

    return result.distinctBy { it.medicineName }
}
