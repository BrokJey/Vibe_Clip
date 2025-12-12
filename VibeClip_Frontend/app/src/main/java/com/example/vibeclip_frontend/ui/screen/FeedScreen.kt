package com.example.vibeclip_frontend.ui.screen

import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.ui.viewmodel.VideoViewModel
import com.example.vibeclip_frontend.BuildConfig

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    token: String,
    onLogout: () -> Unit,
    viewModel: VideoViewModel = viewModel { VideoViewModel(VideoRepository(), token) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val pagerState = rememberPagerState(pageCount = { maxOf(uiState.videos.size, 1) })
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vibe Clip Feed") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("Logout")
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
                ) {
                    CircularProgressIndicator()
                }
            }
            uiState.errorMessage != null && uiState.videos.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
            else -> {
                if (uiState.videos.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Пока нет видео",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.refresh() }) {
                            Text("Обновить")
                        }
                    }
                } else {
                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) { page ->
                        val video = uiState.videos[page]
                        VideoFullScreenCard(
                            video = video
                        )
                    }

                    // Lazy load next page when near end
                    LaunchedEffect(pagerState.currentPage, uiState.hasMore, uiState.isLoading) {
                        if (uiState.hasMore && !uiState.isLoading && pagerState.currentPage >= uiState.videos.size - 2) {
                            viewModel.loadMore()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFullScreenCard(video: VideoResponse) {
    val context = LocalContext.current
    val baseHost = remember {
        // Превращаем http://host:port/api/v1/ -> http://host:port
        BuildConfig.API_BASE_URL
            .removeSuffix("/")
            .substringBefore("/api")
    }
    val resolvedUrl = remember(video.videoUrl) {
        val url = video.videoUrl
        if (url.isNullOrBlank()) {
            Log.e("FeedScreen", "Video URL is null or blank for video ${video.id}")
            null
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            Log.d("FeedScreen", "Using absolute URL: $url")
            url
        } else {
            val fullUrl = "$baseHost/${url.trimStart('/')}"
            Log.d("FeedScreen", "Resolved relative URL: $url -> $fullUrl")
            fullUrl
        }
    }
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(resolvedUrl) {
        playbackError = null
        isBuffering = true
        if (resolvedUrl != null) {
            try {
                Log.d("FeedScreen", "Preparing video: $resolvedUrl")
                exoPlayer.setMediaItem(MediaItem.fromUri(resolvedUrl))
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
            } catch (e: Exception) {
                Log.e("FeedScreen", "Error preparing video", e)
                playbackError = "Ошибка подготовки: ${e.message}"
                isBuffering = false
            }
        } else {
            playbackError = "Видео URL пустой"
            isBuffering = false
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                Log.d("FeedScreen", "Playback state changed: $playbackState")
            }

            override fun onPlayerErrorChanged(error: PlaybackException?) {
                if (error != null) {
                    val errorMessage = when {
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> 
                            "Ошибка сети. Проверьте подключение к интернету."
                        error.errorCode == PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> 
                            "Таймаут подключения. Проверьте доступность сервера."
                        error.errorCode == PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED -> 
                            "Неподдерживаемый формат видео."
                        error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> 
                            "Видеофайл не найден: $resolvedUrl"
                        else -> error.message ?: "Неизвестная ошибка (код: ${error.errorCode})"
                    }
                    Log.e("FeedScreen", "Player error: ${error.errorCode}, message: ${error.message}", error)
                    playbackError = errorMessage
                    isBuffering = false
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                PlayerView(it).apply {
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                }
            }
        )

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }

        playbackError?.let { err ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ошибка воспроизведения: $err",
                        color = Color.White,
                        fontSize = 16.sp
                    )
                    if (resolvedUrl != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "URL: $resolvedUrl",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    if (video.videoUrl.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Исходный URL: ${video.videoUrl}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // Overlay info
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Text(
                text = "@${video.authorUsername.orEmpty()}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = video.title,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!video.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.description,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            if (video.hashtags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = video.hashtags.joinToString(" ") { "#$it" },
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }

        val metrics = video.metrics
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (metrics != null) {
                StatBadge(label = "Views", value = metrics.viewCount)
                StatBadge(label = "Likes", value = metrics.likeCount)
                StatBadge(label = "Comments", value = metrics.commentCount)
                StatBadge(label = "Shares", value = metrics.shareCount)
            }
        }
    }
}

@Composable
private fun StatBadge(label: String, value: Long) {
    Surface(
        color = Color.White.copy(alpha = 0.15f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value.toString(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text = label, color = Color.White, fontSize = 10.sp)
        }
    }
}

