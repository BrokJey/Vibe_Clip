package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.ui.viewmodel.UserProfileFeedViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun UserProfileFeedScreen(
    token: String,
    username: String,
    initialVideoId: String,
    navController: NavController,
    onBackToMainFeed: () -> Unit,
    viewModel: UserProfileFeedViewModel = viewModel(key = "user_feed_${username}_$initialVideoId") {
        UserProfileFeedViewModel(UserRepository(), token, username)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    var viewerUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        AppModule.userRepository.me(token).onSuccess { me ->
            viewerUsername = me.username
        }
    }

    val videoCount = uiState.videos.size
    // Последняя страница — экран «видео закончились»
    val pageCount = if (videoCount > 0) videoCount + 1 else 1

    val startPage = uiState.videos.indexOfFirst { it.id == initialVideoId }.coerceAtLeast(0)
    val pagerState = rememberPagerState(
        initialPage = startPage.coerceAtMost((videoCount - 1).coerceAtLeast(0)),
        pageCount = { pageCount }
    )

    LaunchedEffect(uiState.videos, initialVideoId) {
        if (uiState.videos.isNotEmpty()) {
            val index = uiState.videos.indexOfFirst { it.id == initialVideoId }.coerceAtLeast(0)
            if (pagerState.currentPage != index) {
                pagerState.scrollToPage(index)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = uiState.username.ifBlank { username }
                    Text("Видео @$name")
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null -> {
                ErrorContent(
                    message = uiState.errorMessage!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    showRetry = true,
                    onRetry = { viewModel.load() }
                )
            }
            uiState.videos.isEmpty() -> {
                UserVideosEndContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onBackToMainFeed = onBackToMainFeed
                )
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black)
                ) { page ->
                    if (page < uiState.videos.size) {
                        val video = uiState.videos[page]
                        val isActive = page == pagerState.currentPage
                        VideoFullScreenCard(
                            video = video,
                            isActive = isActive,
                            token = token,
                            navController = navController,
                            viewerUsername = viewerUsername
                        )
                    } else {
                        UserVideosEndContent(
                            modifier = Modifier.fillMaxSize(),
                            onBackToMainFeed = onBackToMainFeed
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserVideosEndContent(
    modifier: Modifier = Modifier,
    onBackToMainFeed: () -> Unit
) {
    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Видео этого пользователя закончились!",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 28.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onBackToMainFeed,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Text("Вернуться в главную ленту")
            }
        }
    }
}
