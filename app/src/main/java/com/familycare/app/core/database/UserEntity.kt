package com.familycare.app.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.familycare.app.domain.model.User
import com.familycare.app.domain.model.UserRole

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String,
    val fullName: String,
    val email: String,
    val avatarUrl: String = "",
    val familyId: String = "",
    val role: String = "MEMBER",
    val birthDate: String = "",
    val bloodType: String = "",
    val chronicDiseasesJson: String = "[]",
    val drugAllergiesJson: String = "[]"
) {
    fun toDomainModel(): User {
        val userRole = runCatching { UserRole.valueOf(role) }.getOrDefault(UserRole.MEMBER)
        val diseases = parseList(chronicDiseasesJson)
        val allergies = parseList(drugAllergiesJson)

        return User(
            id = id,
            fullName = fullName,
            email = email,
            avatarUrl = avatarUrl,
            familyId = familyId,
            role = userRole,
            birthDate = birthDate,
            bloodType = bloodType,
            chronicDiseases = diseases,
            drugAllergies = allergies
        )
    }

    fun toDomain(): User = toDomainModel()

    private fun parseList(raw: String): List<String> {
        if (raw.isBlank() || raw == "[]") return emptyList()
        return raw.trim('[', ']')
            .split(",")
            .map { it.trim().trim('"', '\'') }
            .filter { it.isNotBlank() }
    }

    companion object {
        fun fromDomainModel(user: User): UserEntity {
            val diseasesJson = "[${user.chronicDiseases.joinToString(",") { "\"$it\"" }}]"
            val allergiesJson = "[${user.drugAllergies.joinToString(",") { "\"$it\"" }}]"

            return UserEntity(
                id = user.id,
                fullName = user.fullName,
                email = user.email,
                avatarUrl = user.avatarUrl,
                familyId = user.familyId,
                role = user.role.name,
                birthDate = user.birthDate,
                bloodType = user.bloodType,
                chronicDiseasesJson = diseasesJson,
                drugAllergiesJson = allergiesJson
            )
        }

        fun fromDomain(user: User): UserEntity = fromDomainModel(user)
    }
}
