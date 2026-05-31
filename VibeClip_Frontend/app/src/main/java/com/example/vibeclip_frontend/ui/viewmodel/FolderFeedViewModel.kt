package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.FolderVideoResponse
import com.example.vibeclip_frontend.data.model.isPublishedForFeed
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.VideoFeedVisibilityFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

data class FolderFeedUiState(
    val isLoading: Boolean = false,
    val videos: List<FolderVideoResponse> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = true,
    val errorMessage: String? = null,
    val folderName: String = ""
)

class FolderFeedViewModel(
    private val repo: FolderRepository,
    private val token: String,
    private val folderId: String,
    private val visibilityFilter: VideoFeedVisibilityFilter = AppModule.videoFeedVisibilityFilter
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderFeedUiState())
    val uiState: StateFlow<FolderFeedUiState> = _uiState

    init {
        loadPage()
    }

    fun reload() {
        loadPage(limit = 20, shuffle = true)
    }

    fun loadPage(limit: Int = 20, shuffle: Boolean = false) {
        viewModelScope.launch {
            if (shuffle) visibilityFilter.invalidateCache()
            val previousFirstId = _uiState.value.videos.firstOrNull()?.id
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                videos = if (shuffle) emptyList() else _uiState.value.videos,
                page = if (shuffle) 0 else _uiState.value.page,
                hasMore = if (shuffle) true else _uiState.value.hasMore
            )
            repo.feed(token, folderId, limit)
                .onSuccess { resp ->
                    val published = resp.videos.filter { it.video.isPublishedForFeed() }
                    val visible = visibilityFilter.filterFolderVideos(token, published)
                    var finalVideos = if (shuffle) visible.shuffled(Random(System.nanoTime())) else visible
                    if (
                        shuffle &&
                        previousFirstId != null &&
                        finalVideos.firstOrNull()?.id == previousFirstId &&
                        finalVideos.size > 1
                    ) {
                        var attempts = 0
                        while (
                            finalVideos.firstOrNull()?.id == previousFirstId &&
                            finalVideos.size > 1 &&
                            attempts < 5
                        ) {
                            finalVideos = visible.shuffled(Random(System.nanoTime()))
                            attempts++
                        }
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        videos = finalVideos,
                        page = resp.page,
                        hasMore = resp.hasMore,
                        folderName = resp.folderName
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ErrorMessages.messageOnly(e)
                    )
                }
        }
    }

    fun loadMore() {
        // Для папок бэкенд всегда возвращает непоказанные видео
        // Перегенерируем ленту, запрашивая больше видео
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadPage(_uiState.value.videos.size + 20) // Запрашиваем больше видео
        }
    }
}


