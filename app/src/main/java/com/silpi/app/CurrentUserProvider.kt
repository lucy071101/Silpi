package com.silpi.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object CurrentUserProvider {
    private const val PREF_NAME = "current_user"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_EMAIL = "email"

    fun userId(context: Context): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) return authUser.uid

        return prefs(context).getString(KEY_USER_ID, null) ?: TestUser.USER_ID
    }

    fun userName(context: Context): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            return authUser.displayName
                    ?: prefs(context).getString(KEY_USER_NAME, null)
                    ?: authUser.email
                    ?: TestUser.USER_NAME
        }

        return prefs(context).getString(KEY_USER_NAME, null) ?: TestUser.USER_NAME
    }

    fun user(context: Context): User {
        return User(
                userId = userId(context),
                userName = userName(context)
        )
    }

    fun saveSignedInUser(context: Context, userId: String, userName: String, email: String) {
        prefs(context)
                .edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_EMAIL, email)
                .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
