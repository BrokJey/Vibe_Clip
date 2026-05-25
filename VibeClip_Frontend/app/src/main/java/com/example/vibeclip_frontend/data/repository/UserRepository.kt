package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.UserProfileResponse
import com.example.vibeclip_frontend.data.model.UserResponse
import okhttp3.MultipartBody

class UserRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun me(token: String): Result<UserResponse> = runCatching {
        val resp = apiService.getCurrentUser("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun getProfile(token: String, username: String): Result<UserProfileResponse> = runCatching {
        val resp = apiService.getUserProfile("Bearer $token", username)
        if (resp.isSuccessful && resp.body() != null) {
            resp.body()!!
        } else {
            throw Exception(
                resp.errorBody()?.string().orEmpty().ifBlank { resp.message() }
            )
        }
    }

    suspend fun uploadAvatar(token: String, avatarPart: MultipartBody.Part): Result<UserResponse> = runCatching {
        val resp = apiService.uploadAvatar("Bearer $token", avatarPart)
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun deleteAvatar(token: String): Result<Unit> = runCatching {
        val resp = apiService.deleteAvatar("Bearer $token")
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
}
