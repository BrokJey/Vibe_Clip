package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.ModerationVideoItem
import com.example.vibeclip_frontend.data.model.ReportedUserResponse
import com.example.vibeclip_frontend.util.ReportTrackerStore

class AdminModerationRepository(
    private val reportTracker: ReportTrackerStore
) {
    private val apiService = RetrofitClient.apiService

    suspend fun getReportedVideos(token: String): Result<List<ModerationVideoItem>> = runCatching {
        val response = apiService.getReportedVideos("Bearer $token")
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("Не удалось загрузить список жалоб (код ${response.code()})")
        }

        response.body()!!.map { video ->
            val localReporters = reportTracker.getReporterUsernames(video.id)
                .map { username -> ReportedUserResponse(username = username) }
            val localCount = reportTracker.getCount(video.id)
            val reportCount = maxOf(localCount, localReporters.size.toLong(), 1L)

            ModerationVideoItem(
                id = video.id,
                title = video.title.trim('"', '\'').ifBlank { "Без названия" },
                thumbnailUrl = video.thumbnailUrl,
                authorId = video.authorId,
                authorUsername = video.authorUsername.orEmpty().ifBlank { "unknown" },
                reportCount = reportCount,
                reporters = localReporters
            )
        }
    }

    suspend fun rejectReports(token: String, videoId: String): Result<Unit> = runCatching {
        val response = apiService.rejectVideoReports("Bearer $token", videoId)
        if (response.isSuccessful) {
            reportTracker.clearVideo(videoId)
            Unit
        } else {
            throw Exception("Не удалось отклонить жалобы (код ${response.code()})")
        }
    }

    suspend fun resolveReports(token: String, videoId: String): Result<Unit> = runCatching {
        val response = apiService.resolveVideoReports("Bearer $token", videoId)
        if (response.isSuccessful) {
            reportTracker.clearVideo(videoId)
            Unit
        } else {
            throw Exception("Не удалось обработать жалобы (код ${response.code()})")
        }
    }

    suspend fun deleteVideo(token: String, videoId: String): Result<Unit> = runCatching {
        // Сценарий 1: перед удалением помечаем жалобы как REVIEWED (бэкенд не удаляет строки reports при DELETE видео).
        apiService.resolveVideoReports("Bearer $token", videoId)

        val response = apiService.adminDeleteVideo("Bearer $token", videoId)
        if (response.isSuccessful) {
            reportTracker.clearVideo(videoId)
            Unit
        } else {
            throw Exception("Не удалось удалить видео.")
        }
    }
}
