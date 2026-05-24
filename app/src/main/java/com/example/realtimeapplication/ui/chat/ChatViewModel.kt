package com.example.realtimeapplication.ui.chat

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.model.Message
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.ChatRepository
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {
    private val repository = ChatRepository()

    fun getMessages(otherUserId: String): LiveData<List<Message>> {
        return repository.getMessages(otherUserId).asLiveData()
    }

    fun getOtherUser(userId: String): LiveData<User?> {
        return repository.getUser(userId).asLiveData()
    }

    fun sendMessage(receiverId: String, text: String, type: String = "text", imageUrl: String = "") {
        if (text.trim().isEmpty() && imageUrl.isEmpty()) return
        viewModelScope.launch {
            repository.sendMessage(receiverId, text, type, imageUrl)
        }
    }

    fun setTyping(isTyping: Boolean) {
        repository.updateTypingStatus(isTyping)
    }

    fun markAsRead(otherUserId: String, messageId: String) {
        viewModelScope.launch {
            repository.markMessageAsRead(otherUserId, messageId)
        }
    }
}
