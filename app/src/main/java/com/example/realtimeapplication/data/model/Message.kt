package com.example.realtimeapplication.data.model

data class Message(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val messageText: String = "",
    val timestamp: Long = 0,
    val type: String = "text", // "text" or "image"
    val imageUrl: String = "",
    val read: Boolean = false
)
