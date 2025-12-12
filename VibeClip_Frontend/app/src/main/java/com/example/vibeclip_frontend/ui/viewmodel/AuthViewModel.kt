package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.LoginRequest
import com.example.vibeclip_frontend.data.model.RegisterRequest
import com.example.vibeclip_frontend.data.repository.AuthRepository
import com.example.vibeclip_frontend.util.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val token: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState
    
    fun register(email: String, username: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = authRepository.register(
                RegisterRequest(email = email, username = username, password = password)
            )
            
            result.onSuccess { authResponse ->
                tokenManager.saveToken(authResponse.accessToken)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    token = authResponse.accessToken
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = error.message ?: "Registration failed"
                )
            }
        }
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = authRepository.login(
                LoginRequest(email = email, password = password)
            )
            
            result.onSuccess { authResponse ->
                tokenManager.saveToken(authResponse.accessToken)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true,
                    token = authResponse.accessToken
                )
            }.onFailure { error ->
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = false,
                    errorMessage = error.message ?: "Login failed"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(isSuccess = false)
    }
}

