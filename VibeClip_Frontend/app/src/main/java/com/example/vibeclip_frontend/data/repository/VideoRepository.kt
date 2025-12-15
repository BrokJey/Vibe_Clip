package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.VideoListResponse
import com.example.vibeclip_frontend.data.model.VideoRequest
import com.example.vibeclip_frontend.data.model.VideoResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

class VideoRepository {
    private val apiService = RetrofitClient.apiService
    
    suspend fun getVideos(
        token: String, 
        page: Int = 0, 
        size: Int = 20,
        recommended: Boolean? = null,
        randomPercentage: Double? = null
    ): Result<VideoListResponse> {
        return try {
            val response = apiService.getVideos("Bearer $token", page, size, recommended, randomPercentage)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch videos: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getVideo(token: String, id: String): Result<VideoResponse> {
        return try {
            val response = apiService.getVideo("Bearer $token", id)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch video: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getShareUrl(id: String): Result<String> {
        return try {
            val response = apiService.getShareUrl(id)
            if (response.isSuccessful && response.body() != null) {
                val shareUrl = response.body()!!["shareUrl"]
                if (shareUrl != null) {
                    Result.success(shareUrl)
                } else {
                    Result.failure(Exception("Share URL not found in response"))
                }
            } else {
                Result.failure(Exception("Failed to get share URL: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun createVideo(token: String, request: VideoRequest): Result<VideoResponse> {
        return try {
            val response = apiService.createVideo("Bearer $token", request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create video: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadVideo(
        token: String,
        filePart: MultipartBody.Part,
        thumbnailPart: MultipartBody.Part?,
        title: String?,
        description: String?,
        hashtags: String?,
        durationSeconds: Int
    ): Result<VideoResponse> = runCatching {
        val resp = apiService.uploadVideo(
            token = "Bearer $token",
            file = filePart,
            thumbnail = thumbnailPart,
            title = title,
            description = description,
            hashtags = hashtags,
            durationSeconds = durationSeconds
        )
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
}


