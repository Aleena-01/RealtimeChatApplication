package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.Group
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getGroups(): Flow<List<Group>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection("groups")
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects<Group>())
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createGroup(name: String, members: List<String>) {
        val adminId = auth.currentUser?.uid ?: return
        val allMembers = members.toMutableList().apply { add(adminId) }
        val groupId = db.collection("groups").document().id
        val group = Group(
            groupId = groupId,
            groupName = name,
            members = allMembers,
            adminId = adminId
        )
        db.collection("groups").document(groupId).set(group).await()
    }

    fun getGroupMessages(groupId: String): Flow<List<Message>> = callbackFlow {
        val subscription = db.collection("groups")
            .document(groupId)
            .collection(Constants.MESSAGES_COLLECTION)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects<Message>())
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun sendGroupMessage(groupId: String, text: String) {
        val senderId = auth.currentUser?.uid ?: return
        val messageId = db.collection("groups").document(groupId).collection(Constants.MESSAGES_COLLECTION).document().id
        val message = Message(
            messageId = messageId,
            senderId = senderId,
            receiverId = groupId, // For groups, receiverId is the groupId
            messageText = text,
            timestamp = System.currentTimeMillis()
        )
        db.collection("groups").document(groupId).collection(Constants.MESSAGES_COLLECTION).document(messageId).set(message).await()
    }
}
