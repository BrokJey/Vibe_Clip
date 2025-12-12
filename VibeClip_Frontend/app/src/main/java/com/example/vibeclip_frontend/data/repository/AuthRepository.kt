package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.AuthResponse
import com.example.vibeclip_frontend.data.model.LoginRequest
import com.example.vibeclip_frontend.data.model.RegisterRequest

class AuthRepository {
    private val apiService = RetrofitClient.apiService
    
    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: response.message()
                    ?: "Registration failed"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val error = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                    ?: response.message()
                    ?: "Login failed"
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}


