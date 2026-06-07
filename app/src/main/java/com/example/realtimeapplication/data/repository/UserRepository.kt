package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class UserRepository {
    private val db = FirebaseFirestore.getInstance()

    fun normalizePhone(phone: String): String {
        var p = phone.replace(" ", "").replace("-", "")
        if (p.startsWith("0") && p.length == 11) {
            p = "+92" + p.substring(1)
        } else if (!p.startsWith("+")) {
            p = "+$p"
        }
        return p
    }

    suspend fun getUserByPhone(phone: String): User? {
        val normalized = normalizePhone(phone)
        val snapshot = db.collection(Constants.USERS_COLLECTION)
            .whereEqualTo("phoneNumber", normalized)
            .get()
            .await()

        return snapshot.documents.firstOrNull()?.toObject(User::class.java)
    }

    suspend fun getUserById(uid: String): User? {
        return try {
            val doc = db.collection(Constants.USERS_COLLECTION).document(uid).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
