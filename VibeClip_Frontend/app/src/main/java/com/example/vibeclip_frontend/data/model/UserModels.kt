package com.example.vibeclip_frontend.data.model

import java.util.UUID

data class UserResponse(
    val id: String,
    val email: String? = null,
    val username: String,
    val avatarUrl: String? = null,
    val createdAt: String
)

data class UserProfileResponse(
    val id: String,
    val avatarUrl: String?,
    val username: String,
    val privateProfile: Boolean,
    val subscribed: Boolean,
    val subscribersCount: Long,
    val subscriptionsCount: Long,
    val videos: List<VideoResponse> = emptyList()
)