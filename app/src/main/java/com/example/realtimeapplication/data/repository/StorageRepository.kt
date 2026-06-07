package com.example.realtimeapplication.data.repository

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class StorageRepository {
    private val storage = FirebaseStorage.getInstance()

    suspend fun uploadImage(uri: Uri, folder: String): String {
        try {
            val fileName = "${System.currentTimeMillis()}.jpg"
            val ref = storage.reference.child(folder).child(fileName)
            
            // Standard Firebase Storage upload using putFile
            // This is the most efficient way to upload local files
            ref.putFile(uri).await()
            
            // Immediately request the download URL
            // If this fails with "Object does not exist", it usually means 
            // the upload was rejected by Firebase Security Rules.
            return ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: "Unknown Storage Error"
            if (errorMsg.contains("Permission denied", ignoreCase = true)) {
                throw Exception("Storage Permission Denied: Please check your Firebase Storage Rules in the console.")
            } else if (errorMsg.contains("does not exist", ignoreCase = true)) {
                throw Exception("Storage Error: Object was not created. This usually happens due to locked Firebase Rules.")
            }
            throw Exception(errorMsg)
        }
    }
}
