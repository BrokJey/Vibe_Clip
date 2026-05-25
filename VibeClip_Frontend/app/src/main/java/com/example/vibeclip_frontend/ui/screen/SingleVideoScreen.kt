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
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.util.ErrorMessages

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleVideoScreen(
    token: String,
    videoId: String,
    onBack: () -> Unit
) {
    val videoRepository = remember { AppModule.videoRepository }
    var video by remember { mutableStateOf<VideoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorInfo by remember { mutableStateOf<com.example.vibeclip_frontend.util.UserFacingError?>(null) }
    var reloadNonce by remember { mutableStateOf(0) }

    LaunchedEffect(videoId, reloadNonce) {
        isLoading = true
        errorInfo = null
        videoRepository.getVideo(token, videoId)
            .onSuccess { loadedVideo ->
                video = loadedVideo
                isLoading = false
            }
            .onFailure { error ->
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
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorInfo != null -> {
                ErrorContent(
                    message = errorInfo!!.message,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    showRetry = errorInfo!!.showRetry,
                    onRetry = { reloadNonce++ }
                )
            }
            video != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black)
                ) {
                    VideoFullScreenCard(
                        video = video!!,
                        isActive = true,
                        token = token
                    )
                }
            }
        }
    }
}

