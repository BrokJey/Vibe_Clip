package com.example.vibeclip_frontend.ui.screen

import android.view.ViewGroup
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.*
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.concurrent.TimeUnit
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.data.model.ReactionResponse
import com.example.vibeclip_frontend.data.model.CommentResponse
import com.example.vibeclip_frontend.data.repository.CommentRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    token: String,
    onLogout: () -> Unit,
    viewModel: VideoViewModel = viewModel { VideoViewModel(VideoRepository(), token) }
) {
    val uiState by viewModel.uiState.collectAsState()
    // Бесконечная лента: используем большое число страниц для циклического прокручивания
    val pageCount = if (uiState.videos.isNotEmpty()) {
        // Используем большое число, чтобы создать эффект бесконечности
        // Используем множитель 1000 для плавной прокрутки
        uiState.videos.size * 1000
    } else {
        1
    }
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pageCount }
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VibeClip") },
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
                        // Вычисляем реальный индекс видео для бесконечной ленты
                        val videoIndex = if (uiState.videos.isNotEmpty()) {
                            page % uiState.videos.size
                        } else {
                            0
                        }
                        val video = uiState.videos[videoIndex]
                        // Проверяем, является ли это видео активным (текущая страница)
                        val isActive = page == pagerState.currentPage
                        VideoFullScreenCard(
                            video = video,
                            isActive = isActive,
                            token = token
                        )
                    }

                    // Lazy load next page when near end (для бесконечной ленты)
                    LaunchedEffect(pagerState.currentPage, uiState.hasMore, uiState.isLoading, uiState.videos.size) {
                        if (uiState.videos.isNotEmpty()) {
                            val currentVideoIndex = pagerState.currentPage % uiState.videos.size
                            // Загружаем новые видео, когда дошли почти до конца текущего списка
                            // Проверяем не по текущей странице, а по реальному индексу в списке
                            if (uiState.hasMore && 
                                !uiState.isLoading && 
                                currentVideoIndex >= uiState.videos.size - 3) {
                            viewModel.loadMore()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoFullScreenCard(
    video: VideoResponse,
    isActive: Boolean = true,
    token: String
) {
    val scope = rememberCoroutineScope()
    val reactionRepository = remember { AppModule.reactionRepository }
    val commentRepository = remember { AppModule.commentRepository }
    val videoRepository = remember { AppModule.videoRepository }
    
    // Состояние реакций
    var userLikeReaction by remember { mutableStateOf<ReactionResponse?>(null) }
    var userShareReaction by remember { mutableStateOf<ReactionResponse?>(null) }
    var userViewReaction by remember { mutableStateOf<ReactionResponse?>(null) }
    var metrics by remember { mutableStateOf(video.metrics) }
    var isLoadingReaction by remember { mutableStateOf(false) }
    var hasTrackedView by remember { mutableStateOf(false) } // Отслеживание, был ли отправлен просмотр
    
    // Состояние для BottomSheet комментариев
    var showCommentsSheet by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val baseHost = remember {
        // Превращаем http://host:port/api/v1/ -> http://host:port
        BuildConfig.API_BASE_URL
            .removeSuffix("/")
            .substringBefore("/api")
    }
    
    // Загрузка реакций пользователя и метрик при инициализации
    LaunchedEffect(video.id, token) {
        val reactionsResult = reactionRepository.getVideoReactions(token, video.id)
        reactionsResult.onSuccess { reactions ->
            Log.d("FeedScreen", "Loaded reactions for video ${video.id}: ${reactions.size} reactions")
            userLikeReaction = reactions.find { it.reactionType == "LIKE" }
            userShareReaction = reactions.find { it.reactionType == "SHARE" }
            userViewReaction = reactions.find { it.reactionType == "VIEW" }
            if (userLikeReaction != null) {
                Log.d("FeedScreen", "User has liked this video: ${userLikeReaction!!.id}")
            } else {
                Log.d("FeedScreen", "User has NOT liked this video")
            }
            if (userViewReaction != null) {
                hasTrackedView = true // Уже был просмотр
                Log.d("FeedScreen", "User has already viewed this video: ${userViewReaction!!.id}")
            }
        }.onFailure {
            Log.e("FeedScreen", "Failed to load reactions", it)
        }
        
        // Загружаем актуальные метрики с сервера
        val videoResult = videoRepository.getVideo(token, video.id)
        videoResult.onSuccess { updatedVideo ->
            Log.d("FeedScreen", "Loaded video metrics: likeCount=${updatedVideo.metrics?.likeCount}")
            updatedVideo.metrics?.let { updatedMetrics ->
                metrics = updatedMetrics
            }
        }.onFailure {
            Log.e("FeedScreen", "Failed to load video metrics", it)
        }
    }
    
    // Перезагрузка реакций при активации видео (когда пользователь возвращается к видео)
    var lastActiveVideoId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(isActive, video.id) {
        if (isActive && lastActiveVideoId != video.id) {
            // Когда видео становится активным и это другое видео, перезагружаем реакции и метрики
            // чтобы убедиться, что данные актуальны
            lastActiveVideoId = video.id
            // Сбрасываем флаг отслеживания просмотра для нового видео
            hasTrackedView = false
            val reactionsResult = reactionRepository.getVideoReactions(token, video.id)
            reactionsResult.onSuccess { reactions ->
                Log.d("FeedScreen", "Reloaded reactions for video ${video.id}: ${reactions.size} reactions")
                userLikeReaction = reactions.find { it.reactionType == "LIKE" }
                userShareReaction = reactions.find { it.reactionType == "SHARE" }
                userViewReaction = reactions.find { it.reactionType == "VIEW" }
                if (userViewReaction != null) {
                    hasTrackedView = true // Уже был просмотр этого видео
                }
            }
            
            // Загружаем актуальные метрики с сервера
            val videoResult = videoRepository.getVideo(token, video.id)
            videoResult.onSuccess { updatedVideo ->
                updatedVideo.metrics?.let { updatedMetrics ->
                    metrics = updatedMetrics
                }
            }
        }
    }
    
    // Функции для обработки реакций
    fun handleLike() {
        if (isLoadingReaction) return
        isLoadingReaction = true
        scope.launch {
            if (userLikeReaction != null) {
                // Удаляем лайк
                val currentLikeCount = metrics?.likeCount ?: 0
                // Оптимистичное обновление UI
                userLikeReaction = null
                metrics = metrics?.copy(likeCount = maxOf(0, currentLikeCount - 1)) ?: 
                    com.example.vibeclip_frontend.data.model.VideoMetricsResponse(
                        viewCount = 0,
                        likeCount = maxOf(0, currentLikeCount - 1),
                        commentCount = 0,
                        shareCount = 0
                    )
                
                val result = reactionRepository.deleteReaction(token, video.id, "LIKE")
                result.onSuccess {
                    Log.d("FeedScreen", "Like deleted successfully")
                    // Обновляем метрики с сервера после небольшой задержки
                    kotlinx.coroutines.delay(500) // Даём серверу время обновить метрики
                    // Перезагружаем реакции и метрики для синхронизации
                    val reactionsResult = reactionRepository.getVideoReactions(token, video.id)
                    reactionsResult.onSuccess { reactions ->
                        userLikeReaction = reactions.find { it.reactionType == "LIKE" }
                        userShareReaction = reactions.find { it.reactionType == "SHARE" }
                    }
                    val videoResult = videoRepository.getVideo(token, video.id)
                    videoResult.onSuccess { updatedVideo ->
                        updatedVideo.metrics?.let { updatedMetrics ->
                            metrics = updatedMetrics
                        }
                    }
                }.onFailure {
                    // В случае ошибки откатываем изменения
                    Log.e("FeedScreen", "Failed to delete like", it)
                    // Перезагружаем реакции пользователя
                    val reactionsResult = reactionRepository.getVideoReactions(token, video.id)
                    reactionsResult.onSuccess { reactions ->
                        userLikeReaction = reactions.find { it.reactionType == "LIKE" }
                    }
                    // Восстанавливаем метрики
                    val videoResult = videoRepository.getVideo(token, video.id)
                    videoResult.onSuccess { updatedVideo ->
                        updatedVideo.metrics?.let { updatedMetrics ->
                            metrics = updatedMetrics
                        }
                    }
                }
            } else {
                // Добавляем лайк
                val currentLikeCount = metrics?.likeCount ?: 0
                // Оптимистичное обновление UI
                metrics = metrics?.copy(likeCount = currentLikeCount + 1) ?: 
                    com.example.vibeclip_frontend.data.model.VideoMetricsResponse(
                        viewCount = 0,
                        likeCount = currentLikeCount + 1,
                        commentCount = 0,
                        shareCount = 0
                    )
                
                val result = reactionRepository.createReaction(token, video.id, "LIKE")
                result.onSuccess { reaction ->
                    Log.d("FeedScreen", "Like created successfully: ${reaction.id}")
                    userLikeReaction = reaction
                    // Обновляем метрики с сервера после небольшой задержки
                    kotlinx.coroutines.delay(500) // Даём серверу время обновить метрики
                    // Перезагружаем реакции и метрики для синхронизации
                    val reactionsResult = reactionRepository.getVideoReactions(token, video.id)
                    reactionsResult.onSuccess { reactions ->
                        userLikeReaction = reactions.find { it.reactionType == "LIKE" }
                        userShareReaction = reactions.find { it.reactionType == "SHARE" }
                    }
                    val videoResult = videoRepository.getVideo(token, video.id)
                    videoResult.onSuccess { updatedVideo ->
                        updatedVideo.metrics?.let { updatedMetrics ->
                            metrics = updatedMetrics
                        }
                    }
                }.onFailure {
                    // В случае ошибки откатываем изменения
                    Log.e("FeedScreen", "Failed to create like: ${it.message}", it)
                    userLikeReaction = null
                    // Восстанавливаем метрики
                    val videoResult = videoRepository.getVideo(token, video.id)
                    videoResult.onSuccess { updatedVideo ->
                        updatedVideo.metrics?.let { updatedMetrics ->
                            metrics = updatedMetrics
                        }
                    }
                }
            }
            isLoadingReaction = false
        }
    }
    
    fun handleShare() {
        // Копируем ссылку на видео в буфер обмена
        // Формируем ссылку на видео (аналогично бэкенду: /api/v1/videos/{videoId})
        val shareUrl = "$baseHost/api/v1/videos/${video.id}"
        
        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Video URL", shareUrl)
        clipboard.setPrimaryClip(clip)
        
        // Показываем сообщение "Скопировано"
        Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
        
        // Обновляем счетчик share только если пользователь еще не делился
        // Согласно требованиям: один пользователь - один share засчитывается только один раз,
        // но делиться можно бесконечное количество раз
        if (userShareReaction == null && !isLoadingReaction) {
            isLoadingReaction = true
            scope.launch {
                // Добавляем share реакцию
                val result = reactionRepository.createReaction(token, video.id, "SHARE")
                result.onSuccess { reaction ->
                    Log.d("FeedScreen", "Share reaction created successfully: ${reaction.id}")
                    userShareReaction = reaction
                    // Обновляем метрики с сервера после небольшой задержки
                    kotlinx.coroutines.delay(500) // Даём серверу время обновить метрики
                    val videoResult = videoRepository.getVideo(token, video.id)
                    videoResult.onSuccess { updatedVideo ->
                        Log.d("FeedScreen", "Updated share metrics from server: shareCount=${updatedVideo.metrics?.shareCount}")
                        updatedVideo.metrics?.let { updatedMetrics ->
                            metrics = updatedMetrics
                        }
                    }.onFailure {
                        Log.e("FeedScreen", "Failed to fetch updated video metrics", it)
                    }
                }.onFailure {
                    Log.e("FeedScreen", "Failed to create share reaction: ${it.message}", it)
                }
                isLoadingReaction = false
            }
        }
    }
    
    fun handleComment() {
        showCommentsSheet = true
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
        ExoPlayer.Builder(context).build().apply {
            // Настройка автоматического повтора видео
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isDraggingProgress by remember { mutableStateOf(false) }

    // Инициализация видео
    LaunchedEffect(resolvedUrl) {
        playbackError = null
        isBuffering = true
        if (resolvedUrl != null) {
            try {
                Log.d("FeedScreen", "Preparing video: $resolvedUrl")
                exoPlayer.setMediaItem(MediaItem.fromUri(resolvedUrl))
                exoPlayer.prepare()
                // Не начинаем воспроизведение сразу - только если активно
                exoPlayer.playWhenReady = false
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

    // Управление воспроизведением в зависимости от активности
    LaunchedEffect(isActive) {
        if (isActive && resolvedUrl != null && playbackError == null) {
            // Видео активно - начинаем воспроизведение
            Log.d("FeedScreen", "Video became active, starting playback: ${video.id}")
            exoPlayer.playWhenReady = true
            
            // Отправляем VIEW реакцию при первом просмотре видео
            if (!hasTrackedView && userViewReaction == null) {
                scope.launch {
                    val result = reactionRepository.createReaction(token, video.id, "VIEW")
                    result.onSuccess { reaction ->
                        Log.d("FeedScreen", "View reaction created successfully: ${reaction.id}")
                        userViewReaction = reaction
                        hasTrackedView = true
                        // Обновляем метрики с сервера после небольшой задержки
                        kotlinx.coroutines.delay(500) // Даём серверу время обновить метрики
                        val videoResult = videoRepository.getVideo(token, video.id)
                        videoResult.onSuccess { updatedVideo ->
                            Log.d("FeedScreen", "Updated view metrics from server: viewCount=${updatedVideo.metrics?.viewCount}")
                            updatedVideo.metrics?.let { updatedMetrics ->
                                metrics = updatedMetrics
                            }
                        }.onFailure {
                            Log.e("FeedScreen", "Failed to fetch updated video metrics", it)
                        }
                    }.onFailure {
                        Log.e("FeedScreen", "Failed to create view reaction: ${it.message}", it)
                    }
                }
            }
        } else if (!isActive) {
            // Видео неактивно - останавливаем воспроизведение
            Log.d("FeedScreen", "Video became inactive, pausing: ${video.id}")
            exoPlayer.playWhenReady = false
            exoPlayer.pause()
        }
    }

    // Обновление позиции и длительности видео
    LaunchedEffect(exoPlayer, isActive) {
        if (!isActive) return@LaunchedEffect
        
        while (true) {
            if (!isDraggingProgress) {
                currentPosition = exoPlayer.currentPosition
                if (exoPlayer.duration > 0) {
                    duration = exoPlayer.duration
                }
            }
            kotlinx.coroutines.delay(100) // Обновляем каждые 100мс
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
                isPlaying = exoPlayer.isPlaying
                if (exoPlayer.duration > 0 && duration == 0L) {
                    duration = exoPlayer.duration
                }
                Log.d("FeedScreen", "Playback state changed: $playbackState, isPlaying: $isPlaying")
            }

            override fun onIsPlayingChanged(isCurrentlyPlaying: Boolean) {
                isPlaying = isCurrentlyPlaying
                Log.d("FeedScreen", "IsPlaying changed: $isCurrentlyPlaying")
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
            .clickable {
                // Переключение паузы/возобновления при клике на видео
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                } else {
                    exoPlayer.play()
                }
            }
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

        // Иконка паузы/play в центре экрана (только для активного видео)
        if (isActive && !isPlaying && !isBuffering && playbackError == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

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

        // Просмотры в левом верхнем углу
        val currentMetrics = metrics ?: video.metrics
        if (currentMetrics != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .padding(top = 60.dp) // Отступ от шапки
            ) {
                Text(
                    text = "${currentMetrics.viewCount} просмотров",
                    color = Color(0xFF9C88FF), // Фиолетовый цвет
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
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

        // Кнопки реакций справа сбоку, поверх видео
        val displayMetrics = currentMetrics ?: video.metrics
        val defaultMetrics = displayMetrics ?: com.example.vibeclip_frontend.data.model.VideoMetricsResponse(
            viewCount = 0,
            likeCount = 0,
            commentCount = 0,
            shareCount = 0
        )
        
        // Кнопки всегда отображаются справа по центру
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Кнопка лайка
            ReactionButton(
                icon = if (userLikeReaction != null) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                count = defaultMetrics.likeCount,
                isActive = userLikeReaction != null,
                onClick = { handleLike() },
                isLoading = isLoadingReaction
            )
            
            // Кнопка комментариев
            ReactionButton(
                icon = Icons.AutoMirrored.Filled.Comment,
                count = defaultMetrics.commentCount,
                isActive = false, // Комментарии всегда с обводкой
                onClick = { handleComment() },
                isLoading = false
            )
            
            // Кнопка поделиться
            ReactionButton(
                icon = Icons.Default.Share,
                count = defaultMetrics.shareCount,
                isActive = userShareReaction != null,
                onClick = { handleShare() },
                isLoading = isLoadingReaction
            )
        }

        // Прогресс-бар для видео (только для активного видео) - полоса внизу
        if (isActive && duration > 0 && playbackError == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(12.dp) // Высота 12 пикселей
            ) {
                // Слайдер прогресса - фиолетовая полоса
                Slider(
                    value = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f,
                    onValueChange = { newValue ->
                        isDraggingProgress = true
                        val newPosition = (newValue * duration).toLong()
                        currentPosition = newPosition
                    },
                    onValueChangeFinished = {
                        isDraggingProgress = false
                        exoPlayer.seekTo(currentPosition)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF9C88FF), // Светло-фиолетовый
                        activeTrackColor = Color(0xFF9C88FF), // Светло-фиолетовый
                        inactiveTrackColor = Color(0xFF9C88FF).copy(alpha = 0.3f) // Прозрачный светло-фиолетовый
                    )
                )
            }
        }
        
        // BottomSheet для комментариев
        if (showCommentsSheet) {
            CommentsBottomSheet(
                videoId = video.id,
                token = token,
                commentRepository = commentRepository,
                onDismiss = { showCommentsSheet = false },
                onCommentAdded = {
                    // Обновляем счетчик комментариев
                    metrics = metrics?.copy(commentCount = (metrics?.commentCount ?: 0) + 1)
                }
            )
        }
    }
}

// Функция для форматирования времени в формат MM:SS
private fun formatTime(timeMs: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(timeMs)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

@Composable
private fun ReactionButton(
    icon: ImageVector,
    count: Long,
    isActive: Boolean,
    onClick: () -> Unit,
    isLoading: Boolean
) {
    val purpleColor = Color(0xFF9C88FF)
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                enabled = !isLoading,
                onClick = onClick,
                indication = null, // Убираем визуальную индикацию клика
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .then(
                    if (isActive) {
                        Modifier.background(purpleColor)
                    } else {
                        Modifier.border(2.dp, purpleColor, CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) Color.White else purpleColor,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCount(count),
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatCount(count: Long): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
        count >= 1_000 -> String.format("%.1fK", count / 1_000.0)
        else -> count.toString()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsBottomSheet(
    videoId: String,
    token: String,
    commentRepository: CommentRepository,
    onDismiss: () -> Unit,
    onCommentAdded: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var comments by remember { mutableStateOf<List<CommentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var commentText by remember { mutableStateOf(TextFieldValue("")) }
    var isSending by remember { mutableStateOf(false) }
    
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Загрузка комментариев при открытии модального окна
    LaunchedEffect(videoId) {
        isLoading = true
        error = null
        comments = emptyList() // Очищаем список перед загрузкой
        val result = commentRepository.getVideoComments(token, videoId)
        result.onSuccess { loadedComments ->
            // Бэкенд возвращает комментарии отсортированные по дате (новые сначала)
            comments = loadedComments
            isLoading = false
        }.onFailure { e ->
            error = e.message ?: "Ошибка при загрузке комментариев"
            isLoading = false
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF1A1A1A), // Темный фон
        contentColor = Color.White,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        Color(0xFF9C88FF).copy(alpha = 0.5f),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp) // Фиксированная высота
        ) {
            // Заголовок
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Комментарии",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }
            
            Divider(color = Color(0xFF9C88FF).copy(alpha = 0.3f))
            
            // Список комментариев
            Box(modifier = Modifier.weight(1f)) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF9C88FF))
                        }
                    }
                    error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Ошибка: $error",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пока нет комментариев",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(comments) { comment ->
                                CommentItem(comment = comment)
                            }
                        }
                    }
                }
            }
            
            Divider(color = Color(0xFF9C88FF).copy(alpha = 0.3f))
            
            // Поле ввода комментария
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            text = "Написать комментарий...",
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF9C88FF),
                        unfocusedBorderColor = Color(0xFF9C88FF).copy(alpha = 0.5f),
                        cursorColor = Color(0xFF9C88FF)
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (commentText.text.isNotBlank() && !isSending) {
                                scope.launch {
                                    isSending = true
                                    val result = commentRepository.createComment(
                                        token,
                                        videoId,
                                        commentText.text
                                    )
                                    result.onSuccess { newComment ->
                                        // Добавляем новый комментарий в начало списка (новые сначала)
                                        comments = listOf(newComment) + comments
                                        commentText = TextFieldValue("")
                                        error = null // Очищаем ошибку при успехе
                                        onCommentAdded()
                                    }.onFailure { e ->
                                        error = e.message ?: "Ошибка при отправке комментария"
                                    }
                                    isSending = false
                                }
                            }
                        }
                    ),
                    singleLine = false,
                    maxLines = 3
                )
                IconButton(
                    onClick = {
                        if (commentText.text.isNotBlank() && !isSending) {
                            scope.launch {
                                isSending = true
                                val result = commentRepository.createComment(
                                    token,
                                    videoId,
                                    commentText.text
                                )
                                result.onSuccess { newComment ->
                                    // Добавляем новый комментарий в начало списка (новые сначала)
                                    comments = listOf(newComment) + comments
                                    commentText = TextFieldValue("")
                                    error = null // Очищаем ошибку при успехе
                                    onCommentAdded()
                                }.onFailure { e ->
                                    error = e.message ?: "Ошибка при отправке комментария"
                                }
                                isSending = false
                            }
                        }
                    },
                    enabled = commentText.text.isNotBlank() && !isSending
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color(0xFF9C88FF),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Отправить",
                            tint = if (commentText.text.isNotBlank()) Color(0xFF9C88FF) else Color.White.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: CommentResponse) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${comment.username}",
                    color = Color(0xFF9C88FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = comment.text,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatCommentDate(comment.createdAt),
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

private fun formatCommentDate(dateString: String): String {
    return try {
        // Пробуем разные форматы даты, которые может вернуть бэкенд
        val formats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS",
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss"
        )
        val outputFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        
        var parsed = false
        var formattedDate = dateString
        
        for (format in formats) {
            try {
                val inputFormat = SimpleDateFormat(format, Locale.getDefault())
                val date = inputFormat.parse(dateString)
                if (date != null) {
                    formattedDate = outputFormat.format(date)
                    parsed = true
                    break
                }
            } catch (e: Exception) {
                // Пробуем следующий формат
            }
        }
        
        if (!parsed) {
            // Если не удалось распарсить, возвращаем исходную строку
            dateString
        } else {
            formattedDate
        }
    } catch (e: Exception) {
        dateString
    }
}

