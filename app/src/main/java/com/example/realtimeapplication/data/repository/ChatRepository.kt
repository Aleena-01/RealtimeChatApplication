package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getUsers(): Flow<List<User>> = callbackFlow {
        val subscription = db.collection(Constants.USERS_COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val users = snapshot.toObjects<User>().filter { it.uid != auth.currentUser?.uid }
                    trySend(users)
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val subscription = db.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObject<User>())
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getMessages(otherUserId: String): Flow<List<Message>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        val chatRoomId = getChatRoomId(currentUserId, otherUserId)

        val subscription = db.collection(Constants.CHATS_COLLECTION)
            .document(chatRoomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects<Message>())
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendMessage(receiverId: String, text: String, type: String = "text", imageUrl: String = "") {
        val senderId = auth.currentUser?.uid ?: return
        val chatRoomId = getChatRoomId(senderId, receiverId)
        
        val messageId = db.collection(Constants.CHATS_COLLECTION).document(chatRoomId).collection(Constants.MESSAGES_COLLECTION).document().id
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            messageText = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            imageUrl = imageUrl
        )
        
        db.collection(Constants.CHATS_COLLECTION)
            .document(chatRoomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .document(messageId)
            .set(message)
            .await()
    }

    suspend fun markMessageAsRead(otherUserId: String, messageId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatRoomId = getChatRoomId(currentUserId, otherUserId)
        db.collection(Constants.CHATS_COLLECTION)
            .document(chatRoomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .document(messageId)
            .update("read", true)
            .await()
    }

    fun updateTypingStatus(isTyping: Boolean) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid).update("isTyping", isTyping)
    }

    fun setUserStatus(status: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection(Constants.USERS_COLLECTION).document(uid).update(
            "status", status,
            "lastSeen", System.currentTimeMillis()
        )
    }

    private fun getChatRoomId(user1: String, user2: String): String {
        return if (user1 < user2) "${user1}_${user2}" else "${user2}_${user1}"
    }
}
