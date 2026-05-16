package com.silpi.app
//firestore에서 가져오는 순수 데이터용 Api나 DB용
data class User(
        val userId: String = "",
        val userName: String = "",
        val email: String = "",
        val city: String = "",
        val bio: String = "",
        val interests: List<String> = emptyList(),
        val profileImageData: String = ""
)
