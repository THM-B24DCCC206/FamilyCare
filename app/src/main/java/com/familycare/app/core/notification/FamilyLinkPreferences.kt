
package com.familycare.app.core.notification

import android.content.Context

object FamilyLinkPreferences {
    private const val PREFS_NAME = "family_link_preferences"
    private const val KEY_FAMILY_CODE = "family_code"

    fun getFamilyCode(context: Context): String? {
        return context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FAMILY_CODE, null)
            ?.takeIf { it.isNotBlank() }
    }

    fun saveFamilyCode(context: Context, familyCode: String) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FAMILY_CODE, familyCode.trim().uppercase())
            .apply()
    }

    fun clearFamilyCode(context: Context) {
        context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_FAMILY_CODE)
            .apply()
    }
}
