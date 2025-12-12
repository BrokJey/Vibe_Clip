package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.ui.screen.VideoFullScreenCard
import com.example.vibeclip_frontend.ui.viewmodel.FolderFeedViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderFeedScreen(
    token: String,
    folderId: String,
    onBack: () -> Unit,
    viewModel: FolderFeedViewModel = viewModel {
        FolderFeedViewModel(FolderRepository(), token, folderId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { maxOf(uiState.videos.size, 1) })

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Лента папки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.errorMessage != null && uiState.videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error) }
            }
            uiState.videos.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("В этой папке пока нет видео") }
            }
            else -> {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Black)
                ) { page ->
                    val folderVideo = uiState.videos[page]
                    VideoFullScreenCard(video = folderVideo.video)
                }

                LaunchedEffect(pagerState.currentPage, uiState.hasMore, uiState.isLoading) {
                    if (uiState.hasMore && !uiState.isLoading && pagerState.currentPage >= uiState.videos.size - 2) {
                        viewModel.loadMore()
                    }
                }
            }
        }
    }
}


