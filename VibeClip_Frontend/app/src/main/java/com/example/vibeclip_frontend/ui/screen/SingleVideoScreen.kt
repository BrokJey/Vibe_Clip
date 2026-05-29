package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.util.ErrorMessages
import com.example.vibeclip_frontend.util.UserFacingError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleVideoScreen(
    token: String,
    videoId: String,
    onBack: () -> Unit,
    navController: NavController? = null
) {
    val videoRepository = remember { AppModule.videoRepository }
    var video by remember { mutableStateOf<VideoResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorInfo by remember { mutableStateOf<UserFacingError?>(null) }
    var reloadNonce by remember { mutableIntStateOf(0) }
    var viewerUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        AppModule.userRepository.me(token).onSuccess { me ->
            viewerUsername = me.username
        }
    }

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
                        token = token,
                        navController = navController,
                        viewerUsername = viewerUsername
                    )
                }
            }
        }
    }
}
