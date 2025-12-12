package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.UserResponse

class UserRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun me(token: String): Result<UserResponse> = runCatching {
        val resp = apiService.getCurrentUser("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
}

