package com.example.vibeclip_frontend.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Локальное состояние жалоб: бэкенд не отдаёт reportCount / reportedByMe / список жалобщиков в API видео.
 * Синхронизируем на устройстве при POST/DELETE report и используем для UI и админ-списка.
 */
class ReportTrackerStore(context: Context) {
    private val prefs = context.getSharedPreferences("VibeClipPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun isReported(userKey: String, videoId: String): Boolean {
        if (userKey.isBlank() || videoId.isBlank()) return false
        return loadMyReports()[userKey]?.contains(videoId) == true
    }

    fun getCount(videoId: String): Long {
        if (videoId.isBlank()) return 0L
        return loadCounts()[videoId] ?: 0L
    }

    fun getReporterUsernames(videoId: String): List<String> {
        if (videoId.isBlank()) return emptyList()
        return loadReporters()[videoId]?.toList().orEmpty().sorted()
    }

    fun onReport(userKey: String, videoId: String, reporterUsername: String) {
        if (videoId.isBlank()) return
        val myReports = loadMyReports().toMutableMap()
        val videos = myReports.getOrPut(userKey) { mutableSetOf() }.toMutableSet()
        val wasAlreadyMine = videos.contains(videoId)
        videos.add(videoId)
        myReports[userKey] = videos
        saveMyReports(myReports)

        if (!wasAlreadyMine) {
            val counts = loadCounts().toMutableMap()
            counts[videoId] = (counts[videoId] ?: 0L) + 1L
            saveCounts(counts)
        }

        if (reporterUsername.isNotBlank()) {
            val reporters = loadReporters().toMutableMap()
            val names = reporters.getOrPut(videoId) { mutableSetOf() }.toMutableSet()
            names.add(reporterUsername.trim())
            reporters[videoId] = names
            saveReporters(reporters)
        }
    }

    fun onWithdraw(userKey: String, videoId: String, reporterUsername: String) {
        if (videoId.isBlank()) return
        val myReports = loadMyReports().toMutableMap()
        val videos = myReports[userKey]?.toMutableSet() ?: return
        if (!videos.remove(videoId)) return
        if (videos.isEmpty()) myReports.remove(userKey) else myReports[userKey] = videos
        saveMyReports(myReports)

        val counts = loadCounts().toMutableMap()
        val next = ((counts[videoId] ?: 0L) - 1L).coerceAtLeast(0L)
        if (next == 0L) counts.remove(videoId) else counts[videoId] = next
        saveCounts(counts)

        if (reporterUsername.isNotBlank()) {
            val reporters = loadReporters().toMutableMap()
            val names = reporters[videoId]?.toMutableSet() ?: return
            names.remove(reporterUsername.trim())
            if (names.isEmpty()) reporters.remove(videoId) else reporters[videoId] = names
            saveReporters(reporters)
        }
    }

    /** Синхронизация UI, если сервер уже знает о жалобе, а локальный кэш — нет. */
    fun syncReportedByServer(userKey: String, videoId: String, reporterUsername: String) {
        val myReports = loadMyReports().toMutableMap()
        val videos = myReports.getOrPut(userKey) { mutableSetOf() }.toMutableSet()
        videos.add(videoId)
        myReports[userKey] = videos
        saveMyReports(myReports)

        if (reporterUsername.isBlank()) return
        val reporters = loadReporters().toMutableMap()
        val names = reporters.getOrPut(videoId) { mutableSetOf() }.toMutableSet()
        val isNewReporter = names.add(reporterUsername.trim())
        reporters[videoId] = names
        saveReporters(reporters)

        if (isNewReporter) {
            val counts = loadCounts().toMutableMap()
            counts[videoId] = (counts[videoId] ?: 0L) + 1L
            saveCounts(counts)
        }
    }

    fun clearVideo(videoId: String) {
        if (videoId.isBlank()) return
        val myReports = loadMyReports().mapValues { (_, ids) ->
            ids.toMutableSet().apply { remove(videoId) }
        }.filterValues { it.isNotEmpty() }
        saveMyReports(myReports)

        val counts = loadCounts().toMutableMap()
        counts.remove(videoId)
        saveCounts(counts)

        val reporters = loadReporters().toMutableMap()
        reporters.remove(videoId)
        saveReporters(reporters)
    }

    private fun loadMyReports(): Map<String, Set<String>> = readMap(KEY_MY_REPORTS)
    private fun saveMyReports(map: Map<String, Set<String>>) = writeMap(KEY_MY_REPORTS, map)

    private fun loadCounts(): Map<String, Long> {
        val json = prefs.getString(KEY_COUNTS, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Long>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    private fun saveCounts(map: Map<String, Long>) {
        prefs.edit().putString(KEY_COUNTS, gson.toJson(map)).apply()
    }

    private fun loadReporters(): Map<String, Set<String>> = readMap(KEY_REPORTERS)
    private fun saveReporters(map: Map<String, Set<String>>) = writeMap(KEY_REPORTERS, map)

    private fun readMap(key: String): Map<String, Set<String>> {
        val json = prefs.getString(key, null) ?: return emptyMap()
        val type = object : TypeToken<Map<String, Set<String>>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    private fun writeMap(key: String, map: Map<String, Set<String>>) {
        prefs.edit().putString(key, gson.toJson(map)).apply()
    }

    companion object {
        private const val KEY_MY_REPORTS = "video_report_my_reports"
        private const val KEY_COUNTS = "video_report_counts"
        private const val KEY_REPORTERS = "video_report_reporters"
    }
}
