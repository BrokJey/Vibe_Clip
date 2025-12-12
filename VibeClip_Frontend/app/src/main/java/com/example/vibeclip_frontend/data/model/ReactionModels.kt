package com.example.vibeclip_frontend.data.model

data class ReactionRequest(
    val videoId: String,
    val reactionType: String
)

data class ReactionResponse(
    val id: String,
    val userId: String,
    val videoId: String,
    val reactionType: String,
    val createdAt: String
)


