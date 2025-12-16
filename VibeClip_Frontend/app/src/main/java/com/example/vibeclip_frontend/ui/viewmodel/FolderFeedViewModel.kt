package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.FolderVideoResponse
import com.example.vibeclip_frontend.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
    private val folderId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderFeedUiState())
    val uiState: StateFlow<FolderFeedUiState> = _uiState

    init {
        loadPage()
    }

    fun loadPage(limit: Int = 20) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repo.feed(token, folderId, limit)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        videos = resp.videos,
                        page = resp.page,
                        hasMore = resp.hasMore,
                        folderName = resp.folderName
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
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


