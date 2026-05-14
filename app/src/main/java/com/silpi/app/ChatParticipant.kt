package com.silpi.app

data class ChatParticipant(
        val userId: String,
        val userName: String,
        val profileImageData: String = "",
        val isMe: Boolean = false
)
