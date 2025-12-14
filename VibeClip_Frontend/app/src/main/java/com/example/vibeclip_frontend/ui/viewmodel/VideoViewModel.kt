package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.VideoListResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VideoUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoResponse> = emptyList(),
    val errorMessage: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

class VideoViewModel(
    private val videoRepository: VideoRepository,
    private val token: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(VideoUiState())
    val uiState: StateFlow<VideoUiState> = _uiState
    
    init {
        loadVideos()
    }
    
    fun loadVideos(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Используем систему рекомендаций для главной ленты
            // recommended=true включает персональные рекомендации на основе лайков пользователя
            // randomPercentage=0.25 означает 25% случайных видео для разнообразия
            val result = videoRepository.getVideos(
                token = token, 
                page = page, 
                size = 20,
                recommended = true, // Включаем систему рекомендаций
                randomPercentage = 0.25 // 25% случайных видео
            )
            
            result.onSuccess { response ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = if (page == 0) response.content else _uiState.value.videos + response.content,
                    currentPage = response.pageNumber,
                    hasMore = response.pageNumber < response.totalPages - 1
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to load videos"
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
        loadVideos(0)
    }
}

