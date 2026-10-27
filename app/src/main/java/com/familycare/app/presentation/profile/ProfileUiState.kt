package com.familycare.app.presentation.profile

import com.familycare.app.domain.model.User
import com.familycare.app.presentation.home.Member

sealed interface ProfileUiStatus {
    object Loading : ProfileUiStatus
    object Success : ProfileUiStatus
    data class Error(val message: String) : ProfileUiStatus
}

data class ProfileUiState(
    val status: ProfileUiStatus = ProfileUiStatus.Loading,
    val currentUser: User? = null,
    val familyMembers: List<Member> = emptyList(),
    val selectedMemberId: String = "",
    val selectedMemberUser: User? = null,
    val selectedMemberMedicines: List<String> = emptyList(),
    val isEditing: Boolean = false,
    val editedFullName: String = "",
    val editedBirthDate: String = "",
    val editedBloodType: String = "",
    val editedChronicDiseases: List<String> = emptyList(),
    val editedDrugAllergies: List<String> = emptyList(),
    val newDiseaseInput: String = "",
    val newAllergyInput: String = "",
    val showAddMemberDialog: Boolean = false,
    val showEditHealthCardDialog: Boolean = false,
    val saveSuccessMessage: String? = null
) {
    // Tương thích ngược với các tham chiếu .user cũ
    val user: User? get() = selectedMemberUser ?: currentUser
}
