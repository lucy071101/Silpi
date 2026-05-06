package com.silpi.app
//구조 다시 설계 firebase 서버 사용하기 위해
//여기는 보낸 메시지들이 어떤 데이터를 보내는지
data class ChatMessage(
        val message: String = "",
        val senderId: String = "",
        val senderName: String = "",
        val imageUrl: String = "",
        val messageType: String = "text",
        val timestamp: Long = 0L
)
