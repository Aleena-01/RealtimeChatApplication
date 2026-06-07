package com.example.realtimeapplication.data.api

import com.example.realtimeapplication.data.model.CloudinaryResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface CloudinaryApi {
    @Multipart
    @POST("{cloud_name}/image/upload")
    suspend fun uploadImage(
        @Path("cloud_name") cloudName: String,
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): Response<CloudinaryResponse>

    companion object {
        const val BASE_URL = "https://api.cloudinary.com/v1_1/"
    }
}
