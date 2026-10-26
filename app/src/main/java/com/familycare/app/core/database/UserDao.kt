package com.familycare.app.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    // TV2 & Profile / AI context
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Query("SELECT * FROM users LIMIT 1")
    fun observeCurrentUser(): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    // TV1 & Auth / Session
    suspend fun register(user: UserAccountEntity): Boolean
    suspend fun getUserByEmail(email: String): UserAccountEntity?
    suspend fun getUserAccountById(userId: String): UserAccountEntity?
    suspend fun updateFamily(userId: String, familyId: String, familyName: String, inviteCode: String, role: String? = null)
}
