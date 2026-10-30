package com.familycare.app.presentation.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySetupScreen(
    onSetupComplete: () -> Unit,
    onBackToLogin: () -> Unit = {}
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    // Bắt sự kiện phím Back cứng / cử chỉ vuốt của Android
    BackHandler {
        sessionManager.clearSession()
        onBackToLogin()
    }

    var selectedMode by remember { mutableStateOf("CREATE") } // "CREATE" hoặc "JOIN"

    var familyNameInput by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thiết Lập Tổ Ấm", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        sessionManager.clearSession()
                        onBackToLogin()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại màn hình đăng nhập"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Kết Nối Các Thành Viên",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tạo một không gian gia đình mới hoặc nhập mã mời để cùng theo dõi sức khỏe người thân",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2 Thẻ chọn chế độ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    onClick = {
                        selectedMode = "CREATE"
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedMode == "CREATE") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.AddHome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tạo Tổ Ấm Mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedCard(
                    onClick = {
                        selectedMode = "JOIN"
                        errorMessage = null
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = if (selectedMode == "JOIN") MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhập Mã Gia Đình",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nội dung theo chế độ
            if (selectedMode == "CREATE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Đặt tên cho tổ ấm của bạn",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = familyNameInput,
                            onValueChange = { familyNameInput = it },
                            label = { Text("Tên gia đình") },
                            placeholder = { Text("VD: Gia đình Ly Trần") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hệ thống sẽ tự động tạo một Mã mời 6 ký tự để bạn gửi cho con cháu tham gia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Nhập mã mời từ người thân",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            label = { Text("Mã mời gia đình") },
                            placeholder = { Text("VD: FAM-8921") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Hỏi người tạo gia đình mã mời (có định dạng FAM-XXXX) để cùng vào chung một nhà.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            errorMessage?.let { error ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Button(
                enabled = !isSubmitting,
                onClick = {
                    isSubmitting = true
                    errorMessage = null

                    scope.launch {
                        try {
                            if (selectedMode == "CREATE") {
                                val name = familyNameInput.trim().ifEmpty { "Gia đình của tôi" }
                                val generatedCode = "FAM-" + Random.nextInt(1000, 9999)
                                val familyId = "fam_" + System.currentTimeMillis()

                                // 1. Lưu vào Session (TỰ ĐỘNG PHONG VAI TRÒ: TRƯỞNG GIA ĐÌNH)
                                sessionManager.saveUserSession(
                                    userId = sessionManager.userId,
                                    name = sessionManager.userName,
                                    email = sessionManager.userEmail,
                                    role = "ADMIN",
                                    familyId = familyId,
                                    familyName = name,
                                    inviteCode = generatedCode
                                )

                                // 2. LƯU VĨNH VIỄN VÀO TÀI KHOẢN NGƯỜI DÙNG TRONG SQLITE
                                if (sessionManager.userId.isNotBlank()) {
                                    database.userDao.updateFamily(
                                        userId = sessionManager.userId,
                                        familyId = familyId,
                                        familyName = name,
                                        inviteCode = generatedCode,
                                        role = "ADMIN"
                                    )
                                }

                                onSetupComplete()
                            } else {
                                val code = inviteCodeInput.trim().uppercase()
                                if (code.length < 5) {
                                    errorMessage = "Vui lòng nhập đúng định dạng mã mời (Ví dụ: FAM-8921)"
                                    isSubmitting = false
                                    return@launch
                                }
                                val targetFamily = database.userDao.getFamilyByInviteCode(code)
                                if (targetFamily == null) {
                                    errorMessage = "Không tìm thấy tổ ấm nào với mã mời '$code'. Vui lòng kiểm tra lại!"
                                    isSubmitting = false
                                    return@launch
                                }
                                val familyId = targetFamily.familyId
                                val familyName = targetFamily.familyName

                                // 1. Lưu vào Session (TỰ ĐỘNG GÁN VAI TRÒ: THÀNH VIÊN)
                                sessionManager.saveUserSession(
                                    userId = sessionManager.userId,
                                    name = sessionManager.userName,
                                    email = sessionManager.userEmail,
                                    role = "MEMBER",
                                    familyId = familyId,
                                    familyName = familyName,
                                    inviteCode = code
                                )

                                // 2. LƯU VĨNH VIỄN VÀO TÀI KHOẢN NGƯỜI DÙNG TRONG SQLITE
                                if (sessionManager.userId.isNotBlank()) {
                                    database.userDao.updateFamily(
                                        userId = sessionManager.userId,
                                        familyId = familyId,
                                        familyName = familyName,
                                        inviteCode = code,
                                        role = "MEMBER"
                                    )
                                }

                                onSetupComplete()
                            }
                        } catch (e: Exception) {
                            errorMessage = "Có lỗi xảy ra: ${e.localizedMessage}"
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (selectedMode == "CREATE") "Tạo Tổ Ấm & Bắt Đầu" else "Gia Nhập & Bắt Đầu",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
