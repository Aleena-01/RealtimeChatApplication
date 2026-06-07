package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getChatRoomId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    fun getConversationUsers(): Flow<List<User>> = callbackFlow {
        val currentUserId = auth.currentUser?.uid ?: return@callbackFlow
        
        val chatsListener = db.collection(Constants.CHATS_COLLECTION)
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { chatSnapshot, error ->
                if (error != null) return@addSnapshotListener
                
                val otherUserIds = chatSnapshot?.documents?.mapNotNull { doc ->
                    val participants = doc.get("participants") as? List<*>
                    participants?.filterIsInstance<String>()?.find { it != currentUserId }
                }?.distinct() ?: emptyList()
                
                if (otherUserIds.isEmpty()) {
                    trySend(emptyList())
                } else {
                    db.collection(Constants.USERS_COLLECTION)
                        .whereIn(com.google.firebase.firestore.FieldPath.documentId(), otherUserIds)
                        .addSnapshotListener { userSnapshot, _ ->
                            if (userSnapshot != null) {
                                val users = userSnapshot.toObjects<User>()
                                users.forEach { user ->
                                    val chatDoc = chatSnapshot?.documents?.find { doc ->
                                        val participants = doc.get("participants") as? List<*>
                                        participants?.contains(user.uid) == true
                                    }
                                    user.lastMessage = chatDoc?.getString("lastMessage") ?: ""
                                    user.lastMessageTimestamp = chatDoc?.getLong("lastMessageTimestamp") ?: 0
                                    
                                    // Count unread messages for this user
                                    val chatId = chatDoc?.id ?: ""
                                    if (chatId.isNotEmpty()) {
                                        db.collection(Constants.CHATS_COLLECTION)
                                            .document(chatId)
                                            .collection(Constants.MESSAGES_COLLECTION)
                                            .whereEqualTo("receiverId", currentUserId)
                                            .whereEqualTo("read", false)
                                            .addSnapshotListener { msgSnapshot, _ ->
                                                user.unreadCount = msgSnapshot?.size() ?: 0
                                                trySend(users.toList()) // Resend the list with updated count
                                            }
                                    }
                                }
                                trySend(users)
                            }
                        }
                }
            }
        awaitClose { chatsListener.remove() }
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
        val chatId = getChatRoomId(senderId, receiverId)

        val messageId = db.collection(Constants.CHATS_COLLECTION)
            .document(chatId)
            .collection(Constants.MESSAGES_COLLECTION)
            .document().id

        val msg = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = receiverId,
            messageText = text,
            timestamp = System.currentTimeMillis(),
            type = type,
            imageUrl = imageUrl,
            read = false
        )

        val chatMetadata = hashMapOf(
            "lastMessage" to (if (type == "image") "📷 Image" else text),
            "lastMessageTimestamp" to System.currentTimeMillis(),
            "participants" to listOf(senderId, receiverId)
        )

        // 1. Ensure the parent chat document exists and has the participants list
        db.collection(Constants.CHATS_COLLECTION)
            .document(chatId)
            .set(chatMetadata, com.google.firebase.firestore.SetOptions.merge())
            .await()

        // 2. Add the message to the subcollection
        db.collection(Constants.CHATS_COLLECTION)
            .document(chatId)
            .collection(Constants.MESSAGES_COLLECTION)
            .document(messageId)
            .set(msg)
            .await()
    }

    fun getUser(userId: String): Flow<User?> = callbackFlow {
        val subscription = db.collection(Constants.USERS_COLLECTION)
            .document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) trySend(snapshot.toObject(User::class.java))
            }
        awaitClose { subscription.remove() }
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

    suspend fun markAllAsRead(otherUserId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val chatRoomId = getChatRoomId(currentUserId, otherUserId)
        val unreadMessages = db.collection(Constants.CHATS_COLLECTION)
            .document(chatRoomId)
            .collection(Constants.MESSAGES_COLLECTION)
            .whereEqualTo("receiverId", currentUserId)
            .whereEqualTo("read", false)
            .get()
            .await()

        val batch = db.batch()
        for (doc in unreadMessages.documents) {
            batch.update(doc.reference, "read", true)
        }
        batch.commit().await()
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
}
