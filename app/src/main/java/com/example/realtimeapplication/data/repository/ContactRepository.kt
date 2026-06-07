package com.example.realtimeapplication.data.repository

import com.example.realtimeapplication.data.model.Contact
import com.example.realtimeapplication.data.model.User
import com.example.realtimeapplication.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ContactRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    suspend fun addContactBidirectional(otherUser: User, customName: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        // Fetch current user data for the reverse contact
        val currentUserDoc = db.collection(Constants.USERS_COLLECTION).document(currentUserId).get().await()
        val currentUser = currentUserDoc.toObject(User::class.java) ?: return

        // 1. Add to current user's contacts
        val contactForMe = Contact(
            contactUid = otherUser.uid,
            customName = customName,
            phoneNumber = otherUser.phoneNumber
        )
        db.collection(Constants.USERS_COLLECTION)
            .document(currentUserId)
            .collection(Constants.CONTACTS_COLLECTION)
            .document(otherUser.uid)
            .set(contactForMe)
            .await()

        // 2. Add current user to the other user's contacts (bidirectional)
        val contactForOther = Contact(
            contactUid = currentUserId,
            customName = currentUser.username,
            phoneNumber = currentUser.phoneNumber
        )
        db.collection(Constants.USERS_COLLECTION)
            .document(otherUser.uid)
            .collection(Constants.CONTACTS_COLLECTION)
            .document(currentUserId)
            .set(contactForOther)
            .await()
    }

    fun getContacts(): Flow<List<Contact>> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .collection(Constants.CONTACTS_COLLECTION)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    trySend(snapshot.toObjects(Contact::class.java))
                }
            }
        awaitClose { subscription.remove() }
    }

    fun getContact(contactUid: String): Flow<Contact?> = callbackFlow {
        val uid = auth.currentUser?.uid ?: return@callbackFlow
        val subscription = db.collection(Constants.USERS_COLLECTION)
            .document(uid)
            .collection(Constants.CONTACTS_COLLECTION)
            .document(contactUid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(Contact::class.java))
            }
        awaitClose { subscription.remove() }
    }
}
