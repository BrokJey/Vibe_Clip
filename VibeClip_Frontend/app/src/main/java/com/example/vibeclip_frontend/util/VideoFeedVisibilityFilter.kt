package com.example.vibeclip_frontend.util

import com.example.vibeclip_frontend.data.model.FolderVideoResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.model.isPublishedForFeed
import com.example.vibeclip_frontend.data.repository.UserRepository

/**
 * Фильтр видимости в общей ленте и папках.
 * Повторяет логику бэкенда [canViewVideo] / [canViewProfile]:
 * публичный профиль — видно всем; приватный — только автору и принятым подписчикам.
 *
 * GET /videos и fallback в /videos/feed на бэкенде отдают все PUBLISHED без этой проверки.
 */
class VideoFeedVisibilityFilter(
    private val userRepository: UserRepository,
    private val subscriptionsStore: SubscriptionsStore
) {
    private val authorIsPrivate = mutableMapOf<String, Boolean>()

    suspend fun filterVisible(token: String, videos: List<VideoResponse>): List<VideoResponse> {
        if (videos.isEmpty()) return videos

        val me = userRepository.me(token).getOrNull()
        val meId = me?.id
        val acceptedSubs = subscriptionsStore.getAll()
            .filter { !it.isPending }
            .map { it.userId }
            .toMutableSet()

        val authorsToLoad = videos
            .mapNotNull { video ->
                val authorId = video.authorId ?: return@mapNotNull null
                val username = video.authorUsername ?: return@mapNotNull null
                if (authorId == meId || authorId in acceptedSubs) return@mapNotNull null
                if (authorIsPrivate.containsKey(authorId)) return@mapNotNull null
                authorId to username
            }
            .distinctBy { it.first }

        authorsToLoad.forEach { (authorId, username) ->
            val profile = userRepository.getProfile(token, username).getOrNull()
            if (profile == null) {
                authorIsPrivate[authorId] = true
            } else {
                authorIsPrivate[authorId] = profile.privateProfile
                if (profile.privateProfile && profile.subscribed) {
                    acceptedSubs.add(authorId)
                }
            }
        }

        return videos.filter { video ->
            isVisibleForFeed(video, meId, acceptedSubs)
        }
    }

    suspend fun filterFolderVideos(
        token: String,
        items: List<FolderVideoResponse>
    ): List<FolderVideoResponse> {
        if (items.isEmpty()) return items
        val visibleIds = filterVisible(token, items.map { it.video }).map { it.id }.toSet()
        return items.filter { it.video.id in visibleIds }
    }

    fun invalidateCache() {
        authorIsPrivate.clear()
    }

    private fun isVisibleForFeed(
        video: VideoResponse,
        meId: String?,
        acceptedSubs: Set<String>
    ): Boolean {
        if (!video.isPublishedForFeed()) return false

        val authorId = video.authorId ?: return true
        if (meId != null && authorId == meId) return true
        if (authorId in acceptedSubs) return true

        val isPrivate = authorIsPrivate[authorId] ?: false
        return !isPrivate
    }
}
