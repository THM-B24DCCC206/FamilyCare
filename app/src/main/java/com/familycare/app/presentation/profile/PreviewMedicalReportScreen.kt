package com.familycare.app.presentation.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricEntity
import com.familycare.app.core.database.MedicineEntity
import com.familycare.app.domain.model.User
import com.familycare.app.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Màn hình Xem Trước Báo Cáo Y Tế (PreviewMedicalReportScreen)
 * Hiển thị đầy đủ thông tin y bạ, lịch uống thuốc và chỉ số sinh tồn của thành viên được chọn.
 * Hỗ trợ xuất và chia sẻ file PDF chuẩn y khoa.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PreviewMedicalReportScreen(
    memberId: String = "",
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()
    val locale = remember { Locale.forLanguageTag("vi-VN") }

    val targetId = remember(memberId) {
        memberId.ifBlank { sessionManager.userId.ifBlank { "user_me" } }
    }

    var memberUser by remember { mutableStateOf<User?>(null) }
    var medicines by remember { mutableStateOf<List<MedicineEntity>>(emptyList()) }
    var allMetrics by remember { mutableStateOf<List<HealthMetricEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Quản lý tháng được chọn để xem báo cáo (Mặc định là tháng hiện tại)
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val selectedMonthText = remember(selectedCalendar) {
        SimpleDateFormat("MM/yyyy", locale).format(selectedCalendar.time)
    }

    // Tính mốc đầu và cuối tháng được chọn
    val monthStart = remember(selectedCalendar) {
        (selectedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val nextMonth = remember(selectedCalendar) {
        (selectedCalendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    // Lọc chỉ số sức khỏe theo tháng đang chọn
    val monthlyMetrics = remember(allMetrics, monthStart, nextMonth) {
        allMetrics.filter { it.timestamp in monthStart until nextMonth }
            .sortedByDescending { it.timestamp }
    }

    // Tải dữ liệu thành viên, đơn thuốc và chỉ số
    LaunchedEffect(targetId) {
        isLoading = true
        withContext(Dispatchers.IO) {
            val db = AppDatabase.getInstance(context)
            val u = db.userDao.getUserById(targetId)
            memberUser = u?.toDomainModel() ?: User(
                id = targetId,
                fullName = sessionManager.userName.ifBlank { "Tôi" },
                email = sessionManager.userEmail,
                role = UserRole.MEMBER
            )
            medicines = db.medicineDao.getMedicinesByMember(targetId).firstOrNull() ?: emptyList()
            allMetrics = db.healthMetricDao.getMetricsByMember(targetId).firstOrNull() ?: emptyList()
        }
        isLoading = false
    }

    // Launcher lưu file PDF vào máy (SAF)
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        val memberName = memberUser?.fullName.orEmpty().ifBlank { sessionManager.userName }
                        val memberRole = memberUser?.role?.name.orEmpty()
                        val bloodType = memberUser?.bloodType.orEmpty()
                        val chronicDiseases = memberUser?.chronicDiseases.orEmpty()
                        val drugAllergies = memberUser?.drugAllergies.orEmpty()
                        val medicineStrings = medicines.map {
                            if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name
                        }

                        HealthReportPdfExporter.export(
                            context = context,
                            uri = uri,
                            metrics = monthlyMetrics,
                            memberName = memberName,
                            memberRole = memberRole,
                            bloodType = bloodType,
                            chronicDiseases = chronicDiseases,
                            drugAllergies = drugAllergies,
                            medicines = medicineStrings
                        )
                    }
                    Toast.makeText(context, "Đã lưu báo cáo PDF thành công!", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi khi lưu PDF: ${e.localizedMessage ?: "Thất bại"}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Hàm xuất PDF vào cache và kích hoạt Intent chia sẻ
    fun shareReport() {
        scope.launch {
            try {
                val memberName = memberUser?.fullName.orEmpty().ifBlank { sessionManager.userName }
                val sanitizedName = memberName.replace("\\s+".toRegex(), "_")
                val cleanMonth = selectedMonthText.replace("/", "_")

                val file = withContext(Dispatchers.IO) {
                    val reportsDir = File(context.cacheDir, "medical_reports")
                    if (!reportsDir.exists()) reportsDir.mkdirs()
                    val targetFile = File(reportsDir, "Bao_cao_y_te_${sanitizedName}_$cleanMonth.pdf")
                    val uri = Uri.fromFile(targetFile)

                    HealthReportPdfExporter.export(
                        context = context,
                        uri = uri,
                        metrics = monthlyMetrics,
                        memberName = memberName,
                        memberRole = memberUser?.role?.name.orEmpty(),
                        bloodType = memberUser?.bloodType.orEmpty(),
                        chronicDiseases = memberUser?.chronicDiseases.orEmpty(),
                        drugAllergies = memberUser?.drugAllergies.orEmpty(),
                        medicines = medicines.map {
                            if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name
                        }
                    )
                    targetFile
                }

                if (file.exists()) {
                    val contentUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, contentUri)
                        putExtra(Intent.EXTRA_SUBJECT, "Báo cáo y tế: $memberName ($selectedMonthText)")
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Kính gửi bác sĩ / người thân báo cáo y tế và chỉ số sức khỏe của $memberName trong tháng $selectedMonthText."
                        )
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ báo cáo y tế qua"))
                } else {
                    Toast.makeText(context, "Không thể tạo file báo cáo để chia sẻ", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Lỗi chia sẻ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val displayMemberName = memberUser?.fullName.orEmpty().ifBlank { sessionManager.userName }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", locale) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Xem Trước Báo Cáo Y Tế",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF1E293B),
                        maxLines = 1,
                        softWrap = false
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = Color(0xFF1E293B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            // THANH HÀNH ĐỘNG ĐÁY CỐ ĐỊNH (BOTTOM ACTION BAR)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Nút chính: Tải PDF (Chỉ 1 icon duy nhất, không lặp emoji)
                    Button(
                        onClick = {
                            val sanitizedName = displayMemberName.replace("\\s+".toRegex(), "_")
                            val cleanMonth = selectedMonthText.replace("/", "_")
                            pdfLauncher.launch("Bao_cao_y_te_${sanitizedName}_$cleanMonth.pdf")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Tải Về Báo Cáo PDF",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    // Nút phụ: Chia sẻ Báo Cáo (Chỉ 1 icon duy nhất, không lặp emoji)
                    OutlinedButton(
                        onClick = { shareReport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, Color(0xFF2563EB)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF2563EB)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Chia Sẻ Báo Cáo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF2563EB))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. THẺ TIÊU ĐỀ BÁO CÁO (REPORT IDENTITY HEADER)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "BÁO CÁO Y TẾ CÁ NHÂN",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E40AF),
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Họ và tên: ",
                                fontSize = 14.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = displayMemberName,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Tháng báo cáo (Có thể bấm để chọn tháng trước)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Tháng báo cáo: ",
                                fontSize = 14.sp,
                                color = Color(0xFF475569),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFDBEAFE),
                                border = BorderStroke(1.dp, Color(0xFF93C5FD)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showMonthPicker = true }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Chọn tháng",
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF1D4ED8)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = selectedMonthText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1D4ED8),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFF1D4ED8)
                                    )
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Thời điểm lập: ",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = dateFormat.format(Date()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 2. THẺ THÔNG TIN Y BẠ & BỆNH NỀN
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Thông Tin Y Bạ & Bệnh Nền",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            softWrap = false
                        )

                        // Nhóm máu
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.WaterDrop,
                                    contentDescription = null,
                                    tint = Color(0xFFE11D48),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Nhóm máu",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            val blood = memberUser?.bloodType?.ifBlank { "Chưa xác định" } ?: "Chưa xác định"
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFFF1F2)
                            ) {
                                Text(
                                    text = blood,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFBE123C),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Bệnh nền
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Bệnh nền mãn tính:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                            val diseases = memberUser?.chronicDiseases.orEmpty()
                            if (diseases.isEmpty()) {
                                Text(
                                    text = "Chưa ghi nhận bệnh nền",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    diseases.forEach { d ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEFF6FF),
                                            border = BorderStroke(1.dp, Color(0xFFBFDBFE))
                                        ) {
                                            Text(
                                                text = d,
                                                fontSize = 12.sp,
                                                color = Color(0xFF1D4ED8),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Dị ứng
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Dị ứng thuốc & thức ăn:",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155),
                                maxLines = 1,
                                softWrap = false
                            )
                            val allergies = memberUser?.drugAllergies.orEmpty()
                            if (allergies.isEmpty()) {
                                Text(
                                    text = "Chưa ghi nhận dị ứng",
                                    fontSize = 13.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            } else {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    allergies.forEach { a ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFFEF2F2),
                                            border = BorderStroke(1.dp, Color(0xFFFECACA))
                                        ) {
                                            Text(
                                                text = a,
                                                fontSize = 12.sp,
                                                color = Color(0xFFB91C1C),
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. THẺ LỊCH UỐNG THUỐC ĐANG DÙNG (CHUẨN HÓA ICON THEO LOẠI THUỐC)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Lịch Uống Thuốc Đang Dùng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B),
                            maxLines = 1,
                            softWrap = false
                        )

                        if (medicines.isEmpty()) {
                            Text(
                                text = "Chưa có danh sách thuốc đang sử dụng",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                softWrap = false
                            )
                        } else {
                            medicines.forEachIndexed { index, med ->
                                val isLiquidOrPowder = isLiquidOrPowderMedicine(med)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Icon đổi theo dạng thuốc (Ly nước / Gói hòa tan hoặc Viên thuốc)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(if (isLiquidOrPowder) Color(0xFFE0F2FE) else Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isLiquidOrPowder) {
                                            Icon(
                                                imageVector = Icons.Default.LocalDrink,
                                                contentDescription = "Thuốc gói/hòa tan",
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Default.Medication,
                                                contentDescription = "Thuốc viên",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = med.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (med.purpose.isNotBlank()) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF1F5F9)
                                                ) {
                                                    Text(
                                                        text = med.purpose,
                                                        fontSize = 10.sp,
                                                        color = Color(0xFF475569),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        val subDetails = buildString {
                                            if (med.dosage.isNotBlank()) append("Liều: ${med.dosage}")
                                            if (med.reminderTimes.isNotEmpty()) {
                                                if (isNotEmpty()) append(" • ")
                                                append("Cữ: ${med.reminderTimes.joinToString(", ")}")
                                            }
                                        }
                                        if (subDetails.isNotBlank()) {
                                            Text(
                                                text = subDetails,
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                if (index < medicines.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF8FAFC))
                                }
                            }
                        }
                    }
                }

                // 4. THẺ CHỈ SỐ SINH TỒN TRONG THÁNG
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(1.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chỉ Số Sinh Tồn ($selectedMonthText)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "${monthlyMetrics.size} lần đo",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (monthlyMetrics.isEmpty()) {
                            Text(
                                text = "Chưa có lượt đo sinh tồn nào trong tháng $selectedMonthText.",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            monthlyMetrics.forEachIndexed { index, metric ->
                                val typeName = when (metric.type) {
                                    "BLOOD_PRESSURE" -> "Huyết áp"
                                    "HEART_RATE" -> "Nhịp tim"
                                    "BLOOD_SUGAR" -> "Đường huyết"
                                    "TEMPERATURE" -> "Nhiệt độ"
                                    else -> metric.type
                                }
                                val value = if (metric.valueSecondary != null) {
                                    "${metric.valuePrimary.toInt()}/${metric.valueSecondary.toInt()} ${metric.unit}"
                                } else {
                                    "${metric.valuePrimary.toInt()} ${metric.unit}"
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f, fill = false)) {
                                        Text(
                                            text = "${index + 1}. $typeName",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = dateFormat.format(Date(metric.timestamp)),
                                            fontSize = 11.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Text(
                                        text = value,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF2563EB),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }

                                if (index < monthlyMetrics.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF8FAFC))
                                }
                            }
                        }
                    }
                }

                // Ghi chú y tế & khoảng đệm đáy để không bị BottomBar che khuất
                Text(
                    text = "Lưu ý: Báo cáo tổng hợp dữ liệu cá nhân trên ứng dụng FamilyCare, không thay thế chẩn đoán chuyên môn của bác sĩ.",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // HỘP THOẠI CHỌN THÁNG XEM BÁO CÁO (MONTH PICKER DIALOG)
    if (showMonthPicker) {
        MonthYearPickerDialog(
            currentCalendar = selectedCalendar,
            onDismiss = { showMonthPicker = false },
            onSelect = { newCal ->
                selectedCalendar = newCal
                showMonthPicker = false
            }
        )
    }
}

/**
 * Kiểm tra xem thuốc có phải là dạng gói/bột sủi/hòa tan thay vì dạng viên không
 */
private fun isLiquidOrPowderMedicine(med: MedicineEntity): Boolean {
    val checkStr = "${med.name} ${med.dosage} ${med.instruction} ${med.purpose}".lowercase(Locale.ROOT)
    return checkStr.contains("gói") ||
           checkStr.contains("sủi") ||
           checkStr.contains("bột") ||
           checkStr.contains("cốm") ||
           checkStr.contains("thanh nhiệt") ||
           checkStr.contains("dung dịch") ||
           checkStr.contains("siro") ||
           checkStr.contains("nước") ||
           checkStr.contains("ống") ||
           checkStr.contains("ml")
}

/**
 * Hộp thoại cho phép chọn nhanh các tháng trước để xem báo cáo
 */
@Composable
private fun MonthYearPickerDialog(
    currentCalendar: Calendar,
    onDismiss: () -> Unit,
    onSelect: (Calendar) -> Unit
) {
    val now = Calendar.getInstance()
    val monthsList = remember {
        val list = mutableListOf<Calendar>()
        for (i in 0 until 12) {
            val c = (now.clone() as Calendar).apply {
                add(Calendar.MONTH, -i)
            }
            list.add(c)
        }
        list
    }

    val currentYear = currentCalendar.get(Calendar.YEAR)
    val currentMonth = currentCalendar.get(Calendar.MONTH)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chọn Tháng Báo Cáo",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                softWrap = false
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Dữ liệu chỉ số sức khỏe sẽ được lọc tự động theo tháng được chọn:",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().height(220.dp)
                ) {
                    items(monthsList) { cal ->
                        val isSelected = cal.get(Calendar.YEAR) == currentYear && cal.get(Calendar.MONTH) == currentMonth
                        val monthLabel = SimpleDateFormat("MM/yyyy", Locale.ROOT).format(cal.time)

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFF2563EB) else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelect(cal) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = monthLabel,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else Color(0xFF1E293B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}
