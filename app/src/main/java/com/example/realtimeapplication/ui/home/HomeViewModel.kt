package com.example.realtimeapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.model.Contact
import com.example.realtimeapplication.data.model.Group
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.ChatRepository
import com.example.realtimeapplication.data.repository.ContactRepository
import com.example.realtimeapplication.data.repository.GroupRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    private val chatRepository = ChatRepository()
    private val groupRepository = GroupRepository()
    private val contactRepository = ContactRepository()
    private val auth = FirebaseAuth.getInstance()

    // Local list of deleted chat IDs (for current session)
    private val deletedChatIds = MutableStateFlow<Set<String>>(emptySet())

    // Combine users with active chats and groups into a single list
    val homeItems: LiveData<List<Any>> = combine(
        chatRepository.getConversationUsers(),
        groupRepository.getGroups(),
        deletedChatIds
    ) { users, groups, deletedIds ->
        val filteredUsers = users.filter { it.uid !in deletedIds }
        val filteredGroups = groups.filter { it.groupId !in deletedIds }
        
        (filteredUsers + filteredGroups).sortedByDescending { item ->
            when (item) {
                is User -> item.lastMessageTimestamp
                is Group -> 0L // Group timestamp not yet implemented
                else -> 0L
            }
        }
    }.asLiveData()

    fun deleteChat(item: Any) {
        val id = if (item is User) item.uid else if (item is Group) item.groupId else null
        id?.let {
            deletedChatIds.value = deletedChatIds.value + it
        }
    }

    fun updateStatus(status: String) {
        chatRepository.setUserStatus(status)
    }

    val homeItemsWithContacts: LiveData<Pair<List<Any>, List<Contact>>> = combine(
        homeItems.asFlow(),
        contactRepository.getContacts()
    ) { items, contacts ->
        items to contacts
    }.asLiveData()
}
