package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.FolderRequest
import com.example.vibeclip_frontend.data.model.FolderResponse
import com.example.vibeclip_frontend.data.model.FolderPreferenceRequest
import com.example.vibeclip_frontend.data.repository.FolderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class FolderUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val folders: List<FolderResponse> = emptyList(),
    val created: FolderResponse? = null,
    val shuffledFolders: List<FolderResponse> = emptyList() // Рандомизированный список для бесконечной прокрутки
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
                    // Рандомизируем список папок для бесконечной прокрутки
                    val shuffled = folders.shuffled()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false, 
                        folders = folders,
                        shuffledFolders = shuffled
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun create(name: String, description: String, allowedHashtags: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, created = null)
            val preference = if (allowedHashtags.isNotEmpty()) {
                FolderPreferenceRequest(
                    allowedHashtags = allowedHashtags.toSet(),
                    blockedHashtags = emptySet(),
                    allowedAuthorIds = emptySet(),
                    blockedAuthorIds = emptySet(),
                    minDurationSeconds = null,
                    maxDurationSeconds = null,
                    freshnessWeight = 0.5,
                    popularityWeight = 0.5
                )
            } else {
                null
            }

            repo.create(
                token,
                FolderRequest(
                    name = name,
                    description = description.takeIf { it.isNotBlank() },
                    preference = preference
                )
            )
                .onSuccess { folder ->
                    val updatedFolders = listOf(folder) + _uiState.value.folders
                    val shuffled = updatedFolders.shuffled()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        created = folder,
                        folders = updatedFolders,
                        shuffledFolders = shuffled
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun update(folderId: String, name: String, description: String, allowedHashtags: List<String>) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val preference = if (allowedHashtags.isNotEmpty()) {
                FolderPreferenceRequest(
                    allowedHashtags = allowedHashtags.toSet(),
                    blockedHashtags = emptySet(),
                    allowedAuthorIds = emptySet(),
                    blockedAuthorIds = emptySet(),
                    minDurationSeconds = null,
                    maxDurationSeconds = null,
                    freshnessWeight = 0.5,
                    popularityWeight = 0.5
                )
            } else {
                null
            }

            repo.update(
                token,
                folderId,
                FolderRequest(
                    name = name,
                    description = description.takeIf { it.isNotBlank() },
                    preference = preference
                )
            ).onSuccess { updated ->
                val updatedFolders = _uiState.value.folders.map { if (it.id == updated.id) updated else it }
                val shuffled = updatedFolders.shuffled()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    folders = updatedFolders,
                    shuffledFolders = shuffled
                )
            }.onFailure { e ->
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
            }
        }
    }

    fun delete(folderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repo.delete(token, folderId)
                .onSuccess {
                    val updatedFolders = _uiState.value.folders.filterNot { it.id == folderId }
                    val shuffled = updatedFolders.shuffled()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        folders = updatedFolders,
                        shuffledFolders = shuffled
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun archive(folderId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            repo.archive(token, folderId)
                .onSuccess {
                    // Архивированные папки можно скрывать из списка
                    val updatedFolders = _uiState.value.folders.filterNot { it.id == folderId }
                    val shuffled = updatedFolders.shuffled()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        folders = updatedFolders,
                        shuffledFolders = shuffled
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = e.message)
                }
        }
    }

    fun regenerateFeed(folderId: String, limit: Int = 20) {
        viewModelScope.launch {
            // Просто дергаем regenerate, ошибки показываем, список папок не меняем
            repo.regenerateFeed(token, folderId, limit)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = e.message)
                }
        }
    }

    fun clearCreated() {
        _uiState.value = _uiState.value.copy(created = null)
    }
}

