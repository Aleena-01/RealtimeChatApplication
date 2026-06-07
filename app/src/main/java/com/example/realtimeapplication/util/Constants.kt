package com.example.realtimeapplication.util

object Constants {
    const val USERS_COLLECTION = "users"
    const val CHATS_COLLECTION = "chats"
    const val MESSAGES_COLLECTION = "messages"
    const val CONTACTS_COLLECTION = "contacts"

    fun normalizePhone(phone: String): String {
        var p = phone.replace(" ", "").replace("-", "")
        if (p.startsWith("0") && p.length == 11) {
            p = "+92" + p.substring(1)
        } else if (!p.startsWith("+")) {
            p = "+$p"
        }
        return p
    }
}
