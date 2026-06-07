package com.example.realtimeapplication.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.AuthRepository
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val repository = AuthRepository()

    private val _user = MutableLiveData<FirebaseUser?>()
    val user: LiveData<FirebaseUser?> = _user

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    init {
        _user.value = repository.getCurrentUser()
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

    fun fetchUserData(uid: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                _userData.value = repository.getUserData(uid)
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun saveUser(user: User) {
        viewModelScope.launch {
            _loading.value = true
            try {
                repository.saveUser(user)
                _userData.value = user
            } catch (e: Exception) {
                _error.value = e.message
            } finally {
                _loading.value = false
            }
        }
    }

    fun logout() {
        repository.logout()
        _user.value = null
        _userData.value = null
    }

    fun clearError() {
        _error.value = null
    }
}
