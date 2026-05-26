package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.UserFacingError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoViewScreen(
    videoId: String,
    token: String?,
    onBack: () -> Unit
) {
    val videoRepository = remember { AppModule.videoRepository }
    var video by remember { mutableStateOf<com.example.vibeclip_frontend.data.model.VideoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorInfo by remember { mutableStateOf<UserFacingError?>(null) }
    var reloadNonce by remember { mutableStateOf(0) }

    LaunchedEffect(videoId, reloadNonce) {
        isLoading = true
        errorInfo = null

        val result = if (token != null) {
            videoRepository.getVideo(token, videoId)
        } else {
            videoRepository.getVideo("", videoId)
        }

        result.onSuccess { loadedVideo ->
            video = loadedVideo
            isLoading = false
        }.onFailure { error ->
            errorInfo = ErrorMessages.fromThrowable(error)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { Text("Видео") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black)
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                errorInfo != null -> {
                    ErrorContent(
                        message = errorInfo!!.message,
                        modifier = Modifier.fillMaxSize(),
                        showRetry = errorInfo!!.showRetry,
                        onRetry = { reloadNonce++ }
                    )
                }
                video != null -> {
                    // Используем VideoFullScreenCard из FeedScreen для отображения видео
                    VideoFullScreenCard(
                        video = video!!,
                        isActive = true,
                        token = token ?: ""
                    )
                }
            }
        }
    }
}

