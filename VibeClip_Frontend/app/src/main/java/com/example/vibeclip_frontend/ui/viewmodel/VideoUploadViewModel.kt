package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.VideoRepository
import okhttp3.MultipartBody
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VideoUploadUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val created: VideoResponse? = null
)

class VideoUploadViewModel(
    private val repo: VideoRepository,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoUploadUiState())
    val uiState: StateFlow<VideoUploadUiState> = _uiState

    fun upload(
        title: String,
        description: String?,
        hashtags: String?,
        duration: Int,
        filePart: MultipartBody.Part,
        thumbnailPart: MultipartBody.Part
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, created = null)
            repo.uploadVideo(
                token = token,
                filePart = filePart,
                thumbnailPart = thumbnailPart,
                title = title,
                description = description?.takeIf { it.isNotBlank() },
                hashtags = hashtags?.takeIf { it.isNotBlank() },
                durationSeconds = duration
            ).onSuccess { video ->
                _uiState.value = _uiState.value.copy(isLoading = false, created = video)
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }
}

