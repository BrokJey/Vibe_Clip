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
                val currentVideos = _uiState.value.videos
                val newVideos = if (page == 0) {
                    // При первой загрузке заменяем список, но защищаемся от уменьшения
                    // Если новый список меньше текущего и текущий не пустой, оставляем текущий
                    if (response.content.isEmpty() && currentVideos.isNotEmpty()) {
                        currentVideos // Сохраняем старый список, если новый пустой
                    } else if (response.content.size < currentVideos.size && currentVideos.size >= 3) {
                        // Если новый список значительно меньше текущего, оставляем текущий
                        currentVideos
                    } else {
                        response.content
                    }
                } else {
                    // При загрузке следующих страниц добавляем только новые видео (без дубликатов)
                    val existingIds = currentVideos.map { it.id }.toSet()
                    val newContent = response.content.filter { it.id !in existingIds }
                    // Всегда добавляем новые видео к существующим, список только увеличивается
                    currentVideos + newContent
                }
                
                // Защита: не позволяем списку уменьшиться
                val finalVideos = if (newVideos.size < currentVideos.size && currentVideos.isNotEmpty()) {
                    // Если список уменьшился, оставляем старый список
                    currentVideos
                } else {
                    newVideos
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = finalVideos,
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
        // При обновлении загружаем первую страницу, но сохраняем существующие видео, если их больше
        val currentVideos = _uiState.value.videos
        loadVideos(0)
        // Если после загрузки список стал меньше, восстанавливаем старый список
        // (это обработается в loadVideos через проверку размера)
    }
    
    fun addVideoToStart(video: VideoResponse) {
        val currentVideos = _uiState.value.videos.toMutableList()
        // Проверяем, нет ли уже этого видео в списке
        if (currentVideos.none { it.id == video.id }) {
            currentVideos.add(0, video)
            _uiState.value = _uiState.value.copy(videos = currentVideos)
        }
    }
}

