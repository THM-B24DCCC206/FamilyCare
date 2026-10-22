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
    }

    fun saveUserSession(
        userId: String,
        name: String,
        email: String,
        role: String = "ADMIN"
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_ROLE, role)
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
        prefs.edit().clear().apply()
    }

    val isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    val hasFamily: Boolean
        get() = !prefs.getString(KEY_FAMILY_ID, null).isNullOrEmpty()

    val userId: String
        get() = prefs.getString(KEY_USER_ID, "user_me") ?: "user_me"

    val userName: String
        get() = prefs.getString(KEY_USER_NAME, "Nguyễn Văn Nam") ?: "Nguyễn Văn Nam"

    val userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "nam.nguyen@gmail.com") ?: "nam.nguyen@gmail.com"

    val userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "ADMIN") ?: "ADMIN"

    val familyName: String
        get() = prefs.getString(KEY_FAMILY_NAME, "Gia đình Bác Nam") ?: "Gia đình Bác Nam"

    val inviteCode: String
        get() = prefs.getString(KEY_INVITE_CODE, "FAM-8921") ?: "FAM-8921"
}
