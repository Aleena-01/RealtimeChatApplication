package com.example.realtimeapplication.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadImage(uri: Uri, folder: String): String {
        val fileName = System.currentTimeMillis().toString()
        val ref = storage.reference.child(folder).child(fileName)
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }
}
