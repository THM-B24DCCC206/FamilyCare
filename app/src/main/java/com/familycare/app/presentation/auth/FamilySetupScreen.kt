package com.familycare.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilySetupScreen(
    onSetupComplete: () -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var selectedMode by remember { mutableStateOf("CREATE") } // "CREATE" hoặc "JOIN"

    var familyNameInput by remember { mutableStateOf("") }
    var inviteCodeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thiết Lập Tổ Ấm Gia Đình", fontWeight = FontWeight.Bold) }
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
                        containerColor = if (selectedMode == "JOIN") MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.GroupAdd,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Nhập Mã Tham Gia",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Form tương ứng với chế độ
            if (selectedMode == "CREATE") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Đặt tên cho tổ ấm của bạn:",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = familyNameInput,
                            onValueChange = { familyNameInput = it },
                            placeholder = { Text("Ví dụ: Gia đình Bác Nam") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "✨ Hệ thống sẽ tự động tạo một Mã Mời 6 ký tự để bạn gửi cho con cháu cùng tham gia.",
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
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Nhập mã mời do người thân chia sẻ:",
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = inviteCodeInput,
                            onValueChange = { inviteCodeInput = it.uppercase() },
                            placeholder = { Text("Ví dụ: FAM-8921") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
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
                onClick = {
                    if (selectedMode == "CREATE") {
                        val name = familyNameInput.trim().ifEmpty { "Gia đình của tôi" }
                        val generatedCode = "FAM-" + Random.nextInt(1000, 9999)
                        val familyId = "fam_" + System.currentTimeMillis()

                        sessionManager.saveFamilySession(
                            familyId = familyId,
                            familyName = name,
                            inviteCode = generatedCode
                        )
                        onSetupComplete()
                    } else {
                        val code = inviteCodeInput.trim().uppercase()
                        if (code.length < 5) {
                            errorMessage = "Vui lòng nhập đúng định dạng mã mời (Ví dụ: FAM-8921)"
                            return@Button
                        }
                        val familyId = "fam_joined_" + System.currentTimeMillis()
                        sessionManager.saveFamilySession(
                            familyId = familyId,
                            familyName = "Tổ Ấm Yêu Thương",
                            inviteCode = code
                        )
                        onSetupComplete()
                    }
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(
                    text = if (selectedMode == "CREATE") "Tạo Tổ Ấm & Bắt Đầu" else "Gia Nhập & Bắt Đầu",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
