package com.example.vibeclip_frontend.data.model

import com.google.gson.annotations.SerializedName

data class VideoRequest(
    val title: String? = null,
    val description: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val hashtags: Set<String> = emptySet()
)

data class VideoResponse(
    val id: String,
    val title: String,
    val description: String?,
    val videoUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int,
    val status: String,
    val authorId: String?,
    val authorUsername: String?,
    val hashtags: Set<String>,
    val metrics: VideoMetricsResponse?,
    val createdAt: String,
    val updatedAt: String
)

data class VideoMetricsResponse(
    val viewCount: Long,
    val likeCount: Long,
    val commentCount: Long,
    val shareCount: Long
)

data class VideoListResponse(
    val content: List<VideoResponse>,
    val totalElements: Long,
    val totalPages: Int,
    @SerializedName("number")
    val pageNumber: Int,
    val size: Int,
    val last: Boolean? = null,
    val first: Boolean? = null,
    val numberOfElements: Int? = null
)


