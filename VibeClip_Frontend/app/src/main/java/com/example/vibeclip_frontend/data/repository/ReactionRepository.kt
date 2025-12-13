package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.ReactionRequest
import com.example.vibeclip_frontend.data.model.ReactionResponse

class ReactionRepository {
    private val apiService = RetrofitClient.apiService
    
    suspend fun createReaction(token: String, videoId: String, reactionType: String): Result<ReactionResponse> {
        return try {
            val response = apiService.createReaction("Bearer $token", ReactionRequest(videoId, reactionType))
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create reaction: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteReaction(token: String, videoId: String, reactionType: String): Result<Unit> {
        return try {
            val response = apiService.deleteReaction("Bearer $token", videoId, reactionType)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to delete reaction: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVideoReactions(token: String, videoId: String): Result<List<ReactionResponse>> {
        return try {
            val response = apiService.getVideoReactions("Bearer $token", videoId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get reactions: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

