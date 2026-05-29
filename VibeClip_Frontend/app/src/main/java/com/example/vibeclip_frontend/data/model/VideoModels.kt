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
    val title: String? = null,
    val description: String? = null,
    val videoUrl: String? = null,
    val thumbnailUrl: String? = null,
    val durationSeconds: Int? = null,
    val status: String? = null,
    val authorId: String? = null,
    val authorUsername: String? = null,
    val hashtags: Set<String>? = null,
    val metrics: VideoMetricsResponse? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

/** Совпадает с бэкендом: viewCount, likeCount, commentCount, shareCount. */
data class VideoMetricsResponse(
    val viewCount: Long = 0,
    val likeCount: Long = 0,
    val commentCount: Long = 0,
    val shareCount: Long = 0
)

fun VideoMetricsResponse.withLikeCount(count: Long) = copy(likeCount = count.coerceAtLeast(0))

fun VideoMetricsResponse.withCommentCount(count: Long) = copy(commentCount = count.coerceAtLeast(0))

/** В ленту попадают только опубликованные ролики (жалобы не меняют status видео). */
fun VideoResponse.isPublishedForFeed(): Boolean {
    val normalized = status?.uppercase()?.trim() ?: return true
    return normalized == "PUBLISHED"
}

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


