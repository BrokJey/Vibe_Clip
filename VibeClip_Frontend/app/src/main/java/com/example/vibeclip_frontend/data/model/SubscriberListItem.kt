package com.example.vibeclip_frontend.data.model

data class SubscriberListItem(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    val isPending: Boolean = false
)
