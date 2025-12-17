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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(videoId) {
        isLoading = true
        errorMessage = null
        videoRepository.getVideo(token, videoId)
            .onSuccess { loadedVideo ->
                video = loadedVideo
                isLoading = false
            }
            .onFailure { error ->
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
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                        Button(onClick = onBack) {
                            Text("Назад")
                        }
                    }
                }
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

