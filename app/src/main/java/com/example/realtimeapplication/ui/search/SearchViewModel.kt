package com.example.realtimeapplication.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _searchError = MutableLiveData<String?>()
    val searchError: LiveData<String?> = _searchError

    fun searchUsers(query: String) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            return
        }

        _isSearching.value = true
        _searchError.value = null

        // We check by Email or Phone specifically if it looks like one, or just query all
        db.collection(Constants.USERS_COLLECTION)
            .get()
            .addOnSuccessListener { snapshot ->
                val allUsers = snapshot.toObjects(User::class.java)
                val results = allUsers.filter { user ->
                    (user.email.equals(normalizedQuery, ignoreCase = true) || 
                     user.phoneNumber == normalizedQuery || 
                     user.username.contains(normalizedQuery, ignoreCase = true)) &&
                     user.uid != auth.currentUser?.uid
                }

                _searchResults.value = results
                if (results.isEmpty()) {
                    _searchError.value = "The person with '$normalizedQuery' is not on the app."
                }
                _isSearching.value = false
            }
            .addOnFailureListener {
                _searchError.value = "Search failed: ${it.message}"
                _isSearching.value = false
            }
    }
}
