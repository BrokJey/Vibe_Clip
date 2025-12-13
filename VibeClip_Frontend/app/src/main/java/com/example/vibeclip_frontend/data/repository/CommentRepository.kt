package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.CommentRequest
import com.example.vibeclip_frontend.data.model.CommentResponse

class CommentRepository {
    private val apiService = RetrofitClient.apiService
    
    suspend fun getVideoComments(token: String, videoId: String): Result<List<CommentResponse>> {
        return try {
            val response = apiService.getVideoComments("Bearer $token", videoId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to get comments: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createComment(token: String, videoId: String, text: String): Result<CommentResponse> {
        return try {
            val request = CommentRequest(videoId = videoId, text = text)
            val response = apiService.createComment("Bearer $token", videoId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create comment: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

