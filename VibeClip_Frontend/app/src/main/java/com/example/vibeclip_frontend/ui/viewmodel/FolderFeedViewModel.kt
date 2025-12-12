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
    val errorMessage: String? = null
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

    fun loadPage(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repo.feed(token, folderId, page, 10)
                .onSuccess { resp ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        videos = if (page == 0) resp.videos else _uiState.value.videos + resp.videos,
                        page = resp.page,
                        hasMore = resp.hasMore
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadPage(_uiState.value.page + 1)
        }
    }
}


