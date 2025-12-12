package com.example.vibeclip_frontend.data.model

data class FolderRequest(
    val name: String,
    val description: String? = null,
    val preferences: FolderPreferenceRequest? = null
)

data class FolderResponse(
    val id: String,
    val name: String,
    val description: String?,
    val status: String,
    val ownerId: String?,
    val ownerUsername: String?,
    val preference: FolderPreferenceRequest?,
    val videoCount: Int? = null,
    val createdAt: String,
    val updatedAt: String
)

data class FolderPreferenceRequest(
    val allowedHashtags: List<String> = emptyList(),
    val blockedHashtags: List<String> = emptyList(),
    val allowedAuthors: List<String> = emptyList(),
    val blockedAuthors: List<String> = emptyList(),
    val minDuration: Int? = null,
    val maxDuration: Int? = null,
    val freshnessWeight: Double = 0.5,
    val popularityWeight: Double = 0.5
)

data class FolderPreferenceResponse(
    val allowedHashtags: List<String>,
    val blockedHashtags: List<String>,
    val allowedAuthors: List<String>,
    val blockedAuthors: List<String>,
    val minDuration: Int?,
    val maxDuration: Int?,
    val freshnessWeight: Double,
    val popularityWeight: Double
)

data class FolderFeedResponse(
    val folderId: String,
    val folderName: String,
    val videos: List<FolderVideoResponse>,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int,
    val hasMore: Boolean
)

data class FolderVideoResponse(
    val id: String,
    val position: Int,
    val score: Double,
    val shown: Boolean,
    val addedAt: String?,
    val video: VideoResponse
)


