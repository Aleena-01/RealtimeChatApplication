package com.example.realtimeapplication.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _user.value = repository.getCurrentUser()
    }

    fun login(email: String, pass: String) {
        if (email.isEmpty() || pass.isEmpty()) {
            _error.value = "Please fill all fields"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.login(email, pass)
                _user.value = repository.getCurrentUser()
            } catch (e: Exception) {
                _error.value = e.message ?: "Login failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun register(email: String, pass: String, username: String, phone: String = "") {
        if (email.isEmpty() || pass.isEmpty() || username.isEmpty()) {
            _error.value = "Please fill all fields"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.register(email, pass, username, phone)
                _user.value = repository.getCurrentUser()
            } catch (e: Exception) {
                _error.value = e.message ?: "Registration failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun signInWithCredential(credential: AuthCredential) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.signInWithCredential(credential)
                _user.value = repository.getCurrentUser()
            } catch (e: Exception) {
                _error.value = e.message ?: "Sign-in failed"
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
