package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.model.UserProfileResponse
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.ui.components.ProfileHeader
import com.example.vibeclip_frontend.ui.components.ProfileVideoGrid
import com.example.vibeclip_frontend.ui.viewmodel.UserProfileViewModel
import com.example.vibeclip_frontend.util.subscribersCountLabel
import com.example.vibeclip_frontend.util.subscriptionsCountLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    token: String,
    username: String,
    onBack: () -> Unit,
    onVideoClick: (VideoResponse) -> Unit,
    viewModel: UserProfileViewModel = viewModel(key = "user_profile_$username")
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(token, username) {
        viewModel.init(token, username)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.profile == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.profile == null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    showRetry = true,
                    onRetry = { viewModel.loadProfile() }
                )
            }
            uiState.profile != null -> {
                val profile = uiState.profile!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    ProfileHeader(
                        username = profile.username,
                        avatarUrl = profile.avatarUrl,
                        subtitle = buildProfileStatsSubtitle(
                            profile = profile,
                            isOwnProfile = uiState.isOwnProfile
                        ),
                        action = {
                            if (!uiState.isOwnProfile) {
                                SubscriptionButton(
                                    isPrivateProfile = profile.privateProfile,
                                    isSubscribed = uiState.isSubscribed,
                                    isPending = uiState.isPending,
                                    isLoading = uiState.isSubscriptionActionInProgress,
                                    onClick = { viewModel.toggleSubscription() }
                                )
                            }
                        }
                    )

                    HorizontalDivider()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        ProfileVideoGrid(
                            videos = uiState.videos,
                            isLoading = false,
                            emptyMessage = if (
                                profile.privateProfile &&
                                !uiState.isSubscribed &&
                                !uiState.isOwnProfile &&
                                !uiState.isAdmin
                            ) {
                                "Видео доступны только подписчикам"
                            } else {
                                "У пользователя пока нет видео"
                            },
                            readOnly = true,
                            onVideoClick = onVideoClick
                        )
                    }
                }
            }
        }
    }
}

private fun buildProfileStatsSubtitle(
    profile: UserProfileResponse,
    isOwnProfile: Boolean
): String = buildString {
    append(
        "${profile.subscribersCount} ${subscribersCountLabel(profile.subscribersCount.toInt())}"
    )
    append(" · ")
    append(
        "${profile.subscriptionsCount} ${subscriptionsCountLabel(profile.subscriptionsCount.toInt())}"
    )
    if (profile.privateProfile && !isOwnProfile) {
        append("\nПриватный профиль")
    }
}

@Composable
private fun SubscriptionButton(
    isPrivateProfile: Boolean,
    isSubscribed: Boolean,
    isPending: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    val isLinked = isSubscribed || isPending
    val label = when {
        isPending && !isSubscribed -> "Отменить заявку"
        isSubscribed -> "Отписаться"
        isPrivateProfile -> "Отправить заявку"
        else -> "Подписаться"
    }

    if (isLinked) {
        OutlinedButton(onClick = onClick, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                Text(label)
            }
        }
    } else {
        Button(onClick = onClick, enabled = !isLoading) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
            } else {
                Text(label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
