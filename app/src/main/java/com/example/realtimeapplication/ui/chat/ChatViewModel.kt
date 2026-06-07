package com.example.realtimeapplication.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.model.Group
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.ChatRepository
import com.example.realtimeapplication.data.repository.ContactRepository
import com.example.realtimeapplication.data.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val groupRepository = GroupRepository()
    private val contactRepository = ContactRepository()

    fun getMessages(otherUserId: String): LiveData<List<Message>> {
        return chatRepository.getMessages(otherUserId).asLiveData()
    }

    fun getOtherUser(userId: String): LiveData<User?> {
        return chatRepository.getUser(userId).asLiveData()
    }

    fun getContact(userId: String): LiveData<com.example.realtimeapplication.data.model.Contact?> {
        return contactRepository.getContact(userId).asLiveData()
    }

    fun sendMessage(receiverId: String, text: String, type: String = "text", imageUrl: String = "") {
        if (text.trim().isEmpty() && imageUrl.isEmpty()) return
        viewModelScope.launch {
            chatRepository.sendMessage(receiverId, text, type, imageUrl)
        }
    }

    fun setTyping(isTyping: Boolean) {
        chatRepository.updateTypingStatus(isTyping)
    }

    fun markAsRead(otherUserId: String, messageId: String) {
        viewModelScope.launch {
            chatRepository.markMessageAsRead(otherUserId, messageId)
        }
    }

    fun markAllAsRead(otherUserId: String) {
        viewModelScope.launch {
            chatRepository.markAllAsRead(otherUserId)
        }
    }

    // Group methods
    fun getGroupMessages(groupId: String): LiveData<List<Message>> {
        return groupRepository.getGroupMessages(groupId).asLiveData()
    }

    fun sendGroupMessage(groupId: String, text: String) {
        viewModelScope.launch {
            groupRepository.sendGroupMessage(groupId, text)
        }
    }

    fun getGroup(groupId: String): LiveData<Group?> = callbackFlow {
        val subscription = FirebaseFirestore.getInstance().collection("groups")
            .document(groupId)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject<Group>())
            }
        awaitClose { subscription.remove() }
    }.asLiveData()
}
