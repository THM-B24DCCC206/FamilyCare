package com.familycare.app.presentation.profile

import com.familycare.app.domain.model.User

sealed interface ProfileUiStatus {
    object Loading : ProfileUiStatus
    object Success : ProfileUiStatus
    data class Error(val message: String) : ProfileUiStatus
}

data class ProfileUiState(
    val status: ProfileUiStatus = ProfileUiStatus.Loading,
    val user: User? = null,
    val isEditing: Boolean = false,
    val editedFullName: String = "",
    val editedBirthDate: String = "",
    val editedBloodType: String = "",
    val editedChronicDiseases: List<String> = emptyList(),
    val editedDrugAllergies: List<String> = emptyList(),
    val newDiseaseInput: String = "",
    val newAllergyInput: String = "",
    val saveSuccessMessage: String? = null
)
