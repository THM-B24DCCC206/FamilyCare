package com.familycare.app.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.common.SessionManager
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.HealthMetricDao
import com.familycare.app.core.database.MedicineDao
import com.familycare.app.core.database.UserDao
import com.familycare.app.core.database.UserEntity
import com.familycare.app.domain.model.User
import com.familycare.app.domain.model.UserRole
import com.familycare.app.presentation.home.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * ViewModel quản lý màn hình Hồ Sơ Sức Khỏe (Profile Screen)
 * Hỗ trợ chuyển đổi xem và chỉnh sửa thông tin y tế (nhóm máu, bệnh nền, dị ứng)
 * cho từng thành viên trong gia đình và xuất báo cáo PDF tương ứng.
 */
class ProfileViewModel(
    private val userDao: UserDao,
    private val sessionManager: SessionManager,
    private val healthMetricDao: HealthMetricDao,
    private val medicineDao: MedicineDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfileData()
    }

    fun loadProfileData(targetMemberId: String? = null) {
        _uiState.update { it.copy(status = ProfileUiStatus.Loading) }
        viewModelScope.launch {
            try {
                loadProfileDataInternal(targetMemberId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        status = ProfileUiStatus.Error(
                            "Không thể tải hồ sơ: ${e.localizedMessage ?: "Vui lòng thử lại"}"
                        )
                    )
                }
            }
        }
    }

    private suspend fun loadProfileDataInternal(targetMemberId: String? = null) = withContext(Dispatchers.IO) {
        val currentUserId = sessionManager.userId.ifBlank { "user_me" }
        val currentUserName = sessionManager.userName.ifBlank { "Tôi" }
        val currentUserEmail = sessionManager.userEmail
        val currentRole = sessionManager.userRole

        // Dọn dẹp các user id giả nếu có
        runCatching {
            userDao.deleteUserById("user_01")
            userDao.deleteUserById("user_demo_01")
        }

        // Đảm bảo tài khoản đăng nhập hiện tại có trong DB
        var currentDbUser = userDao.getUserById(currentUserId)
        if (currentDbUser == null) {
            val newCurr = UserEntity(
                id = currentUserId,
                fullName = currentUserName,
                email = currentUserEmail,
                role = currentRole,
                bloodType = "O+",
                chronicDiseasesJson = "[]",
                drugAllergiesJson = "[]"
            )
            userDao.insertUser(newCurr)
            currentDbUser = newCurr
        }

        var allDbUsers = userDao.getAllUsers()

        // Nếu chưa có thành viên khác trong DB, seed mẫu các thành viên gia đình quen thuộc
        val otherMembersInDb = allDbUsers.filter { it.id != currentUserId && it.id != "user_01" && it.id != "user_demo_01" }
        if (otherMembersInDb.isEmpty()) {
            val lyTran = UserEntity(
                id = "member_ly_tran",
                fullName = "Ly Trần",
                email = "ly.tran@familycare.vn",
                role = "MEMBER",
                birthDate = "20/11/1992",
                bloodType = "A+",
                chronicDiseasesJson = "[]",
                drugAllergiesJson = "[\"Dị ứng Kháng sinh\"]"
            )
            userDao.insertUser(lyTran)

            val bacNam = UserEntity(
                id = "member_bac_nam",
                fullName = "Bác Nguyễn Văn Nam",
                email = "nam.nguyen@familycare.vn",
                role = "MEMBER",
                birthDate = "15/04/1958",
                bloodType = "O+",
                chronicDiseasesJson = "[\"Cao huyết áp\", \"Đái tháo đường type 2\"]",
                drugAllergiesJson = "[\"Dị ứng Aspirin\"]"
            )
            userDao.insertUser(bacNam)

            allDbUsers = userDao.getAllUsers()
        }

        // Tạo danh sách Member cho LazyRow: Tài khoản đăng nhập luôn đứng ĐẦU TIÊN
        val membersList = mutableListOf<Member>()
        val currentUserObj = currentDbUser.toDomainModel()

        membersList.add(
            Member(
                id = currentUserId,
                fullName = currentUserObj.fullName.ifBlank { "Tôi" },
                avatarUrl = currentUserObj.avatarUrl.takeIf { it.isNotBlank() },
                initials = "Tôi",
                role = if (currentRole == "ADMIN") "Trưởng gia đình" else "Tôi"
            )
        )

        allDbUsers
            .filter { it.id != currentUserId && it.id != "user_01" && it.id != "user_demo_01" }
            .forEach { u ->
                val displayRole = when (u.role) {
                    "ADMIN" -> "Trưởng gia đình"
                    "MEMBER" -> "Thành viên"
                    else -> u.role
                }
                membersList.add(
                    Member(
                        id = u.id,
                        fullName = u.fullName,
                        avatarUrl = u.avatarUrl.takeIf { it.isNotBlank() },
                        initials = u.fullName.trim().take(1).uppercase(),
                        role = displayRole
                    )
                )
            }

        // Xác định thành viên được chọn
        val selectedId = when {
            targetMemberId != null && membersList.any { it.id == targetMemberId } -> targetMemberId
            _uiState.value.selectedMemberId.isNotBlank() && membersList.any { it.id == _uiState.value.selectedMemberId } -> _uiState.value.selectedMemberId
            else -> currentUserId
        }

        val selectedEntity = userDao.getUserById(selectedId) ?: currentDbUser
        val selectedUser = selectedEntity.toDomainModel()

        // Lấy danh sách thuốc của thành viên được chọn
        val medicinesList = runCatching {
            medicineDao.getMedicinesByMember(selectedId).firstOrNull()?.map {
                if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name
            } ?: emptyList()
        }.getOrDefault(emptyList())

        _uiState.update { state ->
            state.copy(
                status = ProfileUiStatus.Success,
                currentUser = currentUserObj,
                familyMembers = membersList,
                selectedMemberId = selectedId,
                selectedMemberUser = selectedUser,
                selectedMemberMedicines = medicinesList,
                isEditing = false,
                editedFullName = selectedUser.fullName,
                editedBirthDate = selectedUser.birthDate,
                editedBloodType = selectedUser.bloodType,
                editedChronicDiseases = selectedUser.chronicDiseases,
                editedDrugAllergies = selectedUser.drugAllergies
            )
        }
    }

    /**
     * Chuyển đổi thành viên được chọn từ thanh avatar LazyRow
     */
    fun onMemberSelect(memberId: String) {
        if (_uiState.value.selectedMemberId == memberId) return

        viewModelScope.launch {
            _uiState.update { it.copy(selectedMemberId = memberId) }
            withContext(Dispatchers.IO) {
                val memberEntity = userDao.getUserById(memberId)
                val memberUser = memberEntity?.toDomainModel()
                val medicinesList = runCatching {
                    medicineDao.getMedicinesByMember(memberId).firstOrNull()?.map {
                        if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name
                    } ?: emptyList()
                }.getOrDefault(emptyList())

                _uiState.update { state ->
                    state.copy(
                        selectedMemberUser = memberUser,
                        selectedMemberMedicines = medicinesList,
                        isEditing = false,
                        showEditHealthCardDialog = false,
                        editedFullName = memberUser?.fullName.orEmpty(),
                        editedBirthDate = memberUser?.birthDate.orEmpty(),
                        editedBloodType = memberUser?.bloodType.orEmpty(),
                        editedChronicDiseases = memberUser?.chronicDiseases.orEmpty(),
                        editedDrugAllergies = memberUser?.drugAllergies.orEmpty()
                    )
                }
            }
        }
    }

    /**
     * Mở Dialog chỉnh sửa Y bạ cho thành viên đang chọn
     */
    fun openEditHealthCardDialog() {
        val u = _uiState.value.selectedMemberUser ?: return
        _uiState.update {
            it.copy(
                showEditHealthCardDialog = true,
                editedBloodType = u.bloodType,
                editedChronicDiseases = u.chronicDiseases,
                editedDrugAllergies = u.drugAllergies,
                saveSuccessMessage = null
            )
        }
    }

    fun dismissEditHealthCardDialog() {
        _uiState.update { it.copy(showEditHealthCardDialog = false) }
    }

    /**
     * Mở Dialog thêm thành viên mới
     */
    fun openAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = true) }
    }

    fun dismissAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = false) }
    }

    /**
     * Lưu thông tin Y bạ (nhóm máu, bệnh nền, dị ứng) cho thành viên đang chọn
     */
    fun saveHealthCard(
        bloodType: String,
        chronicDiseases: List<String>,
        drugAllergies: List<String>
    ) {
        val currentMember = _uiState.value.selectedMemberUser ?: return
        val updated = currentMember.copy(
            bloodType = bloodType.trim(),
            chronicDiseases = chronicDiseases,
            drugAllergies = drugAllergies
        )

        viewModelScope.launch(Dispatchers.IO) {
            userDao.insertUser(UserEntity.fromDomainModel(updated))
            _uiState.update { state ->
                state.copy(
                    selectedMemberUser = updated,
                    showEditHealthCardDialog = false,
                    saveSuccessMessage = "Đã cập nhật thông tin y tế cho ${updated.fullName}!"
                )
            }
        }
    }

    /**
     * Thêm thành viên mới vào gia đình
     */
    fun addNewMember(
        fullName: String,
        relationship: String,
        bloodType: String,
        chronicDiseases: List<String>,
        drugAllergies: List<String>
    ) {
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) return

        val newId = "member_${UUID.randomUUID().toString().take(8)}"
        val newMemberEntity = UserEntity(
            id = newId,
            fullName = trimmedName,
            email = "",
            role = relationship.ifBlank { "Thành viên" },
            bloodType = bloodType.trim(),
            chronicDiseasesJson = "[${chronicDiseases.joinToString(",") { "\"$it\"" }}]",
            drugAllergiesJson = "[${drugAllergies.joinToString(",") { "\"$it\"" }}]"
        )

        viewModelScope.launch(Dispatchers.IO) {
            userDao.insertUser(newMemberEntity)
            loadProfileDataInternal(targetMemberId = newId)
            _uiState.update { state ->
                state.copy(
                    showAddMemberDialog = false,
                    saveSuccessMessage = "Đã thêm thành viên $trimmedName vào gia đình!"
                )
            }
        }
    }

    fun clearSaveMessage() {
        _uiState.update { it.copy(saveSuccessMessage = null) }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                val sessionManager = SessionManager(context.applicationContext)
                return ProfileViewModel(
                    userDao = db.userDao(),
                    sessionManager = sessionManager,
                    healthMetricDao = db.healthMetricDao(),
                    medicineDao = db.medicineDao()
                ) as T
            }
        }
    }
}
