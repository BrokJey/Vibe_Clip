package com.example.vibeclip_frontend.data.model

data class CommentRequest(
    val videoId: String,
    val text: String
)

data class CommentResponse(
    val id: String,
    val videoId: String,
    val userId: String,
    val username: String,
    val text: String,
    val createdAt: String
)

