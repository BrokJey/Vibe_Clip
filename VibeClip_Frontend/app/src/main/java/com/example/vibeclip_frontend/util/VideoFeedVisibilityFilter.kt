package com.example.vibeclip_frontend.util

import com.example.vibeclip_frontend.data.model.FolderVideoResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.model.isPublishedForFeed
import com.example.vibeclip_frontend.data.repository.SubscriptionRepository
import com.example.vibeclip_frontend.data.repository.UserRepository

/**
 * Фильтр видимости в общей ленте и папках.
 * Скрывает только видео авторов с подтверждённым приватным профилем
 * (без подписки и не своё видео).
 */
class VideoFeedVisibilityFilter(
    private val userRepository: UserRepository,
    private val subscriptionsStore: SubscriptionsStore,
    private val subscriptionRepository: SubscriptionRepository
) {
    private val authorIsPrivate = mutableMapOf<String, Boolean>()

    suspend fun filterVisible(token: String, videos: List<VideoResponse>): List<VideoResponse> {
        if (videos.isEmpty()) return videos

        val me = userRepository.me(token).getOrNull()
        val meId = me?.id
        val acceptedSubs = buildAcceptedSubscriptionIds(token)

        val authorsToLoad = videos
            .mapNotNull { video ->
                val authorId = video.authorId?.normalizeId() ?: return@mapNotNull null
                val username = video.authorUsername?.trim()?.takeIf { it.isNotEmpty() }
                    ?: return@mapNotNull null
                if (isSameUser(authorId, meId) || authorId in acceptedSubs) return@mapNotNull null
                if (authorIsPrivate.containsKey(authorId)) return@mapNotNull null
                authorId to username
            }
            .distinctBy { it.first }

        authorsToLoad.forEach { (authorId, username) ->
            userRepository.getProfile(token, username)
                .onSuccess { profile ->
                    authorIsPrivate[authorId] = profile.privateProfile
                    if (profile.privateProfile && profile.subscribed) {
                        acceptedSubs.add(authorId)
                    }
                }
            // При ошибке загрузки профиля не кэшируем — не скрываем видео без подтверждения
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

    private suspend fun buildAcceptedSubscriptionIds(token: String): MutableSet<String> {
        val accepted = subscriptionsStore.getAll()
            .filter { !it.isPending }
            .mapNotNull { it.userId.normalizeId() }
            .toMutableSet()

        subscriptionRepository.getFollowing(token)
            .getOrNull()
            .orEmpty()
            .forEach { request ->
                request.subscriberId.normalizeId()?.let { accepted.add(it) }
            }

        return accepted
    }

    private fun isVisibleForFeed(
        video: VideoResponse,
        meId: String?,
        acceptedSubs: Set<String>
    ): Boolean {
        if (!video.isPublishedForFeed()) return false

        val authorId = video.authorId?.normalizeId() ?: return true
        if (isSameUser(authorId, meId)) return true
        if (authorId in acceptedSubs) return true

        // Скрываем только если приватность автора подтверждена через API
        return authorIsPrivate[authorId] != true
    }

    private fun isSameUser(authorId: String, meId: String?): Boolean {
        if (meId == null) return false
        return authorId.equals(meId.trim(), ignoreCase = true)
    }

    private fun String.normalizeId(): String? {
        val normalized = trim()
        return normalized.takeIf { it.isNotEmpty() }
    }
}
