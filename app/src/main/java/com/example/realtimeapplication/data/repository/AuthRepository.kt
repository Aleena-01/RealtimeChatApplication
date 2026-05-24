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

    suspend fun login(email: String, pass: String) = auth.signInWithEmailAndPassword(email, pass).await()

    suspend fun register(email: String, pass: String, username: String, phone: String = "") {
        val result = auth.createUserWithEmailAndPassword(email, pass).await()
        val uid = result.user?.uid ?: return
        val user = User(
            uid = uid, 
            username = username, 
            email = email, 
            phoneNumber = phone,
            showLastSeen = true,
            showReadReceipts = true
        )
        db.collection(Constants.USERS_COLLECTION).document(uid).set(user).await()
    }

    suspend fun signInWithCredential(credential: AuthCredential) = auth.signInWithCredential(credential).await()

    fun logout() = auth.signOut()
    
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
}
