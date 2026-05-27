package com.example.vibeclip_frontend.data.model

data class ReportedUserResponse(
    val id: String? = null,
    val username: String? = null
)

data class VideoReportAdminResponse(
    val id: String? = null,
    val reporter: ReportedUserResponse? = null,
    val reporterId: String? = null,
    val reporterUsername: String? = null
)

data class ReportedVideoAdminResponse(
    val id: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val authorId: String? = null,
    val authorUsername: String? = null,
    val reportCount: Long? = null,
    val reports: List<VideoReportAdminResponse>? = null
)

data class ModerationVideoItem(
    val id: String,
    val title: String,
    val thumbnailUrl: String?,
    val authorId: String?,
    val authorUsername: String,
    val reportCount: Long,
    val reporters: List<ReportedUserResponse> = emptyList()
)
