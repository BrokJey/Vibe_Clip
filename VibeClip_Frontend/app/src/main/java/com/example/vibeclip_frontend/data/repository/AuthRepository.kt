package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.AuthResponse
import com.example.vibeclip_frontend.data.model.LoginRequest
import com.example.vibeclip_frontend.data.model.RegisterRequest
import com.example.vibeclip_frontend.util.ErrorContext
import com.example.vibeclip_frontend.util.ErrorMessages

class AuthRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun register(request: RegisterRequest): Result<AuthResponse> {
        return try {
            val response = apiService.register(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val body = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        ErrorMessages.messageFromHttp(response.code(), body, ErrorContext.AuthRegister)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorMessages.messageOnly(e, ErrorContext.AuthRegister)))
        }
    }

    suspend fun login(request: LoginRequest): Result<AuthResponse> {
        return try {
            val response = apiService.login(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val body = response.errorBody()?.string()
                Result.failure(
                    Exception(
                        ErrorMessages.messageFromHttp(response.code(), body, ErrorContext.AuthLogin)
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(Exception(ErrorMessages.messageOnly(e, ErrorContext.AuthLogin)))
        }
    }
}
