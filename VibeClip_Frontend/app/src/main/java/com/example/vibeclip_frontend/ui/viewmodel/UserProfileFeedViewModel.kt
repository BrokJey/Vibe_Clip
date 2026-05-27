package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class UserProfileFeedUiState(
    val isLoading: Boolean = false,
    val videos: List<VideoResponse> = emptyList(),
    val username: String = "",
    val errorMessage: String? = null
)

class UserProfileFeedViewModel(
    private val userRepository: UserRepository,
    private val token: String,
    private val username: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileFeedUiState())
    val uiState: StateFlow<UserProfileFeedUiState> = _uiState

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            userRepository.getProfile(token, username)
                .onSuccess { profile ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        videos = profile.videos.filter { it.status == "PUBLISHED" },
                        username = profile.username
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
}
