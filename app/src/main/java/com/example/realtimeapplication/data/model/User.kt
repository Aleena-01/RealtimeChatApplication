package com.example.realtimeapplication.data.model

data class User(
    val uid: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    val status: String = "Offline",
    val lastSeen: Long = 0,
    val isTyping: Boolean = false,
    val about: String = "Hey there! I am using ChatApp.",
    // Settings
    val showLastSeen: Boolean = true,
    val showReadReceipts: Boolean = true,
    var lastMessage: String = "", // Non-persistent, used for UI display in chat list
    var lastMessageTimestamp: Long = 0,
    var unreadCount: Int = 0 // Non-persistent, calculated for UI
)
