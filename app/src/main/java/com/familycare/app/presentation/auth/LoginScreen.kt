package com.familycare.app.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familycare.app.core.common.SessionManager

@Composable
fun LoginScreen(
    onLoginSuccess: (hasFamily: Boolean) -> Unit
) {
    val context = LocalContext.current
    val sessionManager = remember { SessionManager(context) }

    var isRegisterMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf("ADMIN") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 1. Logo & Tên Ứng Dụng
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(80.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFE91E63),
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "FamilyCare",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "Hệ thống chăm sóc & bảo vệ sức khỏe gia đình",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2. Tab Chuyển đổi Đăng nhập / Đăng ký
        TabRow(
            selectedTabIndex = if (isRegisterMode) 1 else 0,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = !isRegisterMode,
                onClick = {
                    isRegisterMode = false
                    errorMessage = null
                },
                text = { Text("Đăng Nhập", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = isRegisterMode,
                onClick = {
                    isRegisterMode = true
                    errorMessage = null
                },
                text = { Text("Đăng Ký", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. Form Nhập Liệu
        if (isRegisterMode) {
            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Họ và Tên") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = null
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (isRegisterMode) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Vai trò trong gia đình:",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRole == "ADMIN",
                    onClick = { selectedRole = "ADMIN" },
                    label = { Text("Trưởng gia đình") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedRole == "MEMBER",
                    onClick = { selectedRole = "MEMBER" },
                    label = { Text("Thành viên") },
                    modifier = Modifier.weight(1f)
                )
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

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Nút Hành Động Chính
        Button(
            onClick = {
                // Kiểm tra hợp lệ
                if (isRegisterMode && fullName.isBlank()) {
                    errorMessage = "Vui lòng nhập họ và tên"
                    return@Button
                }
                if (email.isBlank() || !email.contains("@")) {
                    errorMessage = "Vui lòng nhập địa chỉ email hợp lệ"
                    return@Button
                }
                if (password.length < 6) {
                    errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                    return@Button
                }

                // Lưu session đăng nhập
                val nameToSave = if (isRegisterMode) fullName else "Nguyễn Văn Nam"
                val userId = "user_" + System.currentTimeMillis()
                sessionManager.saveUserSession(
                    userId = userId,
                    name = nameToSave,
                    email = email,
                    role = selectedRole
                )

                onLoginSuccess(sessionManager.hasFamily)
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text(
                text = if (isRegisterMode) "Đăng Ký Tài Khoản" else "Đăng Nhập",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
