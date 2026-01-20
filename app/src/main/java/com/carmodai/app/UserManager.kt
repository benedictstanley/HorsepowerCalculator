package com.carmodai.app

import android.content.Context

object UserManager {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_CURRENT_USER_EMAIL = "current_user_email"

    fun isLoggedIn(context: Context): Boolean {
        return getCurrentUserEmail(context) != null
    }

    fun getCurrentUserEmail(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CURRENT_USER_EMAIL, null)
    }

    fun login(context: Context, email: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CURRENT_USER_EMAIL, email).apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CURRENT_USER_EMAIL).apply()
    }
}
