package com.example.vibeclip_frontend.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.example.vibeclip_frontend.data.model.UserProfileResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.SubscriptionRepository
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.util.SubscriptionsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val profile: UserProfileResponse? = null,
    val isOwnProfile: Boolean = false,
    val isSubscribed: Boolean = false,
    val isPending: Boolean = false,
    val isSubscriptionActionInProgress: Boolean = false,
    val videos: List<VideoResponse> = emptyList()
)

class UserProfileViewModel(
    private val userRepository: UserRepository = AppModule.userRepository,
    private val subscriptionRepository: SubscriptionRepository = AppModule.subscriptionRepository,
    private val subscriptionsStore: SubscriptionsStore = AppModule.subscriptionsStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    private var token: String = ""
    private var currentUsername: String = ""

    fun init(token: String, targetUsername: String) {
        this.token = token
        this.currentUsername = targetUsername
        loadProfile()
    }

    fun loadProfile() {
        if (token.isBlank() || currentUsername.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

            val meResult = userRepository.me(token)
            val me = meResult.getOrNull()
            val meUsername = me?.username
            val isOwnProfile = meUsername != null && meUsername.equals(currentUsername, ignoreCase = true)

            val profileResult = userRepository.getProfile(token, currentUsername)
            val outgoingResult = subscriptionRepository.getOutgoingRequests(token)
            val outgoing = outgoingResult.getOrNull().orEmpty()
            val pendingTargetIds = outgoing.map { it.subscriberId }.toSet()

            profileResult
                .onSuccess { profile ->
                    val isOwnProfileById = me != null && me.id == profile.id
                    val isPending = pendingTargetIds.contains(profile.id)
                    _uiState.value = UserProfileUiState(
                        isLoading = false,
                        profile = profile,
                        isOwnProfile = isOwnProfile || isOwnProfileById,
                        isSubscribed = profile.subscribed,
                        isPending = isPending && !profile.subscribed,
                        videos = profile.videos
                    )
                    if (profile.subscribed || isPending) {
                        subscriptionsStore.add(
                            StoredSubscription(
                                userId = profile.id,
                                username = profile.username,
                                avatarUrl = profile.avatarUrl,
                                isPending = isPending && !profile.subscribed
                            )
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Не удалось загрузить профиль"
                    )
                }
        }
    }

    fun toggleSubscription() {
        val profile = _uiState.value.profile ?: return
        if (_uiState.value.isOwnProfile || _uiState.value.isSubscriptionActionInProgress) return

        viewModelScope.launch {
            val me = userRepository.me(token).getOrNull()
            if (me != null && me.id == profile.id) {
                _uiState.value = _uiState.value.copy(
                    isOwnProfile = true,
                    errorMessage = "Нельзя подписаться на самого себя"
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isSubscriptionActionInProgress = true,
                errorMessage = null
            )

            val wasLinked = _uiState.value.isSubscribed || _uiState.value.isPending
            val result = if (wasLinked) {
                subscriptionRepository.unsubscribe(token, profile.id)
            } else {
                subscriptionRepository.subscribe(token, profile.id)
            }

            result
                .onSuccess {
                    if (wasLinked) {
                        subscriptionsStore.remove(profile.id)
                    } else {
                        subscriptionsStore.add(
                            StoredSubscription(
                                userId = profile.id,
                                username = profile.username,
                                avatarUrl = profile.avatarUrl,
                                isPending = true
                            )
                        )
                    }
                    loadProfile()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        errorMessage = e.message ?: "Ошибка подписки",
                        isSubscriptionActionInProgress = false
                    )
                }

            _uiState.value = _uiState.value.copy(isSubscriptionActionInProgress = false)
        }
    }
}
