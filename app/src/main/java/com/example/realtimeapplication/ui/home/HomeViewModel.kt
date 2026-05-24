package com.example.realtimeapplication.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.ChatRepository

class HomeViewModel : ViewModel() {
    private val repository = ChatRepository()

    val users: LiveData<List<User>> = repository.getUsers().asLiveData()

    fun updateStatus(status: String) {
        repository.setUserStatus(status)
    }
}
