package com.familycare.app.presentation.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.familycare.app.core.database.AppDatabase
import com.familycare.app.core.database.dao.UserDao
import com.familycare.app.core.database.entity.UserEntity
import com.familycare.app.domain.model.User
import com.familycare.app.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel quản lý màn hình Hồ sơ y tế cá nhân (Profile Screen)
 * Hỗ trợ xem và chỉnh sửa thông tin bệnh nền, nhóm máu, dị ứng thuốc và lưu vào Room Database.
 */
class ProfileViewModel(
    private val userDao: UserDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        _uiState.update { it.copy(status = ProfileUiStatus.Loading) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val userEntity = userDao.getCurrentUser() ?: seedDefaultUser()
                    val userDomain = userEntity.toDomainModel()

                    _uiState.update { state ->
                        state.copy(
                            status = ProfileUiStatus.Success,
                            user = userDomain,
                            isEditing = false,
                            editedFullName = userDomain.fullName,
                            editedBirthDate = userDomain.birthDate,
                            editedBloodType = userDomain.bloodType,
                            editedChronicDiseases = userDomain.chronicDiseases,
                            editedDrugAllergies = userDomain.drugAllergies
                        )
                    }
                }
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

    fun startEditing() {
        val u = _uiState.value.user ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editedFullName = u.fullName,
                editedBirthDate = u.birthDate,
                editedBloodType = u.bloodType,
                editedChronicDiseases = u.chronicDiseases,
                editedDrugAllergies = u.drugAllergies,
                saveSuccessMessage = null
            )
        }
    }

    fun cancelEditing() {
        val u = _uiState.value.user ?: return
        _uiState.update {
            it.copy(
                isEditing = false,
                editedFullName = u.fullName,
                editedBirthDate = u.birthDate,
                editedBloodType = u.bloodType,
                editedChronicDiseases = u.chronicDiseases,
                editedDrugAllergies = u.drugAllergies,
                saveSuccessMessage = null
            )
        }
    }

    fun onFullNameChange(name: String) = _uiState.update { it.copy(editedFullName = name) }
    fun onBirthDateChange(dob: String) = _uiState.update { it.copy(editedBirthDate = dob) }
    fun onBloodTypeChange(type: String) = _uiState.update { it.copy(editedBloodType = type) }

    fun addDisease(disease: String) {
        val trimmed = disease.trim()
        if (trimmed.isNotBlank() && !_uiState.value.editedChronicDiseases.contains(trimmed)) {
            _uiState.update {
                it.copy(editedChronicDiseases = it.editedChronicDiseases + trimmed)
            }
        }
    }

    fun removeDisease(disease: String) {
        _uiState.update {
            it.copy(editedChronicDiseases = it.editedChronicDiseases.filter { item -> item != disease })
        }
    }

    fun addAllergy(allergy: String) {
        val trimmed = allergy.trim()
        if (trimmed.isNotBlank() && !_uiState.value.editedDrugAllergies.contains(trimmed)) {
            _uiState.update {
                it.copy(editedDrugAllergies = it.editedDrugAllergies + trimmed)
            }
        }
    }

    fun removeAllergy(allergy: String) {
        _uiState.update {
            it.copy(editedDrugAllergies = it.editedDrugAllergies.filter { item -> item != allergy })
        }
    }

    /**
     * Lưu thông tin đã chỉnh sửa vào Room Database
     */
    fun saveProfile() {
        val current = _uiState.value.user ?: return
        val updatedUser = current.copy(
            fullName = _uiState.value.editedFullName.ifBlank { current.fullName },
            birthDate = _uiState.value.editedBirthDate.ifBlank { current.birthDate },
            bloodType = _uiState.value.editedBloodType.ifBlank { current.bloodType },
            chronicDiseases = _uiState.value.editedChronicDiseases,
            drugAllergies = _uiState.value.editedDrugAllergies
        )

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    userDao.insertUser(UserEntity.fromDomainModel(updatedUser))
                }
                _uiState.update {
                    it.copy(
                        user = updatedUser,
                        isEditing = false,
                        saveSuccessMessage = "Đã lưu cập nhật hồ sơ y tế thành công!"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(status = ProfileUiStatus.Error("Lỗi khi lưu: ${e.localizedMessage}"))
                }
            }
        }
    }

    private suspend fun seedDefaultUser(): UserEntity {
        val defaultUser = UserEntity(
            id = "user_demo_01",
            fullName = "Bác Nguyễn Văn Nam",
            email = "nam.nguyen@familycare.vn",
            avatarUrl = "",
            familyId = "fam_123456",
            role = "MEMBER",
            birthDate = "15/04/1958",
            bloodType = "O+",
            chronicDiseasesJson = "[\"Cao huyết áp vô căn (10 năm)\", \"Đái tháo đường type 2\"]",
            drugAllergiesJson = "[\"Dị ứng Aspirin\", \"Dị ứng nhóm Sulfamid\"]"
        )
        userDao.insertUser(defaultUser)
        return defaultUser
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val db = AppDatabase.getInstance(context.applicationContext)
                return ProfileViewModel(db.userDao()) as T
            }
        }
    }
}
