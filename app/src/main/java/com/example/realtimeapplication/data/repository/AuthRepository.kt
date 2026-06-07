package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser() = auth.currentUser

    suspend fun signInWithCredential(credential: AuthCredential) = auth.signInWithCredential(credential).await()

    fun logout() = auth.signOut()

    suspend fun getUserData(uid: String): User? {
        return try {
            val doc = db.collection(Constants.USERS_COLLECTION).document(uid).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveUser(user: User) {
        db.collection(Constants.USERS_COLLECTION).document(user.uid).set(user).await()
    }
    
    suspend fun updateProfileImage(url: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid).update("profileImageUrl", url).await()
    }

    suspend fun updatePrivacySettings(lastSeen: Boolean, readReceipts: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid).update(
            "showLastSeen", lastSeen,
            "showReadReceipts", readReceipts
        ).await()
    }

    suspend fun updateAbout(about: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid)
            .set(mapOf("about" to about), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    suspend fun updateUsername(name: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid)
            .update("username", name)
            .await()
    }
}
