package com.familycare.app.presentation.ocr

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.familycare.app.core.common.SessionManager
import java.io.File

private const val TAG = "ScanPrescriptionScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrPrescriptionScreen(
    memberId: String = "",
    onBack: () -> Unit,
    viewModel: ScanPrescriptionViewModel = viewModel()
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val targetMemberId = memberId.ifBlank { sessionManager.userId.ifBlank { "user_me" } }

    val uiState by viewModel.uiState.collectAsState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showManualAddDialog by remember { mutableStateOf(false) }

    fun createTempCameraUri(): Uri {
        val cacheFile = File.createTempFile("rx_${System.currentTimeMillis()}", ".jpg", context.cacheDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cacheFile
        )
    }

    // Launcher chụp ảnh từ Camera
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.processImage(context, tempCameraUri!!)
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
            viewModel.processImage(context, uri)
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
            Spacer(modifier = Modifier.height(8.dp))

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
                        .height(48.dp),
                    enabled = !uiState.isAnalyzing && !uiState.isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Chụp ảnh", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // 2. Chọn ảnh từ Thư viện
                FilledTonalButton(
                    onClick = { pickGalleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFFEFF6FF),
                        contentColor = Color(0xFF1E40AF)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    enabled = !uiState.isAnalyzing && !uiState.isSaving
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Thư viện", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Thumbnail xem trước ảnh đã chụp/chọn
            if (uiState.selectedImageUri != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (uiState.previewBitmap != null) {
                            Image(
                                bitmap = uiState.previewBitmap!!.asImageBitmap(),
                                contentDescription = "Ảnh đơn thuốc",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF64748B))
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ảnh đơn thuốc đã tải",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1E293B)
                            )
                            Text(
                                text = "Chạm nút trên để chụp lại nếu ảnh mờ",
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

            // KHỐI LOADING KHI AI ĐANG ĐỌC & CHUẨN HÓA
            if (uiState.isAnalyzing) {
                Spacer(modifier = Modifier.height(18.dp))
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
                            modifier = Modifier.size(40.dp)
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
                            text = uiState.analysisStepText,
                            fontSize = 12.sp,
                            color = Color(0xFF3B82F6)
                        )
                    }
                }
            }

            // KHỐI THÔNG BÁO LỖI NẾU CÓ
            if (uiState.errorMessage != null && !uiState.isAnalyzing) {
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
                            text = uiState.errorMessage ?: "",
                            color = Color(0xFFB91C1C),
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // GIAO DIỆN XEM LẠI & CHỈNH SỬA (REVIEW SCREEN)
            if (uiState.scannedItems.isNotEmpty() && !uiState.isAnalyzing) {
                Spacer(modifier = Modifier.height(14.dp))

                // Sửa lỗi layout: Hàng tiêu đề + Bỏ chọn + Thêm trên 1 dòng duy nhất, maxLines = 1 không rớt chữ
                val selectedCount = uiState.scannedItems.count { it.isSelected }
                val allSelected = uiState.scannedItems.all { it.isSelected }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Thuốc bóc tách ($selectedCount/${uiState.scannedItems.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextButton(
                            onClick = { viewModel.selectAll(!allSelected) },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (allSelected) "Bỏ chọn" else "Chọn tất cả",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB),
                                maxLines = 1
                            )
                        }

                        TextButton(
                            onClick = { showManualAddDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "+ Thêm",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB),
                                maxLines = 1
                            )
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
                    items(uiState.scannedItems, key = { it.id }) { item ->
                        ScannedMedicineReviewCard(
                            item = item,
                            onToggleSelect = { isChecked ->
                                viewModel.toggleSelectItem(item.id, isChecked)
                            },
                            onUpdate = { updated ->
                                viewModel.updateItem(updated)
                            },
                            onDelete = {
                                viewModel.removeItem(item.id)
                            }
                        )
                    }
                }

                // NÚT LƯU THUỐC ĐÃ CHỌN VÀO LỊCH UỐNG
                Button(
                    onClick = {
                        viewModel.saveSelectedMedicines(targetMemberId) {
                            Toast.makeText(context, "Đã lưu thành công vào lịch uống!", Toast.LENGTH_SHORT).show()
                            onBack()
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
                    enabled = selectedCount > 0 && !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Đang lưu...", fontSize = 14.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Lưu $selectedCount thuốc đã chọn vào Lịch uống",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (!uiState.isAnalyzing && uiState.scannedItems.isEmpty() && uiState.selectedImageUri == null) {
                // Hướng dẫn mở đầu
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
                            text = "Quét đơn thuốc & thực phẩm chức năng",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Chụp ảnh bao bì hộp thuốc, trà thảo dược hoặc đơn của bác sĩ. AI sẽ tự động đọc, sửa lỗi chính tả tên thuốc và bóc tách liều lượng chuẩn xác.",
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
        var addUnit by remember { mutableStateOf("gói") }
        var addNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualAddDialog = false },
            title = { Text("Thêm thuốc vào danh sách", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = addName,
                        onValueChange = { addName = it },
                        label = { Text("Tên thuốc / sản phẩm") },
                        placeholder = { Text("Ví dụ: Bột sủi thanh nhiệt Livecool") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = addDosage,
                            onValueChange = { addDosage = it },
                            label = { Text("Hàm lượng") },
                            placeholder = { Text("42.9g...") },
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
                        placeholder = { Text("Hòa vào nước uống...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (addName.isNotBlank()) {
                            viewModel.addItem(
                                ScannedMedicineItem(
                                    medicineName = addName.trim(),
                                    dosage = addDosage.trim(),
                                    unit = addUnit.trim(),
                                    times = emptyList(), // Để rỗng nếu không nhập
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
 * Thẻ xem lại thuốc bóc tách:
 * - Checkbox + Tiêu đề
 * - Ô Tên thuốc chuẩn hóa
 * - Ô Hàm lượng (ví dụ 42.9g)
 * - Ô Đơn vị dạng DropdownMenu chọn nhanh [gói], [viên], [ống], [chai], [ml]
 * - Trường Cữ giờ uống: nếu rỗng hiển thị "Chạm để thêm giờ uống"
 * - Trường Lời dặn
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
    var unit by remember(item.id) { mutableStateOf(item.unit) }
    var note by remember(item.id) { mutableStateOf(item.note) }
    val timesList = remember(item.id) { mutableStateListOf<String>().apply { addAll(item.times) } }

    var unitMenuExpanded by remember { mutableStateOf(false) }
    val unitOptions = listOf("gói", "viên", "ống", "chai", "ml", "hộp", "tuýp")

    fun notifyChange() {
        onUpdate(
            item.copy(
                medicineName = name,
                dosage = dosage,
                unit = unit,
                note = note,
                times = timesList.toList()
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
            // Hàng Checkbox + Tên mục + Nút Xóa
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
                        text = name.ifBlank { "Mục thuốc chưa đặt tên" },
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
                        contentDescription = "Xóa",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Ô Tên thuốc chuẩn hóa
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    notifyChange()
                },
                label = { Text("Tên thuốc chuẩn hóa *") },
                placeholder = { Text("Ví dụ: Bột sủi thanh nhiệt Livecool") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Ô Hàm lượng & Đơn vị (Dropdown chọn nhanh)
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
                    placeholder = { Text("42.9g, 500mg...") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                // Dropdown chọn nhanh Đơn vị
                Box(modifier = Modifier.width(105.dp)) {
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("Đơn vị") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Chọn đơn vị",
                                modifier = Modifier.clickable { unitMenuExpanded = true }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { unitMenuExpanded = true }
                    )

                    DropdownMenu(
                        expanded = unitMenuExpanded,
                        onDismissRequest = { unitMenuExpanded = false }
                    ) {
                        unitOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, fontSize = 13.sp) },
                                onClick = {
                                    unit = option
                                    unitMenuExpanded = false
                                    notifyChange()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Trường Giờ uống (Times): Nếu rỗng thì hiển thị placeholder rõ ràng "Chạm để thêm giờ uống"
            Text(
                text = "Giờ uống trong ngày:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))

            if (timesList.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Chưa có giờ uống • Chạm gợi ý bên dưới để thêm",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            } else {
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
                                Text(
                                    text = time,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E40AF)
                                )
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
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Các gợi ý thêm cữ giờ nhanh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
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

            // Ô Lời dặn (để trống nếu không có, không tự bịa)
            OutlinedTextField(
                value = note,
                onValueChange = {
                    note = it
                    notifyChange()
                },
                label = { Text("Lời dặn / Cách dùng") },
                placeholder = { Text("Ví dụ: Hòa 1 gói vào 150ml nước (để trống nếu không có)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
