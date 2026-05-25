package com.example.vibeclip_frontend.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.example.vibeclip_frontend.data.model.SubscriberListItem
import com.example.vibeclip_frontend.data.model.UserResponse
import com.example.vibeclip_frontend.data.model.VideoRequest
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.SubscriptionRepository
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.util.buildImagePart
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.SubscribersStore
import com.example.vibeclip_frontend.util.SubscriptionsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val isLoading: Boolean = false,
    val isLoadingVideos: Boolean = false,
    val isSavingProfile: Boolean = false,
    val errorMessage: String? = null,
    val profileSaveError: String? = null,
    val user: UserResponse? = null,
    val videos: List<VideoResponse> = emptyList(),
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val mySubscriptions: List<StoredSubscription> = emptyList(),
    val mySubscribers: List<StoredSubscription> = emptyList(),
    val pendingSubscriberRequests: List<SubscriberListItem> = emptyList(),
    val subscribersCount: Long = 0,
    val privateProfile: Boolean = false,
    val isUpdatingPrivacy: Boolean = false,
    val privacyError: String? = null
)

class ProfileViewModel(
    private val userRepo: UserRepository,
    private val videoRepo: VideoRepository,
    private val subscriptionRepo: SubscriptionRepository,
    private val subscriptionsStore: SubscriptionsStore,
    private val subscribersStore: SubscribersStore,
    private val token: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        load()
        loadVideos()
        loadSubscriptions()
        loadSubscribers()
    }

    fun loadSubscriptions() {
        viewModelScope.launch {
            val outgoing = subscriptionRepo.getOutgoingRequests(token).getOrNull().orEmpty()
            val pendingIds = outgoing.map { it.subscriberId }.toSet()

            val pendingList = mutableListOf<StoredSubscription>()
            val acceptedList = mutableListOf<StoredSubscription>()

            outgoing.forEach { request ->
                val profile = userRepo.getProfile(token, request.username).getOrNull()
                val isPrivate = profile?.privateProfile != false
                val item = StoredSubscription(
                    userId = request.subscriberId,
                    username = request.username,
                    avatarUrl = profile?.avatarUrl,
                    isPending = isPrivate
                )
                if (isPrivate) {
                    subscriptionsStore.add(item)
                    pendingList.add(item)
                } else {
                    val accepted = item.copy(isPending = false)
                    subscriptionsStore.add(accepted)
                    acceptedList.add(accepted)
                }
            }

            subscriptionsStore.getAll()
                .filter { stored ->
                    pendingList.none { it.userId == stored.userId } &&
                        acceptedList.none { it.userId == stored.userId }
                }
                .forEach { stored ->
                    if (stored.userId in pendingIds) return@forEach
                    val profile = userRepo.getProfile(token, stored.username).getOrNull()
                    val isPrivate = profile?.privateProfile != false
                    when {
                        profile?.subscribed == true -> {
                            val updated = stored.copy(
                                avatarUrl = profile.avatarUrl ?: stored.avatarUrl,
                                isPending = false
                            )
                            subscriptionsStore.add(updated)
                            acceptedList.add(updated)
                        }
                        !isPrivate -> {
                            val updated = stored.copy(
                                avatarUrl = profile?.avatarUrl ?: stored.avatarUrl,
                                isPending = false
                            )
                            subscriptionsStore.add(updated)
                            acceptedList.add(updated)
                        }
                        else -> {
                            // Приватный: нет в outgoing и не подписан — заявка отклонена
                            subscriptionsStore.remove(stored.userId)
                        }
                    }
                }

            _uiState.value = _uiState.value.copy(
                mySubscriptions = pendingList + acceptedList
            )
        }
    }

    fun refreshSubscriptions() = loadSubscriptions()

    fun loadSubscribers() {
        viewModelScope.launch {
            val incoming = subscriptionRepo.getIncomingRequests(token).getOrNull().orEmpty()
            val pendingIds = incoming.map { it.subscriberId }.toSet()

            val pending = incoming.map { request ->
                val profile = userRepo.getProfile(token, request.username).getOrNull()
                SubscriberListItem(
                    userId = request.subscriberId,
                    username = request.username,
                    avatarUrl = profile?.avatarUrl,
                    isPending = true
                )
            }

            // Только принятые подписчики; заявки (pending/rejected) не считаем
            val accepted = subscribersStore.getAll()
                .filter { it.userId !in pendingIds }
                .map { stored ->
                    val profile = userRepo.getProfile(token, stored.username).getOrNull()
                    val updated = stored.copy(avatarUrl = profile?.avatarUrl ?: stored.avatarUrl)
                    subscribersStore.add(updated)
                    updated
                }

            _uiState.value = _uiState.value.copy(
                mySubscribers = accepted,
                pendingSubscriberRequests = pending,
                subscribersCount = accepted.size.toLong()
            )
        }
    }

    fun acceptSubscriber(subscriberId: String) {
        viewModelScope.launch {
            subscriptionRepo.acceptRequest(token, subscriberId)
                .onSuccess {
                    val pending = _uiState.value.pendingSubscriberRequests
                        .find { it.userId == subscriberId }
                    if (pending != null) {
                        subscribersStore.add(
                            StoredSubscription(
                                userId = pending.userId,
                                username = pending.username,
                                avatarUrl = pending.avatarUrl
                            )
                        )
                    }
                    loadSubscribers()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = ErrorMessages.messageOnly(e))
                }
        }
    }

    fun rejectSubscriber(subscriberId: String) {
        viewModelScope.launch {
            subscriptionRepo.rejectRequest(token, subscriberId)
                .onSuccess {
                    subscribersStore.remove(subscriberId)
                    loadSubscribers()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = ErrorMessages.messageOnly(e))
                }
        }
    }

    fun enrichSubscriptionsAvatars() {
        viewModelScope.launch {
            loadSubscriptions()
        }
    }

    fun setPrivateProfile(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isUpdatingPrivacy = true,
                privacyError = null
            )
            userRepo.updatePrivacy(token, enabled)
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        privateProfile = enabled,
                        isUpdatingPrivacy = false
                    )
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isUpdatingPrivacy = false,
                        privacyError = ErrorMessages.messageOnly(e)
                    )
                }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            userRepo.me(token)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(isLoading = false, user = user)
                    userRepo.getProfile(token, user.username)
                        .onSuccess { profile ->
                            _uiState.value = _uiState.value.copy(privateProfile = profile.privateProfile)
                            subscriptionsStore.updateAvatar(user.id, profile.avatarUrl)
                            refreshSubscriptions()
                            enrichSubscriptionsAvatars()
                        }
                    loadSubscribers()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = ErrorMessages.messageOnly(e)
                    )
                }
        }
    }

    fun saveAvatar(context: Context, pickedAvatarUri: Uri, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingProfile = true,
                profileSaveError = null
            )

            if (_uiState.value.user == null) return@launch

            val part = buildImagePart(context, pickedAvatarUri, "avatar")
            userRepo.uploadAvatar(token, part)
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isSavingProfile = false,
                        profileSaveError = ErrorMessages.messageOnly(e)
                    )
                    return@launch
                }

            load()
            refreshSubscriptions()
            loadSubscribers()
            _uiState.value = _uiState.value.copy(isSavingProfile = false, profileSaveError = null)
            onSuccess()
        }
    }

    fun loadVideos(page: Int = 0) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingVideos = true, errorMessage = null)
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
                .onFailure {
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
                                errorMessage = ErrorMessages.messageOnly(e2)
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
                    _uiState.value = _uiState.value.copy(
                        videos = _uiState.value.videos.filter { it.id != videoId }
                    )
                    onSuccess()
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = ErrorMessages.messageOnly(e))
                }
        }
    }

    fun updateVideo(videoId: String, request: VideoRequest, onSuccess: (VideoResponse) -> Unit) {
        viewModelScope.launch {
            videoRepo.updateVideo(token, videoId, request)
                .onSuccess { updated ->
                    _uiState.value = _uiState.value.copy(
                        videos = _uiState.value.videos.map { if (it.id == videoId) updated else it }
                    )
                    onSuccess(updated)
                }
                .onFailure { e ->
                    _uiState.value = _uiState.value.copy(errorMessage = ErrorMessages.messageOnly(e))
                }
        }
    }

    fun refresh() {
        load()
        loadVideos(0)
        refreshSubscriptions()
        loadSubscribers()
    }
}
