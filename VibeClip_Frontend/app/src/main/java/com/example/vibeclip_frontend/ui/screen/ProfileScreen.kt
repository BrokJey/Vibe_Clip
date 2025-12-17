package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.vibeclip_frontend.BuildConfig
import com.example.vibeclip_frontend.R
import com.example.vibeclip_frontend.data.model.VideoRequest
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,
    onLogout: () -> Unit,
    onVideoClick: (VideoResponse) -> Unit,
    viewModel: ProfileViewModel = viewModel { 
        ProfileViewModel(UserRepository(), VideoRepository(), token) 
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user
    var showDeleteDialog by remember { mutableStateOf<VideoResponse?>(null) }
    var showEditDialog by remember { mutableStateOf<VideoResponse?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { Text("Профиль") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Выход") }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && user == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.errorMessage != null && user == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.refresh() }) { Text("Повторить") }
                }
            }
            user != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    // Верхняя часть: аватар, имя, email
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Аватар
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape)
                                .background(Color.Gray.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(R.drawable.profile),
                                contentDescription = "Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        
                        // Username
                        Text(
                            text = user.username,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Email
                        Text(
                            text = user.email,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    
                    Divider()
                    
                    // Нижняя часть: сетка с видео
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    ) {
                        when {
                            uiState.isLoadingVideos && uiState.videos.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            uiState.errorMessage != null && uiState.videos.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Ошибка загрузки",
                                            color = MaterialTheme.colorScheme.error,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = uiState.errorMessage!!,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                        Button(onClick = { viewModel.loadVideos(0) }) {
                                            Text("Повторить")
                                        }
                                    }
                                }
                            }
                            uiState.videos.isEmpty() -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "У вас пока нет видео",
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            else -> {
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(3),
                                    contentPadding = PaddingValues(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(uiState.videos.size) { index ->
                                        val video = uiState.videos[index]
                                        
                                        // Загружаем больше видео когда доходим до конца
                                        LaunchedEffect(index) {
                                            if (index >= uiState.videos.size - 4 && uiState.hasMore && !uiState.isLoadingVideos) {
                                                viewModel.loadMore()
                                            }
                                        }
                                        
                                        VideoGridItem(
                                            video = video,
                                            onVideoClick = { onVideoClick(video) },
                                            onDeleteClick = { showDeleteDialog = video },
                                            onEditClick = { showEditDialog = video }
                                        )
                                    }
                                    
                                    if (uiState.isLoadingVideos) {
                                        item(span = { GridItemSpan(3) }) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                CircularProgressIndicator()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Диалог удаления
    showDeleteDialog?.let { video ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Удалить видео?") },
            text = { Text("Вы уверены, что хотите удалить видео ${video.title.trim('"', '\'')}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVideo(video.id) {
                            showDeleteDialog = null
                        }
                    }
                ) {
                    Text("Удалить", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог редактирования
    showEditDialog?.let { video ->
        EditVideoDialog(
            video = video,
            onDismiss = { showEditDialog = null },
            onSave = { title, description, hashtags ->
                viewModel.updateVideo(
                    video.id,
                    VideoRequest(
                        title = title.ifBlank { null },
                        description = description.ifBlank { null },
                        hashtags = hashtags.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
                    )
                ) {
                    showEditDialog = null
                }
            }
        )
    }
}

@Composable
fun VideoGridItem(
    video: VideoResponse,
    onVideoClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onVideoClick)
    ) {
        // Превью видео
        if (!video.thumbnailUrl.isNullOrBlank()) {
            // Формируем правильный URL для превью (аналогично FeedScreen)
            val baseHost = remember {
                BuildConfig.API_BASE_URL
                    .removeSuffix("/")
                    .substringBefore("/api")
            }
            val thumbnailUrl = remember(video.thumbnailUrl) {
                val url = video.thumbnailUrl!!
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    url
                } else {
                    "$baseHost/${url.trimStart('/')}"
                }
            }
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = video.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.vc_logo),
                    contentDescription = video.title,
                    modifier = Modifier.size(48.dp)
                )
            }
        }
        
        // Затемнение для кнопок
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
        )
        
        // Кнопки действий в правом верхнем углу
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Редактировать",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // Название видео внизу
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = video.title.trim('"', '\''),
                color = Color.White,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EditVideoDialog(
    video: VideoResponse,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(video.title) }
    var description by remember { mutableStateOf(video.description ?: "") }
    var hashtags by remember { mutableStateOf(video.hashtags.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Редактировать видео",
                    style = MaterialTheme.typography.titleLarge
                )
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp),
                    maxLines = 5
                )
                
                OutlinedTextField(
                    value = hashtags,
                    onValueChange = { hashtags = it },
                    label = { Text("Хештеги (через запятую)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("пример, тест, видео") }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Отмена")
                    }
                    
                    Button(
                        onClick = { onSave(title, description, hashtags) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Сохранить")
                    }
                }
            }
        }
    }
}
