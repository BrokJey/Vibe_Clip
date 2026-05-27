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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.ui.viewmodel.FolderFeedViewModel
import com.example.vibeclip_frontend.navigation.Screen

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FolderFeedScreen(
    token: String,
    folderId: String,
    navController: NavController,
    onBack: () -> Unit,
    viewModel: FolderFeedViewModel = viewModel {
        FolderFeedViewModel(FolderRepository(), token, folderId)
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { maxOf(uiState.videos.size, 1) })
    var viewerUsername by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(token) {
        AppModule.userRepository.me(token).onSuccess { me ->
            viewerUsername = me.username
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var didEnterFolderFeed by remember { mutableStateOf(false) }
    LaunchedEffect(currentBackStackEntry) {
        val route = currentBackStackEntry?.destination?.route
        val currentFolderId = currentBackStackEntry?.arguments?.getString("folderId")
        if (route == Screen.FolderFeed.routePattern && currentFolderId == folderId) {
            if (didEnterFolderFeed) {
                viewModel.reload()
            } else {
                didEnterFolderFeed = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val name = uiState.folderName
                    Text(
                        text = if (name.isNotBlank()) "Лента папки $name" else "Лента папки",
                    )
                },
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
                ErrorContent(
                    message = uiState.errorMessage!!,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    showRetry = true,
                    onRetry = { viewModel.loadPage() }
                )
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
                    val isActive = page == pagerState.currentPage
                    VideoFullScreenCard(
                        video = folderVideo.video,
                        isActive = isActive,
                        token = token,
                        navController = navController,
                        viewerUsername = viewerUsername
                    )
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


