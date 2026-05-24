package com.example.vibeclip_frontend.data.model

data class SubscriptionRequestResponse(
    val subscriberId: String,
    val username: String
)

data class StoredSubscription(
    val userId: String,
    val username: String,
    val avatarUrl: String? = null,
    /** Заявка отправлена, ожидает принятия целевым пользователем */
    val isPending: Boolean = false
)
