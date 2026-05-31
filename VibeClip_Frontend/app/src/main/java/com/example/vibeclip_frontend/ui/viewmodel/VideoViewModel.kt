package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.VideoListResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.model.isPublishedForFeed
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.VideoFeedVisibilityFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class VideoUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoResponse> = emptyList(),
    val errorMessage: String? = null,
    val errorShowRetry: Boolean = false,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    /** Увеличивается при полной перезагрузке ленты (сброс пагинатора на UI). */
    val reloadNonce: Int = 0
)

class VideoViewModel(
    private val videoRepository: VideoRepository,
    private val token: String,
    private val visibilityFilter: VideoFeedVisibilityFilter = AppModule.videoFeedVisibilityFilter
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState

    private var requestToken: Int = 0

    init {
        loadVideos()
    }

    fun loadVideos(page: Int = 0) {
        val myToken = ++requestToken
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                errorShowRetry = false,
                videos = if (page == 0) emptyList() else _uiState.value.videos,
                currentPage = if (page == 0) 0 else _uiState.value.currentPage,
                hasMore = if (page == 0) true else _uiState.value.hasMore
            )

            fetchFeedPage(page, FEED_PAGE_SIZE)
                .onSuccess { (videos, response, hasMore) ->
                    if (myToken != requestToken) return@onSuccess
                    if (page == 0) visibilityFilter.invalidateCache()
                    applyFeedPage(page, videos, response, hasMore)
                }
                .onFailure { error ->
                    if (myToken != requestToken) return@onFailure
                    val err = ErrorMessages.fromThrowable(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message,
                        errorShowRetry = err.showRetry
                    )
                }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadVideos(_uiState.value.currentPage + 1)
        }
    }

    fun refresh() {
        reloadFeed()
    }

    fun reloadFeed() {
        val myToken = ++requestToken
        viewModelScope.launch {
            val baseNonce = _uiState.value.reloadNonce
            val previousFirstId = _uiState.value.videos.firstOrNull()?.id
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                errorShowRetry = false,
                videos = emptyList(),
                currentPage = 0,
                hasMore = true
            )

            fetchFeedPage(0, FEED_PAGE_SIZE)
                .onSuccess { (videos, response, hasMore) ->
                    if (myToken != requestToken) return@onSuccess
                    visibilityFilter.invalidateCache()

                    var shuffled = videos.shuffled(Random(System.nanoTime()))
                    var attempts = 0
                    while (
                        previousFirstId != null &&
                        shuffled.firstOrNull()?.id == previousFirstId &&
                        shuffled.size > 1 &&
                        attempts < 5
                    ) {
                        shuffled = videos.shuffled(Random(System.nanoTime()))
                        attempts++
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        videos = shuffled,
                        currentPage = response.pageNumber,
                        hasMore = hasMore,
                        reloadNonce = baseNonce + 1
                    )
                }
                .onFailure { error ->
                    if (myToken != requestToken) return@onFailure
                    val err = ErrorMessages.fromThrowable(error)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = err.message,
                        errorShowRetry = err.showRetry
                    )
                }
        }
    }

    fun addVideoToStart(video: VideoResponse) {
        viewModelScope.launch {
            val visible = visibilityFilter.filterVisible(token, listOf(video))
            if (visible.isEmpty()) return@launch
            val allowed = visible.first()
            val currentVideos = _uiState.value.videos.toMutableList()
            if (currentVideos.none { it.id == allowed.id }) {
                currentVideos.add(0, allowed)
                _uiState.value = _uiState.value.copy(videos = currentVideos)
            }
        }
    }

    /**
     * Страница 0: смешанная лента /videos/feed + дополнение из полного каталога PUBLISHED.
     * Страницы 1+: только GET /videos без recommended (полная пагинация по БД).
     *
     * Не используем recommended=true: на бэкенде выборка ограничена ~size*10 роликами
     * и часть PUBLISHED-видео (в т.ч. после reject-reports) может не попасть в ленту.
     */
    private suspend fun fetchFeedPage(
        page: Int,
        size: Int
    ): Result<Triple<List<VideoResponse>, VideoListResponse, Boolean>> = runCatching {
        if (page == 0) {
            val mixed = videoRepository.getMixedFeed(token, 0, size).getOrNull()
            val catalog = videoRepository.getPublishedFeed(token, 0, size).getOrThrow()

            val mixedVideos = mixed?.content.orEmpty().filter { it.isPublishedForFeed() }
            val mixedIds = mixedVideos.map { it.id }.toSet()
            val catalogVideos = catalog.content.filter { it.isPublishedForFeed() }
            val extraFromCatalog = catalogVideos.filter { it.id !in mixedIds }
            val merged = (mixedVideos + extraFromCatalog).distinctBy { it.id }

            val videos = if (merged.isNotEmpty()) merged else catalogVideos
            val visible = visibilityFilter.filterVisible(token, videos)
            val hasMore = catalog.last == false ||
                catalog.pageNumber < catalog.totalPages - 1 ||
                videos.isNotEmpty()

            Triple(visible, catalog, hasMore)
        } else {
            val catalog = videoRepository.getPublishedFeed(token, page, size).getOrThrow()
            val videos = catalog.content.filter { it.isPublishedForFeed() }
            val visible = visibilityFilter.filterVisible(token, videos)
            val hasMore = catalog.last == false || catalog.pageNumber < catalog.totalPages - 1
            Triple(visible, catalog, hasMore)
        }
    }

    private fun applyFeedPage(
        page: Int,
        incoming: List<VideoResponse>,
        response: VideoListResponse,
        hasMore: Boolean
    ) {
        val finalVideos = if (page == 0) {
            incoming.shuffled(Random(System.nanoTime()))
        } else {
            val existingIds = _uiState.value.videos.map { it.id }.toSet()
            _uiState.value.videos + incoming.filter { it.id !in existingIds }
        }

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            videos = finalVideos,
            currentPage = response.pageNumber,
            hasMore = hasMore
        )
    }

    companion object {
        private const val FEED_PAGE_SIZE = 20
    }
}
