package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.ModerationVideoItem
import com.example.vibeclip_frontend.data.repository.AdminModerationRepository
import com.example.vibeclip_frontend.util.ErrorMessages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AdminModerationUiState(
    val isLoading: Boolean = false,
    val isActionLoading: Boolean = false,
    val items: List<ModerationVideoItem> = emptyList(),
    val errorMessage: String? = null
) {
    val notificationsCount: Int get() = items.size
}

class AdminModerationViewModel(
    private val repository: AdminModerationRepository,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminModerationUiState())
    val uiState: StateFlow<AdminModerationUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repository.getReportedVideos(token)
                .onSuccess { items ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        items = items,
                        errorMessage = null
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

    fun rejectReports(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            repository.rejectReports(token, videoId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        items = _uiState.value.items.filterNot { it.id == videoId }
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        errorMessage = ErrorMessages.messageOnly(e)
                    )
                }
        }
    }

    fun blockVideo(videoId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            repository.deleteVideo(token, videoId)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        items = _uiState.value.items.filterNot { it.id == videoId }
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(
                        isActionLoading = false,
                        errorMessage = "Не удалось удалить видео."
                    )
                }
        }
    }

    fun clearAllReports() {
        val ids = _uiState.value.items.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isActionLoading = true, errorMessage = null)
            var failed = false
            ids.forEach { id ->
                val result = repository.rejectReports(token, id)
                if (result.isFailure) failed = true
            }
            if (failed) {
                load()
                _uiState.value = _uiState.value.copy(
                    isActionLoading = false,
                    errorMessage = "Не удалось сбросить часть жалоб. Проверьте список."
                )
            } else {
                _uiState.value = _uiState.value.copy(isActionLoading = false, items = emptyList())
            }
        }
    }
}
