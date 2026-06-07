package com.example.realtimeapplication.data.repository

import android.content.Context
import android.net.Uri
import com.example.realtimeapplication.data.api.RetrofitClient
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class CloudinaryRepository(private val context: Context) {
    
    private val cloudName = "dwpsxrpuu"
    private val uploadPreset = "chat app"

    suspend fun uploadImage(uri: Uri): String {
        try {
            val file = uriToFile(uri) ?: throw Exception("Failed to process image file")
            
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val presetBody = uploadPreset.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = RetrofitClient.cloudinaryApi.uploadImage(cloudName, body, presetBody)

            if (response.isSuccessful && response.body() != null) {
                return response.body()!!.secureUrl
            } else {
                val errorBody = response.errorBody()?.string()
                throw Exception("Upload failed: ${response.message()} - $errorBody")
            }
        } catch (e: Exception) {
            throw Exception("Cloudinary Error: ${e.localizedMessage}")
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val file = File(context.cacheDir, "temp_upload_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}
