package com.familycare.app.domain.repository

import com.familycare.app.core.common.Resource
import com.familycare.app.domain.model.Family
import com.familycare.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCurrentUser(): Flow<User?>
    suspend fun login(email: String, pass: String): Resource<User>
    suspend fun register(email: String, pass: String, fullName: String): Resource<User>
    suspend fun logout()
    suspend fun createFamily(familyName: String): Resource<Family>
    suspend fun joinFamilyByInviteCode(code: String): Resource<Family>
}
