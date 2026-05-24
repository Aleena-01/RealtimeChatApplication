package com.example.realtimeapplication.data.model

data class Group(
    val groupId: String = "",
    val groupName: String = "",
    val groupImageUrl: String = "",
    val members: List<String> = emptyList(),
    val adminId: String = ""
)
