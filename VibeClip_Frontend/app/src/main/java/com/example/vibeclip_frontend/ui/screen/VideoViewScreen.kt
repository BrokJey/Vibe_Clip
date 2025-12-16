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
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(videoId) {
        isLoading = true
        errorMessage = null
        
        // Публичный endpoint доступен без токена, но если токен есть - используем его
        val result = if (token != null) {
            videoRepository.getVideo(token, videoId)
        } else {
            // Публичный запрос - используем пустой токен (endpoint доступен без аутентификации)
            videoRepository.getVideo("", videoId)
        }
        
        result.onSuccess { loadedVideo ->
            video = loadedVideo
            isLoading = false
        }.onFailure { error ->
            errorMessage = error.message ?: "Не удалось загрузить видео"
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
                errorMessage != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onBack) {
                            Text("Назад")
                        }
                    }
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

