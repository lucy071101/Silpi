package com.silpi.app

data class ChatMessage(
        val message: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val imageUrl: String = "",
        val imageData: String = "",
        val messageType: String = "text",
        val timestamp: Long = 0L
)
