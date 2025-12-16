package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.UserResponse
import com.example.vibeclip_frontend.data.model.VideoRequest
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoadingVideos: Boolean = false,
    val errorMessage: String? = null,
    val user: UserResponse? = null,
    val videos: List<VideoResponse> = emptyList(),
    val currentPage: Int = 0,
    val hasMore: Boolean = true
)

class ProfileViewModel(
    private val userRepo: UserRepository,
    private val videoRepo: VideoRepository,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        load()
        loadVideos()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            userRepo.me(token)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, user = user)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }
    
    fun loadVideos(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVideos = true, errorMessage = null)
            // Пробуем запросить со статусом PUBLISHED, так как большинство видео имеют этот статус
            // Если нужно получить все видео, включая PENDING и другие, можно убрать фильтр
            videoRepo.getMyVideos(token, page, 20, "PUBLISHED")
                .onSuccess { response ->
                    _uiState.value = _uiState.value.copy(
                        isLoadingVideos = false,
                        videos = if (page == 0) response.content else _uiState.value.videos + response.content,
                        currentPage = response.pageNumber,
                        hasMore = response.pageNumber < response.totalPages - 1,
                        errorMessage = null
                    )
                }
                .onFailure { e ->
                    // Если запрос с PUBLISHED не удался, пробуем без фильтра (null)
                    videoRepo.getMyVideos(token, page, 20, null)
                        .onSuccess { response ->
                            _uiState.value = _uiState.value.copy(
                                isLoadingVideos = false,
                                videos = if (page == 0) response.content else _uiState.value.videos + response.content,
                                currentPage = response.pageNumber,
                                hasMore = response.pageNumber < response.totalPages - 1,
                                errorMessage = null
                            )
                        }
                        .onFailure { e2 ->
                            _uiState.value = _uiState.value.copy(
                                isLoadingVideos = false, 
                                errorMessage = e2.message ?: "Не удалось загрузить видео"
                            )
                        }
                }
        }
    }
    
    fun loadMore() {
        if (!_uiState.value.isLoadingVideos && _uiState.value.hasMore) {
            loadVideos(_uiState.value.currentPage + 1)
        }
    }
    
    fun deleteVideo(videoId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            videoRepo.deleteVideo(token, videoId)
                .onSuccess {
                    // Удаляем видео из списка после успешного удаления
                    _uiState.value = _uiState.value.copy(
                        videos = _uiState.value.videos.filter { it.id != videoId }
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }
    
    fun updateVideo(videoId: String, request: VideoRequest, onSuccess: (VideoResponse) -> Unit) {
        viewModelScope.launch {
            videoRepo.updateVideo(token, videoId, request)
                .onSuccess { updated ->
                    // Обновляем видео в списке, заменяя старое на обновленное
                    _uiState.value = _uiState.value.copy(
                        videos = _uiState.value.videos.map { if (it.id == videoId) updated else it }
                    )
                    onSuccess(updated)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }
    
    fun refresh() {
        load()
        loadVideos(0)
    }
}

