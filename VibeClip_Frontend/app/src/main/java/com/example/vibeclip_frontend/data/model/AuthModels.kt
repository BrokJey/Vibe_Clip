package com.example.vibeclip_frontend.data.model

data class RegisterRequest(
    val email: String,
    val username: String,
    val password: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class AuthResponse(
    val accessToken: String
)


