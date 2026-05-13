package com.silpi.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

object CurrentUserProvider {
    private const val PREF_NAME = "current_user"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_CITY = "city"
    private const val KEY_BIO = "bio"
    private const val KEY_INTERESTS = "interests"
    private const val KEY_PROFILE_IMAGE_DATA = "profile_image_data"

    fun userId(context: Context): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) return authUser.uid

        return prefs(context).getString(KEY_USER_ID, null) ?: TestUser.USER_ID
    }

    fun userName(context: Context): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        if (authUser != null) {
            return prefs(context).getString(KEY_USER_NAME, null)
                    ?: authUser.displayName
                    ?: authUser.email
                    ?: TestUser.USER_NAME
        }

        return prefs(context).getString(KEY_USER_NAME, null) ?: TestUser.USER_NAME
    }

    fun email(context: Context): String {
        val authUser = FirebaseAuth.getInstance().currentUser
        return authUser?.email ?: prefs(context).getString(KEY_EMAIL, null) ?: ""
    }

    fun city(context: Context): String {
        return prefs(context).getString(KEY_CITY, null) ?: "서울시 강남구"
    }

    fun bio(context: Context): String {
        return prefs(context).getString(KEY_BIO, null)
                ?: "안녕하세요! 서울에서 IT 개발자로 일하고 있습니다. 새로운 사람들과 소통하는 것을 좋아하고, 긍정적인 에너지를 나누고 싶어요"
    }

    fun interests(context: Context): List<String> {
        val rawValue = prefs(context).getString(KEY_INTERESTS, null)
        return rawValue
                ?.split("|")
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                ?.takeIf { it.isNotEmpty() }
                ?: listOf("게임", "독서", "음악 감상", "카페 탐방", "러닝", "영화")
    }

    fun profileImageData(context: Context): String {
        return prefs(context).getString(KEY_PROFILE_IMAGE_DATA, null) ?: ""
    }

    fun user(context: Context): User {
        return User(
                userId = userId(context),
                userName = userName(context),
                email = email(context),
                city = city(context),
                bio = bio(context),
                interests = interests(context),
                profileImageData = profileImageData(context)
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

    fun saveProfile(
            context: Context,
            userName: String,
            city: String,
            bio: String,
            interests: List<String>,
            profileImageData: String
    ) {
        prefs(context)
                .edit()
                .putString(KEY_USER_NAME, userName)
                .putString(KEY_CITY, city)
                .putString(KEY_BIO, bio)
                .putString(KEY_INTERESTS, interests.joinToString("|"))
                .putString(KEY_PROFILE_IMAGE_DATA, profileImageData)
                .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
            context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
}
