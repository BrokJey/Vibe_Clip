package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.ReportTrackerStore

sealed class ReportAction {
    data object Reported : ReportAction()
    data object Withdrawn : ReportAction()
    data object AlreadyReported : ReportAction()
}

class ReportRepository(
    private val reportTracker: ReportTrackerStore
) {
    private val apiService = RetrofitClient.apiService

    suspend fun reportVideo(
        token: String,
        videoId: String,
        userKey: String,
        reporterUsername: String,
        reason: String = DEFAULT_REASON
    ): Result<ReportAction> = runCatching {
        val response = apiService.reportVideo("Bearer $token", videoId, reason)
        when {
            response.isSuccessful -> {
                reportTracker.onReport(userKey, videoId, reporterUsername)
                ReportAction.Reported
            }
            response.code() == 400 -> {
                val message = response.errorBody()?.string().orEmpty()
                if (message.contains("уже жаловались", ignoreCase = true)) {
                    reportTracker.syncReportedByServer(userKey, videoId, reporterUsername)
                    ReportAction.AlreadyReported
                } else {
                    throw Exception(
                        ErrorMessages.messageFromHttp(response.code(), message)
                    )
                }
            }
            else -> throw Exception(
                ErrorMessages.messageFromHttp(response.code(), response.errorBody()?.string())
            )
        }
    }

    suspend fun withdrawReport(
        token: String,
        videoId: String,
        userKey: String,
        reporterUsername: String
    ): Result<ReportAction> = runCatching {
        val response = apiService.withdrawReport("Bearer $token", videoId)
        when {
            response.isSuccessful -> {
                reportTracker.onWithdraw(userKey, videoId, reporterUsername)
                ReportAction.Withdrawn
            }
            response.code() == 400 -> {
                val message = response.errorBody()?.string().orEmpty()
                if (message.contains("не жаловались", ignoreCase = true)) {
                    reportTracker.onWithdraw(userKey, videoId, reporterUsername)
                    ReportAction.Withdrawn
                } else {
                    throw Exception(
                        ErrorMessages.messageFromHttp(response.code(), message)
                    )
                }
            }
            else -> throw Exception(
                ErrorMessages.messageFromHttp(response.code(), response.errorBody()?.string())
            )
        }
    }

    fun isReportedLocally(userKey: String, videoId: String): Boolean =
        reportTracker.isReported(userKey, videoId)

    fun localReportCount(videoId: String): Long = reportTracker.getCount(videoId)

    fun localReporters(videoId: String): List<String> = reportTracker.getReporterUsernames(videoId)

    companion object {
        const val DEFAULT_REASON = "spam"
    }
}
