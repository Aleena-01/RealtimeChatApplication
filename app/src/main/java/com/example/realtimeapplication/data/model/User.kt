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
    // Settings
    val showLastSeen: Boolean = true,
    val showReadReceipts: Boolean = true
)
