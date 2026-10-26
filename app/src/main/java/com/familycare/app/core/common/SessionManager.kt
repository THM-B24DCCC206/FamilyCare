package com.familycare.app.core.common

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("familycare_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_ROLE = "user_role"

        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_FAMILY_NAME = "family_name"
        private const val KEY_INVITE_CODE = "invite_code"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
    }

    fun saveUserSession(
        userId: String,
        name: String,
        email: String,
        role: String = "ADMIN",
        familyId: String? = null,
        familyName: String? = null,
        inviteCode: String? = null
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
            if (!familyId.isNullOrEmpty()) {
                putString(KEY_FAMILY_ID, familyId)
                putString(KEY_FAMILY_NAME, familyName ?: "Tổ Ấm Gia Đình")
                putString(KEY_INVITE_CODE, inviteCode ?: "")
            } else {
                remove(KEY_FAMILY_ID)
                remove(KEY_FAMILY_NAME)
                remove(KEY_INVITE_CODE)
            }
            apply()
        }
    }

    fun saveFamilySession(
        familyId: String,
        familyName: String,
        inviteCode: String
    ) {
        prefs.edit().apply {
            putString(KEY_FAMILY_ID, familyId)
            putString(KEY_FAMILY_NAME, familyName)
            putString(KEY_INVITE_CODE, inviteCode)
            apply()
        }
    }

    fun clearSession() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_ID)
            remove(KEY_USER_NAME)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_ROLE)
            remove(KEY_FAMILY_ID)
            remove(KEY_FAMILY_NAME)
            remove(KEY_INVITE_CODE)
            apply()
        }
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val hasFamily: Boolean
        get() = !prefs.getString(KEY_FAMILY_ID, null).isNullOrEmpty()

    val userId: String
        get() = prefs.getString(KEY_USER_ID, "") ?: ""

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "Thành viên") ?: "Thành viên"

    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    val userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "MEMBER") ?: "MEMBER"

    val familyName: String
        get() = prefs.getString(KEY_FAMILY_NAME, "Tổ Ấm Yêu Thương") ?: "Tổ Ấm Yêu Thương"

    val inviteCode: String
        get() = prefs.getString(KEY_INVITE_CODE, "") ?: ""

    fun saveGeminiApiKey(apiKey: String) {
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply()
    }

    val geminiApiKey: String
        get() = prefs.getString(KEY_GEMINI_API_KEY, "") ?: ""
}
