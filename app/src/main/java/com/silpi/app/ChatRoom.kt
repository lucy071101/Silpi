package com.silpi.app

data class ChatRoom(
        val roomId: String = "",
        val roomName: String = "",

        val participants: List<String> = emptyList(),
        val participantNames: Map<String, String> = emptyMap(),

        val lastMessage: String = "",
        val lastMessageTime: Long = 0L,

        val unreadCount: Map<String, Int> = emptyMap(),

        val group: Boolean = false,

        val createdBy: String = "",
        val createdAt: Long = 0L
)