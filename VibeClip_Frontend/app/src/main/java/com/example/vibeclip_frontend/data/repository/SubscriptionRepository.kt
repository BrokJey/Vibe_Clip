package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.SubscriptionRequestResponse

class SubscriptionRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun subscribe(token: String, targetUserId: String): Result<Unit> = runCatching {
        val resp = apiService.subscribe("Bearer $token", targetUserId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun unsubscribe(token: String, targetUserId: String): Result<Unit> = runCatching {
        val resp = apiService.unsubscribe("Bearer $token", targetUserId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun getOutgoingRequests(token: String): Result<List<SubscriptionRequestResponse>> = runCatching {
        val resp = apiService.getOutgoingSubscriptionRequests("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun getIncomingRequests(token: String): Result<List<SubscriptionRequestResponse>> = runCatching {
        val resp = apiService.getIncomingSubscriptionRequests("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun acceptRequest(token: String, subscriberId: String): Result<Unit> = runCatching {
        val resp = apiService.acceptSubscription("Bearer $token", subscriberId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun rejectRequest(token: String, subscriberId: String): Result<Unit> = runCatching {
        val resp = apiService.rejectSubscription("Bearer $token", subscriberId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
}
