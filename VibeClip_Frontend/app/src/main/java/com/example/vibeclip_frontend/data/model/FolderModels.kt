package com.example.vibeclip_frontend.data.model

data class FolderRequest(
    val name: String,
    val description: String? = null,
    // Имя поля должно совпадать с backend DTO: preference
    val preference: FolderPreferenceRequest? = null
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
    val allowedHashtags: Set<String>? = null,
    val blockedHashtags: Set<String>? = null,
    val allowedAuthorIds: Set<String>? = null,
    val blockedAuthorIds: Set<String>? = null,
    val minDurationSeconds: Int? = null,
    val maxDurationSeconds: Int? = null,
    val freshnessWeight: Double? = null,
    val popularityWeight: Double? = null
)

data class FolderPreferenceResponse(
    val allowedHashtags: Set<String>?,
    val blockedHashtags: Set<String>?,
    val allowedAuthorIds: Set<String>?,
    val blockedAuthorIds: Set<String>?,
    val minDurationSeconds: Int?,
    val maxDurationSeconds: Int?,
    val freshnessWeight: Double?,
    val popularityWeight: Double?
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


