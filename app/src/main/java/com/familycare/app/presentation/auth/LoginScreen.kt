package com.familycare.app.presentation.auth

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.familycare.app.R
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.UserAccountEntity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun LoginScreen(
    onLoginSuccess: (hasFamily: Boolean) -> Unit
) {
    val context = LocalContext.current
    val database = remember { AppDatabase.getInstance(context) }
    val sessionManager = remember { SessionManager(context) }
    val scope = rememberCoroutineScope()

    var isRegisterMode by remember { mutableStateOf(false) }

    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    // Cấu hình Google Sign-In Client
    val webClientId = "129696848056-jvhpo503nh309tuivpr7nlj5r4e37ng8.apps.googleusercontent.com"
    val googleSignInClient = remember {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    // Launcher đón kết quả chọn tài khoản Google
    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val googleEmail = account.email?.trim()?.lowercase() ?: ""
            val googleName = account.displayName?.trim()?.ifEmpty { "Người dùng Google" } ?: "Người dùng Google"
            val googleId = account.id ?: UUID.randomUUID().toString()

            if (googleEmail.isNotBlank()) {
                isLoading = true
                errorMessage = null
                scope.launch {
                    try {
                        // 1. Kiểm tra tài khoản trong CSDL SQLite
                        var user = database.userDao.getUserByEmail(googleEmail)
                        if (user == null) {
                            // Tài khoản Google lần đầu vào app -> Tự động đăng ký vào CSDL
                            val newUserId = "user_gg_" + googleId
                            val newUser = UserAccountEntity(
                                id = newUserId,
                                email = googleEmail,
                                password = "google_oauth_linked",
                                fullName = googleName,
                                role = "MEMBER",
                                familyId = null,
                                familyName = null,
                                inviteCode = null
                            )
                            database.userDao.register(newUser)
                            user = newUser
                        }

                        // 2. Lưu phiên làm việc
                        sessionManager.saveUserSession(
                            userId = user.id,
                            name = user.fullName,
                            email = user.email,
                            role = user.role,
                            familyId = user.familyId,
                            familyName = user.familyName,
                            inviteCode = user.inviteCode
                        )

                        // 3. Điều hướng: Nếu đã có tổ ấm thì vào Dashboard, nếu mới thì thiết lập tổ ấm
                        val hasFamily = !user.familyId.isNullOrEmpty()
                        onLoginSuccess(hasFamily)
                    } catch (e: Exception) {
                        errorMessage = "Lỗi xử lý tài khoản Google: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            }
        } catch (e: ApiException) {
            // 12501 = người dùng bấm hủy ra ngoài
            if (e.statusCode != 12501 && e.statusCode != 16) {
                errorMessage = "Đăng nhập Google thất bại (Mã lỗi: ${e.statusCode})"
            }
        } catch (e: Exception) {
            errorMessage = "Có lỗi xảy ra: ${e.localizedMessage}"
        }
    }

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
                placeholder = { Text("VD: Nguyễn Văn Nam") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = { Text("Email") },
            placeholder = { Text("example@gmail.com") },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
            },
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

        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Nút Đăng Nhập / Đăng Ký Email
        Button(
            enabled = !isLoading,
            onClick = {
                val cleanEmail = email.trim().lowercase()
                val cleanPassword = password.trim()
                val cleanFullName = fullName.trim()

                // Kiểm tra cú pháp cơ bản
                if (isRegisterMode && cleanFullName.isBlank()) {
                    errorMessage = "Vui lòng nhập họ và tên của bạn"
                    return@Button
                }
                if (cleanEmail.isBlank() || !cleanEmail.contains("@")) {
                    errorMessage = "Vui lòng nhập địa chỉ email hợp lệ"
                    return@Button
                }
                if (cleanPassword.length < 6) {
                    errorMessage = "Mật khẩu phải có ít nhất 6 ký tự"
                    return@Button
                }

                isLoading = true
                errorMessage = null

                scope.launch {
                    try {
                        if (isRegisterMode) {
                            // --- LUỒNG ĐĂNG KÝ EMAIL ---
                            val existingUser = database.userDao.getUserByEmail(cleanEmail)
                            if (existingUser != null) {
                                errorMessage = "Email này đã được đăng ký tài khoản. Vui lòng chuyển sang tab Đăng Nhập!"
                                isLoading = false
                                return@launch
                            }

                            val newUserId = "user_" + UUID.randomUUID().toString()
                            val newUser = UserAccountEntity(
                                id = newUserId,
                                email = cleanEmail,
                                password = cleanPassword,
                                fullName = cleanFullName,
                                role = "MEMBER",
                                familyId = null,
                                familyName = null,
                                inviteCode = null
                            )

                            val success = database.userDao.register(newUser)
                            if (success) {
                                sessionManager.saveUserSession(
                                    userId = newUserId,
                                    name = cleanFullName,
                                    email = cleanEmail,
                                    role = "MEMBER",
                                    familyId = null,
                                    familyName = null,
                                    inviteCode = null
                                )
                                onLoginSuccess(false) // Người mới đăng ký -> Vào thiết lập tổ ấm
                            } else {
                                errorMessage = "Đăng ký không thành công. Vui lòng thử lại!"
                            }
                        } else {
                            // --- LUỒNG ĐĂNG NHẬP EMAIL (KIỂM TRA CHẶT CHẼ) ---
                            val user = database.userDao.getUserByEmail(cleanEmail)
                            if (user == null) {
                                errorMessage = "Tài khoản không tồn tại. Vui lòng bấm sang tab Đăng Ký trước!"
                                isLoading = false
                                return@launch
                            }

                            if (user.password != cleanPassword) {
                                errorMessage = "Mật khẩu không chính xác. Vui lòng thử lại!"
                                isLoading = false
                                return@launch
                            }

                            // Đăng nhập thành công -> Nạp đúng tổ ấm cũ của người này
                            sessionManager.saveUserSession(
                                userId = user.id,
                                name = user.fullName,
                                email = user.email,
                                role = user.role,
                                familyId = user.familyId,
                                familyName = user.familyName,
                                inviteCode = user.inviteCode
                            )

                            val hasFamily = !user.familyId.isNullOrEmpty()
                            onLoginSuccess(hasFamily)
                        }
                    } catch (e: Exception) {
                        errorMessage = "Có lỗi xảy ra: ${e.localizedMessage}"
                    } finally {
                        isLoading = false
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = if (isRegisterMode) "Đăng Ký Tài Khoản" else "Đăng Nhập",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dải phân cách "HOẶC"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f))
            Text(
                text = "HOẶC",
                modifier = Modifier.padding(horizontal = 12.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalDivider(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(18.dp))

        // 5. NÚT ĐĂNG NHẬP BẰNG GOOGLE
        OutlinedButton(
            enabled = !isLoading,
            onClick = {
                // Đăng xuất client trước để luôn hiện bảng chọn tài khoản
                googleSignInClient.signOut().addOnCompleteListener {
                    googleLauncher.launch(googleSignInClient.signInIntent)
                }
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Google Icon",
                tint = Color(0xFF4285F4),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Tiếp tục với Google",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
