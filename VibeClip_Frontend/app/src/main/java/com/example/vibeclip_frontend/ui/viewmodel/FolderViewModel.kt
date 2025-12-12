package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.FolderFeedResponse
import com.example.vibeclip_frontend.data.model.FolderRequest
import com.example.vibeclip_frontend.data.model.FolderResponse
import com.example.vibeclip_frontend.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FolderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val folders: List<FolderResponse> = emptyList(),
    val created: FolderResponse? = null
)

class FolderViewModel(
    private val repo: FolderRepository,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(FolderUiState())
    val uiState: StateFlow<FolderUiState> = _uiState

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repo.list(token)
                .onSuccess { folders ->
                    _uiState.value = _uiState.value.copy(isLoading = false, folders = folders)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun create(name: String, description: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, created = null)
            repo.create(token, FolderRequest(name = name, description = description.takeIf { it.isNotBlank() }))
                .onSuccess { folder ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        created = folder,
                        folders = listOf(folder) + _uiState.value.folders
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun clearCreated() {
        _uiState.value = _uiState.value.copy(created = null)
    }
}

