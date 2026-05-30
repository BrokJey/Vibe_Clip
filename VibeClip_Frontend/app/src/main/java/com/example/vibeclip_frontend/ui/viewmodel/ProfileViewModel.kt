package com.example.vibeclip_frontend.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.example.vibeclip_frontend.data.model.SubscriberListItem
import com.example.vibeclip_frontend.data.model.SubscriptionRequestResponse
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
    val subscriptionsCount: Long = 0,
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
            val currentUser = _uiState.value.user ?: userRepo.me(token).getOrNull()
            val followingResult = subscriptionRepo.getFollowing(token)
            val following = followingResult.getOrNull().orEmpty()
                .filterNot { it.isSelf(currentUser) }
                .ifEmpty {
                    subscriptionsStore.getAll()
                        .filter { !it.isPending && !it.isSelf(currentUser) }
                        .map { SubscriptionRequestResponse(subscriberId = it.userId, username = it.username) }
                }
            val outgoing = subscriptionRepo.getOutgoingRequests(token).getOrNull().orEmpty()
                .filterNot { it.isSelf(currentUser) }
            val followingIds = following.map { it.subscriberId }.toSet()

            val acceptedList = following.map { request ->
                val profile = userRepo.getProfile(token, request.username).getOrNull()
                StoredSubscription(
                    userId = request.subscriberId,
                    username = request.username,
                    avatarUrl = profile?.avatarUrl,
                    isPending = false
                ).also { subscriptionsStore.add(it) }
            }

            val pendingList = outgoing
                .filter { it.subscriberId !in followingIds }
                .map { request ->
                    val profile = userRepo.getProfile(token, request.username).getOrNull()
                    StoredSubscription(
                        userId = request.subscriberId,
                        username = request.username,
                        avatarUrl = profile?.avatarUrl,
                        isPending = true
                    ).also { subscriptionsStore.add(it) }
                }

            currentUser?.id?.let { subscriptionsStore.remove(it) }

            refreshProfileCounts(currentUser)

            _uiState.value = _uiState.value.copy(
                mySubscriptions = pendingList + acceptedList
            )
        }
    }

    fun refreshSubscriptions() = loadSubscriptions()

    fun loadSubscribers() {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: userRepo.me(token).getOrNull()
            val followersResult = subscriptionRepo.getFollowers(token)
            val followers = followersResult.getOrNull().orEmpty()
                .filterNot { it.isSelf(currentUser) }
                .ifEmpty {
                    subscribersStore.getAll()
                        .filter { !it.isSelf(currentUser) }
                        .map { SubscriptionRequestResponse(subscriberId = it.userId, username = it.username) }
                }
            val incoming = subscriptionRepo.getIncomingRequests(token).getOrNull().orEmpty()
                .filterNot { it.isSelf(currentUser) }
            val followerIds = followers.map { it.subscriberId }.toSet()

            val accepted = followers.map { request ->
                val profile = userRepo.getProfile(token, request.username).getOrNull()
                StoredSubscription(
                    userId = request.subscriberId,
                    username = request.username,
                    avatarUrl = profile?.avatarUrl
                ).also { subscribersStore.add(it) }
            }

            val pending = incoming
                .filter { it.subscriberId !in followerIds }
                .mapNotNull { request ->
                    if (request.isSelf(currentUser)) return@mapNotNull null
                    val profile = userRepo.getProfile(token, request.username).getOrNull()
                    SubscriberListItem(
                        userId = request.subscriberId,
                        username = request.username,
                        avatarUrl = profile?.avatarUrl,
                        isPending = true
                    )
                }

            currentUser?.id?.let { subscribersStore.remove(it) }

            refreshProfileCounts(currentUser)

            _uiState.value = _uiState.value.copy(
                mySubscribers = accepted,
                pendingSubscriberRequests = pending
            )
        }
    }

    fun acceptSubscriber(subscriberId: String) {
        viewModelScope.launch {
            val currentUser = _uiState.value.user ?: userRepo.me(token).getOrNull()
            if (currentUser != null && subscriberId == currentUser.id) return@launch

            subscriptionRepo.acceptRequest(token, subscriberId)
                .onSuccess {
                    val pending = _uiState.value.pendingSubscriberRequests
                        .find { it.userId == subscriberId }
                    if (pending != null && !pending.isSelf(currentUser)) {
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
                            _uiState.value = _uiState.value.copy(
                                privateProfile = profile.privateProfile,
                                subscribersCount = profile.subscribersCount,
                                subscriptionsCount = profile.subscriptionsCount
                            )
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

    fun deleteAvatar(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSavingProfile = true,
                profileSaveError = null
            )

            userRepo.deleteAvatar(token)
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

    private fun StoredSubscription.isSelf(currentUser: UserResponse?): Boolean =
        isSelfUser(userId, username, currentUser)

    private fun SubscriberListItem.isSelf(currentUser: UserResponse?): Boolean =
        isSelfUser(userId, username, currentUser)

    private fun SubscriptionRequestResponse.isSelf(
        currentUser: UserResponse?
    ): Boolean = isSelfUser(subscriberId, username, currentUser)

    private fun isSelfUser(userId: String, username: String, currentUser: UserResponse?): Boolean {
        if (currentUser == null) return false
        return userId == currentUser.id ||
            username.equals(currentUser.username, ignoreCase = true)
    }

    private suspend fun refreshProfileCounts(currentUser: UserResponse?) {
        if (currentUser == null) return
        userRepo.getProfile(token, currentUser.username).onSuccess { profile ->
            _uiState.value = _uiState.value.copy(
                subscribersCount = profile.subscribersCount,
                subscriptionsCount = profile.subscriptionsCount,
                privateProfile = profile.privateProfile
            )
        }
    }
}
