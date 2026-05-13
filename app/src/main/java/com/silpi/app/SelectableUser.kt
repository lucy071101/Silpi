package com.silpi.app
//firestore에서 가져와서 변환 UI용
data class SelectableUser(
        val userId: String,
        val userName: String,
        val email: String = "",
        var isSelected: Boolean = false
)
