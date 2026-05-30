package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.SubscriptionRequestResponse

class SubscriptionRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun subscribe(token: String, targetUserId: String): Result<Unit> = runCatching {
        val resp = apiService.subscribe("Bearer $token", targetUserId)
        when {
            resp.isSuccessful -> Unit
            resp.code() == 409 -> resolveSubscribeConflict(token, targetUserId)
            else -> throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
        }
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

    suspend fun getFollowing(token: String): Result<List<SubscriptionRequestResponse>> = runCatching {
        val resp = apiService.getFollowingSubscriptions("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun getFollowers(token: String): Result<List<SubscriptionRequestResponse>> = runCatching {
        val resp = apiService.getFollowers("Bearer $token")
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

    /**
     * 409: заявка уже есть. Если в outgoing — ок. Иначе (например REJECTED) — снимаем старую запись и создаём новую.
     */
    private suspend fun resolveSubscribeConflict(token: String, targetUserId: String) {
        val bearer = "Bearer $token"
        val inOutgoing = getOutgoingRequests(token).getOrNull()
            .orEmpty()
            .any { it.subscriberId == targetUserId }
        if (inOutgoing) return

        val un = apiService.unsubscribe(bearer, targetUserId)
        if (!un.isSuccessful) {
            throw Exception(
                un.errorBody()?.string().orEmpty().ifBlank { "Не удалось отправить заявку повторно" }
            )
        }
        val retry = apiService.subscribe(bearer, targetUserId)
        if (!retry.isSuccessful) {
            throw Exception(retry.errorBody()?.string().orEmpty().ifBlank { retry.message() })
        }
    }
}
