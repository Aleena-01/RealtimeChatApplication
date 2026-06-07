package com.example.realtimeapplication.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.data.repository.UserRepository
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    private val _searchResults = MutableLiveData<List<User>>()
    val searchResults: LiveData<List<User>> = _searchResults

    private val _isSearching = MutableLiveData<Boolean>(false)
    val isSearching: LiveData<Boolean> = _isSearching

    private val _searchError = MutableLiveData<String?>()
    val searchError: LiveData<String?> = _searchError

    fun searchUsers(query: String) {
        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            return
        }

        val normalizedQuery = Constants.normalizePhone(query)

        _isSearching.value = true
        _searchError.value = null

        viewModelScope.launch {
            try {
                // 1. Try exact phone search first
                val exactUser = userRepository.getUserByPhone(normalizedQuery)
                
                // 2. Also search by name AND partial phone matches to be more flexible
                db.collection(Constants.USERS_COLLECTION)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val allUsers = snapshot.toObjects(User::class.java)
                        val queryLower = query.lowercase()
                        
                        val results = allUsers.filter { user ->
                            val matchesName = user.username.lowercase().contains(queryLower)
                            val matchesPhone = user.phoneNumber.contains(query)
                            
                            (matchesName || matchesPhone) && user.uid != auth.currentUser?.uid
                        }
                        
                        // Combine exact and filtered results, ensuring uniqueness by UID
                        val combinedResults = (listOfNotNull(exactUser) + results).distinctBy { it.uid }
                        _searchResults.value = combinedResults
                    }
                    .addOnFailureListener {
                        _searchError.value = "Search failed: ${it.message}"
                    }
            } catch (e: Exception) {
                _searchError.value = "Search failed: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }
}
