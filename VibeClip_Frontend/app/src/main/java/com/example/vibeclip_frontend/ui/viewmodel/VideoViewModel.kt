package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.util.ErrorMessages
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
    private val token: String
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

            // recommended=true включает персональные рекомендации на основе лайков пользователя
            // randomPercentage=0.25 означает 25% случайных видео для разнообразия
            val result = videoRepository.getVideos(
                token = token,
                page = page,
                size = 20,
                recommended = true,
                randomPercentage = 0.25
            )

            result.onSuccess { response ->
                if (myToken != requestToken) return@onSuccess

                val publishedVideos = response.content.filter { it.status == "PUBLISHED" }

                val finalVideos = if (page == 0) {
                    // Полная замена на первой странице + перемешивание для смены порядка
                    publishedVideos.shuffled(Random(System.nanoTime()))
                } else {
                    val currentVideos = _uiState.value.videos
                    val existingIds = currentVideos.map { it.id }.toSet()
                    val additional = publishedVideos.filter { it.id !in existingIds }
                    currentVideos + additional
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = finalVideos,
                    currentPage = response.pageNumber,
                    hasMore = response.pageNumber < response.totalPages - 1
                )
            }.onFailure { error ->
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

    /** Полная перезагрузка ленты с сервера (как при новом входе в аккаунт). */
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

            val result = videoRepository.getVideos(
                token = token,
                page = 0,
                size = 20,
                recommended = true,
                randomPercentage = 0.25
            )

            result.onSuccess { response ->
                if (myToken != requestToken) return@onSuccess

                val publishedVideos = response.content.filter { it.status == "PUBLISHED" }
                var shuffled = publishedVideos.shuffled(Random(System.nanoTime()))
                var attempts = 0
                while (
                    previousFirstId != null &&
                    shuffled.firstOrNull()?.id == previousFirstId &&
                    shuffled.size > 1 &&
                    attempts < 5
                ) {
                    shuffled = publishedVideos.shuffled(Random(System.nanoTime()))
                    attempts++
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    videos = shuffled,
                    currentPage = response.pageNumber,
                    hasMore = response.pageNumber < response.totalPages - 1,
                    reloadNonce = baseNonce + 1
                )
            }.onFailure { error ->
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
        if (video.status != "PUBLISHED") return
        val currentVideos = _uiState.value.videos.toMutableList()
        // Проверяем, нет ли уже этого видео в списке
        if (currentVideos.none { it.id == video.id }) {
            currentVideos.add(0, video)
            _uiState.value = _uiState.value.copy(videos = currentVideos)
        }
    }
}

