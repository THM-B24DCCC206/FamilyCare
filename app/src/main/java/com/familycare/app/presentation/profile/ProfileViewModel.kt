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

        // Dọn dẹp sạch sẽ toàn bộ các ID mẫu / dữ liệu ảo cũ nếu có trong DB
        val fakeIds = listOf("user_01", "user_demo_01", "member_bac_nam", "member_bo", "member_me", "member_anhtrai", "user_02", "member_ly_tran")
        fakeIds.forEach { id ->
            runCatching { userDao.deleteUserById(id) }
        }

        // Đảm bảo tài khoản đăng nhập hiện tại có trong DB với đúng thông tin thật
        var currentDbUser = userDao.getUserById(currentUserId)
        if (currentDbUser == null) {
            val newCurr = UserEntity(
                id = currentUserId,
                fullName = currentUserName,
                email = currentUserEmail,
                role = currentRole,
                birthDate = "",
                bloodType = "",
                chronicDiseasesJson = "[]",
                drugAllergiesJson = "[]",
                gender = "",
                phoneNumber = "",
                address = ""
            )
            userDao.insertUser(newCurr)
            currentDbUser = newCurr
        }

        val currentFamilyId = currentDbUser.familyId.trim().ifBlank { sessionManager.familyId.trim() }
        val allDbUsers = userDao.getAllUsers().filter { it.id !in fakeIds }

        // Tạo danh sách Member
        val membersList = mutableListOf<Member>()
        val currentUserObj = currentDbUser.toDomainModel()

        membersList.add(
            Member(
                id = currentUserId,
                fullName = currentUserObj.fullName.ifBlank { "Tôi" },
                avatarUrl = currentUserObj.avatarUrl.takeIf { it.isNotBlank() },
                initials = (currentUserObj.fullName.ifBlank { "Tôi" }).trim().take(1).uppercase(),
                role = if (currentRole == "ADMIN" || currentDbUser.role.uppercase() == "ADMIN") "Trưởng gia đình" else "Thành viên"
            )
        )

        if (currentFamilyId.isNotBlank()) {
            allDbUsers
                .filter { it.id != currentUserId && it.familyId.isNotBlank() && it.familyId == currentFamilyId }
                .forEach { u ->
                    val displayRole = when (u.role.uppercase()) {
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
        }

        val allDomainUsers = if (currentFamilyId.isNotBlank()) {
            allDbUsers.filter { it.id == currentUserId || (it.familyId.isNotBlank() && it.familyId == currentFamilyId) }.map { it.toDomainModel() }
        } else {
            listOf(currentUserObj)
        }

        // Thành viên được chọn
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

        // Cập nhật viewingMember nếu đang xem
        val currentViewing = _uiState.value.viewingMember?.let { vm ->
            allDomainUsers.find { it.id == vm.id } ?: vm
        }

        _uiState.update { state ->
            state.copy(
                status = ProfileUiStatus.Success,
                currentUser = currentUserObj,
                familyMembers = membersList,
                allUsers = allDomainUsers,
                selectedMemberId = selectedId,
                selectedMemberUser = selectedUser,
                selectedMemberMedicines = medicinesList,
                viewingMember = currentViewing,
                isEditing = false,
                editedFullName = selectedUser.fullName,
                editedBirthDate = selectedUser.birthDate,
                editedGender = selectedUser.gender,
                editedPhoneNumber = selectedUser.phoneNumber,
                editedAddress = selectedUser.address,
                editedBloodType = selectedUser.bloodType,
                editedChronicDiseases = selectedUser.chronicDiseases,
                editedDrugAllergies = selectedUser.drugAllergies
            )
        }
    }

    /**
     * Mở màn hình Chi tiết Thành viên khi bấm vào thành viên trong danh sách gia đình
     */
    fun openMemberDetail(memberId: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val memberEntity = userDao.getUserById(memberId)
                val memberUser = memberEntity?.toDomainModel()
                val medicines = runCatching {
                    medicineDao.getMedicinesByMember(memberId).firstOrNull()?.map {
                        if (it.dosage.isNotBlank()) "${it.name} (${it.dosage})" else it.name
                    } ?: emptyList()
                }.getOrDefault(emptyList())

                _uiState.update {
                    it.copy(
                        viewingMember = memberUser,
                        viewingMemberMedicines = medicines,
                        selectedMemberId = memberId,
                        selectedMemberUser = memberUser
                    )
                }
            }
        }
    }

    fun closeMemberDetail() {
        _uiState.update { it.copy(viewingMember = null, viewingMemberMedicines = emptyList()) }
    }

    fun onMemberSelect(memberId: String) {
        if (_uiState.value.selectedMemberId == memberId) return
        openMemberDetail(memberId)
    }

    /**
     * Mở Dialog chỉnh sửa Hồ sơ cá nhân (thông tin + y bạ)
     */
    fun openEditProfileDialog(targetUser: User? = null) {
        val userToEdit = targetUser ?: _uiState.value.currentUser ?: return
        _uiState.update {
            it.copy(
                showEditProfileDialog = true,
                editedFullName = userToEdit.fullName,
                editedBirthDate = userToEdit.birthDate,
                editedGender = userToEdit.gender,
                editedPhoneNumber = userToEdit.phoneNumber,
                editedAddress = userToEdit.address,
                editedBloodType = userToEdit.bloodType,
                editedChronicDiseases = userToEdit.chronicDiseases,
                editedDrugAllergies = userToEdit.drugAllergies,
                saveSuccessMessage = null
            )
        }
    }

    fun dismissEditProfileDialog() {
        _uiState.update { it.copy(showEditProfileDialog = false) }
    }

    /**
     * Lưu hồ sơ người dùng / thành viên
     */
    fun saveProfile(
        userId: String,
        fullName: String,
        birthDate: String,
        gender: String,
        phoneNumber: String,
        address: String,
        bloodType: String,
        chronicDiseases: List<String>,
        drugAllergies: List<String>
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = userDao.getUserById(userId)
            val updatedEntity = UserEntity(
                id = userId,
                fullName = fullName.trim(),
                email = existing?.email.orEmpty(),
                role = existing?.role ?: "MEMBER",
                birthDate = birthDate.trim(),
                bloodType = bloodType.trim(),
                chronicDiseasesJson = "[${chronicDiseases.joinToString(",") { "\"$it\"" }}]",
                drugAllergiesJson = "[${drugAllergies.joinToString(",") { "\"$it\"" }}]",
                gender = gender.trim(),
                phoneNumber = phoneNumber.trim(),
                address = address.trim(),
                avatarUrl = existing?.avatarUrl.orEmpty(),
                familyId = existing?.familyId.orEmpty()
            )
            userDao.insertUser(updatedEntity)

            // Cập nhật lại session nếu là tài khoản hiện tại
            val currentUserId = sessionManager.userId.ifBlank { "user_me" }
            if (userId == currentUserId) {
                sessionManager.saveUserSession(
                    userId = currentUserId,
                    name = fullName.trim(),
                    email = sessionManager.userEmail,
                    role = sessionManager.userRole
                )
            }

            loadProfileDataInternal(targetMemberId = userId)

            _uiState.update {
                it.copy(
                    showEditProfileDialog = false,
                    showEditHealthCardDialog = false,
                    saveSuccessMessage = "Đã lưu thông tin của $fullName thành công!"
                )
            }
        }
    }

    fun openEditHealthCardDialog() {
        val u = _uiState.value.selectedMemberUser ?: _uiState.value.currentUser ?: return
        openEditProfileDialog(u)
    }

    fun dismissEditHealthCardDialog() {
        dismissEditProfileDialog()
    }

    fun openAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = true) }
    }

    fun dismissAddMemberDialog() {
        _uiState.update { it.copy(showAddMemberDialog = false) }
    }

    fun addNewMember(
        fullName: String,
        relationship: String,
        birthDate: String = "",
        gender: String = "",
        phoneNumber: String = "",
        address: String = "",
        bloodType: String = "O+",
        chronicDiseases: List<String> = emptyList(),
        drugAllergies: List<String> = emptyList()
    ) {
        val trimmedName = fullName.trim()
        if (trimmedName.isBlank()) return

        val newId = "member_${UUID.randomUUID().toString().take(8)}"
        val newMemberEntity = UserEntity(
            id = newId,
            fullName = trimmedName,
            email = "",
            role = relationship.ifBlank { "Thành viên" },
            birthDate = birthDate.trim(),
            bloodType = bloodType.trim(),
            chronicDiseasesJson = "[${chronicDiseases.joinToString(",") { "\"$it\"" }}]",
            drugAllergiesJson = "[${drugAllergies.joinToString(",") { "\"$it\"" }}]",
            gender = gender.trim(),
            phoneNumber = phoneNumber.trim(),
            address = address.trim()
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

    // Dialogs điều khiển
    fun openHealthDataDialog() { _uiState.update { it.copy(showHealthDataDialog = true) } }
    fun dismissHealthDataDialog() { _uiState.update { it.copy(showHealthDataDialog = false) } }

    fun openSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = true) } }
    fun dismissSettingsDialog() { _uiState.update { it.copy(showSettingsDialog = false) } }

    fun openHelpDialog() { _uiState.update { it.copy(showHelpDialog = true) } }
    fun dismissHelpDialog() { _uiState.update { it.copy(showHelpDialog = false) } }

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
