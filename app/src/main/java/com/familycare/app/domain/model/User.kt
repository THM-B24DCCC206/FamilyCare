package com.familycare.app.domain.model

enum class UserRole {
    ADMIN,  // Trưởng gia đình: cấu hình ngưỡng, chỉnh sửa hồ sơ
    MEMBER  // Thành viên: theo dõi, đo chỉ số
}

data class User(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val familyId: String = "",
    val role: UserRole = UserRole.MEMBER,
    val birthDate: String = "",
    val bloodType: String = "",          // A, B, AB, O
    val chronicDiseases: List<String> = emptyList(), // Bệnh nền
    val drugAllergies: List<String> = emptyList(),    // Dị ứng thuốc
    val gender: String = "",             // Nam / Nữ
    val phoneNumber: String = "",
    val address: String = ""
)

data class Family(
    val id: String = "",
    val name: String = "",
    val inviteCode: String = "", // Mã mời 6 ký tự để thành viên khác vào nhà
    val createdBy: String = "",
    val memberIds: List<String> = emptyList()
)
