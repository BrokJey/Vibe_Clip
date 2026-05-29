package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.model.VideoRequest
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.ui.components.ProfileHeader
import com.example.vibeclip_frontend.ui.components.ProfileVideoGrid
import com.example.vibeclip_frontend.ui.viewmodel.AdminModerationViewModel
import com.example.vibeclip_frontend.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,
    onLogout: () -> Unit,
    onVideoClick: (VideoResponse) -> Unit,
    onNavigateToUser: (String) -> Unit,
    viewModel: ProfileViewModel = viewModel {
        ProfileViewModel(
            UserRepository(),
            VideoRepository(),
            AppModule.subscriptionRepository,
            AppModule.subscriptionsStore,
            AppModule.subscribersStore,
            token
        )
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val isAdmin = uiState.user?.email.equals("admin@vibeclip.com", ignoreCase = true)
    val moderationViewModel: AdminModerationViewModel? = if (isAdmin) {
        viewModel(key = "admin_moderation_vm") {
            AdminModerationViewModel(AppModule.adminModerationRepository, token)
        }
    } else null
    val moderationUiState = moderationViewModel?.uiState?.collectAsState()?.value
    val user = uiState.user
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadSubscriptions()
        viewModel.loadSubscribers()
    }
    LaunchedEffect(isAdmin) {
        if (isAdmin) moderationViewModel?.load()
    }

    var showDeleteDialog by remember { mutableStateOf<VideoResponse?>(null) }
    var showEditDialog by remember { mutableStateOf<VideoResponse?>(null) }
    var showSubscriptionsDialog by remember { mutableStateOf(false) }
    var showSubscribersDialog by remember { mutableStateOf(false) }
    var showAvatarEdit by remember { mutableStateOf(false) }
    var showPrivacyConfirm by remember { mutableStateOf(false) }
    var showModerationSheet by remember { mutableStateOf(false) }

    if (showSubscriptionsDialog) {
        UsersListDialog(
            title = "Мои подписки",
            users = uiState.mySubscriptions,
            emptyMessage = "Вы ещё ни на кого не подписались",
            onDismiss = {
                showSubscriptionsDialog = false
                viewModel.loadSubscriptions()
            },
            onUserClick = { username ->
                showSubscriptionsDialog = false
                onNavigateToUser(username)
            }
        )
    }

    if (showPrivacyConfirm) {
        PrivacyConfirmSheet(
            onDismiss = { showPrivacyConfirm = false },
            onConfirm = {
                showPrivacyConfirm = false
                viewModel.setPrivateProfile(true)
            }
        )
    }

    if (showSubscribersDialog) {
        UsersListDialog(
            title = "Мои подписчики",
            users = uiState.mySubscribers,
            emptyMessage = "Пока нет подписчиков",
            pendingRequests = uiState.pendingSubscriberRequests,
            onDismiss = {
                showSubscribersDialog = false
                viewModel.loadSubscribers()
            },
            onUserClick = { username ->
                showSubscribersDialog = false
                onNavigateToUser(username)
            },
            onAcceptRequest = { viewModel.acceptSubscriber(it) },
            onRejectRequest = { viewModel.rejectSubscriber(it) }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = {
                    Column {
                        Text("Профиль")
                        ProfilePrivacyToggle(
                            checked = uiState.privateProfile,
                            enabled = !uiState.isUpdatingPrivacy,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showPrivacyConfirm = true
                                } else {
                                    viewModel.setPrivateProfile(false)
                                }
                            }
                        )
                    }
                },
                actions = {
                    if (isAdmin && moderationUiState != null && moderationViewModel != null) {
                        IconButton(onClick = {
                            showModerationSheet = true
                            moderationViewModel.load()
                        }) {
                            BadgedBox(
                                badge = {
                                    if (moderationUiState.notificationsCount > 0) {
                                        Badge { Text(moderationUiState.notificationsCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Жалобы на видео"
                                )
                            }
                        }
                    }
                    TextButton(onClick = onLogout) { Text("Выход") }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && user == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.errorMessage != null && user == null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    showRetry = true,
                    onRetry = { viewModel.refresh() }
                )
            }
            user != null -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    ProfileHeader(
                        username = user.username,
                        avatarUrl = user.avatarUrl,
                        subtitle = user.email,
                        showAvatarEdit = true,
                        onAvatarEditClick = { showAvatarEdit = true }
                    )

                    if (uiState.mySubscriptions.isNotEmpty()) {
                        ProfileLinkRow(
                            title = "Мои подписки",
                            subtitle = "${uiState.mySubscriptions.size} ${subscriptionsCountLabel(uiState.mySubscriptions.size)}",
                            onClick = { showSubscriptionsDialog = true }
                        )
                    }

                    val acceptedSubscribers = uiState.mySubscribers.size
                    val pendingRequests = uiState.pendingSubscriberRequests.size
                    if (acceptedSubscribers > 0 || pendingRequests > 0) {
                        ProfileLinkRow(
                            title = "Мои подписчики",
                            subtitle = buildString {
                                if (acceptedSubscribers > 0) {
                                    append("$acceptedSubscribers ${subscribersCountLabel(acceptedSubscribers)}")
                                }
                                if (pendingRequests > 0) {
                                    if (isNotEmpty()) append(" · ")
                                    append("$pendingRequests ${pendingRequestsLabel(pendingRequests)}")
                                }
                            },
                            onClick = { showSubscribersDialog = true }
                        )
                    }

                    HorizontalDivider()

                    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                        when {
                            uiState.errorMessage != null && uiState.videos.isEmpty() -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                                        Button(onClick = { viewModel.loadVideos(0) }) { Text("Повторить") }
                                    }
                                }
                            }
                            else -> {
                                ProfileVideoGrid(
                                    videos = uiState.videos,
                                    isLoading = uiState.isLoadingVideos,
                                    emptyMessage = "У вас пока нет видео",
                                    readOnly = false,
                                    onVideoClick = onVideoClick,
                                    onDeleteClick = { showDeleteDialog = it },
                                    onEditClick = { showEditDialog = it },
                                    onLoadMore = { viewModel.loadMore() },
                                    hasMore = uiState.hasMore
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        if (isAdmin && moderationUiState != null && moderationViewModel != null) {
            AdminReportsSheet(
                visible = showModerationSheet,
                isLoading = moderationUiState.isLoading,
                isActionLoading = moderationUiState.isActionLoading,
                items = moderationUiState.items,
                errorMessage = moderationUiState.errorMessage,
                onDismiss = { showModerationSheet = false },
                onRejectReports = { moderationViewModel.rejectReports(it) },
                onBlockVideo = { moderationViewModel.blockVideo(it) },
                onClearAllReports = { moderationViewModel.clearAllReports() },
                onOpenUser = onNavigateToUser,
                onRetry = { moderationViewModel.load() }
            )
        }

        AvatarEditSheet(
            visible = showAvatarEdit,
            initialAvatarUrl = user?.avatarUrl,
            isSaving = uiState.isSavingProfile,
            errorMessage = uiState.profileSaveError,
            onDismiss = { showAvatarEdit = false },
            onSave = { pickedUri ->
                viewModel.saveAvatar(context, pickedUri) {
                    showAvatarEdit = false
                }
            },
            onDeleteAvatar = {
                viewModel.deleteAvatar {
                    showAvatarEdit = false
                }
            }
        )
    }

    showDeleteDialog?.let { video ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить видео?") },
            text = { Text("Вы уверены, что хотите удалить видео ${video.title.orEmpty().trim('"', '\'')}?") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteVideo(video.id) { showDeleteDialog = null } }) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Отмена") }
            }
        )
    }

    showEditDialog?.let { video ->
        EditVideoDialog(
            video = video,
            onDismiss = { showEditDialog = null },
            onSave = { title, description, hashtags ->
                viewModel.updateVideo(
                    video.id,
                    VideoRequest(
                        title = title.ifBlank { null },
                        description = description.ifBlank { null },
                        hashtags = hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    )
                ) { showEditDialog = null }
            }
        )
    }
}

@Composable
private fun ProfileLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }
}

private fun subscriptionsCountLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "подписок"
        mod10 == 1 -> "подписка"
        mod10 in 2..4 -> "подписки"
        else -> "подписок"
    }
}

private fun subscribersCountLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "подписчиков"
        mod10 == 1 -> "подписчик"
        mod10 in 2..4 -> "подписчика"
        else -> "подписчиков"
    }
}

private fun pendingRequestsLabel(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "заявок"
        mod10 == 1 -> "заявка"
        mod10 in 2..4 -> "заявки"
        else -> "заявок"
    }
}

@Composable
fun EditVideoDialog(
    video: VideoResponse,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(video.title.orEmpty()) }
    var description by remember { mutableStateOf(video.description ?: "") }
    var hashtags by remember { mutableStateOf(video.hashtags.orEmpty().joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(text = "Редактировать видео", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                    maxLines = 5
                )
                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    label = { Text("Хештеги (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Отмена") }
                    Button(onClick = { onSave(title, description, hashtags) }, modifier = Modifier.weight(1f)) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
