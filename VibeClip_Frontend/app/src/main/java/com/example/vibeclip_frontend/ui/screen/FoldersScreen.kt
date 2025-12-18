package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.ui.viewmodel.FolderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    token: String,
    onOpenFolder: (String) -> Unit,
    viewModel: FolderViewModel = viewModel { FolderViewModel(FolderRepository(), token) }
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    // Храним маппинг: нормализованный хэштег (для отображения) -> исходный хэштег (как в базе)
    var hashtagMapping by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var allHashtags by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedHashtags by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isLoadingHashtags by remember { mutableStateOf(false) }
    var editingFolderId by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    LaunchedEffect(uiState.created) {
        if (uiState.created != null) {
            name = ""
            description = ""
            selectedHashtags = emptySet()
            viewModel.clearCreated()
        }
    }

    // Загружаем доступные хэштеги из ленты видео, чтобы предлагать их при настройке папки
    LaunchedEffect(Unit) {
        isLoadingHashtags = true
        val videoRepo = VideoRepository()
        videoRepo.getVideos(token = token, page = 0, size = 50)
            .onSuccess { listResponse ->
                val tags = mutableSetOf<String>()
                val mapping = mutableMapOf<String, String>()
                listResponse.content.forEach { video ->
                    video.hashtags.forEach { rawTag ->
                        val cleaned = rawTag.trim().trim('"')
                        if (cleaned.isNotBlank()) {
                            // Нормализуем для отображения (убираем #)
                            val normalized = cleaned.trimStart('#')
                            if (normalized.isNotBlank()) {
                                // Сохраняем маппинг: нормализованный -> исходный (как в базе)
                                // Используем исходный формат (с # или без), как он хранится в базе
                                mapping[normalized] = cleaned
                                tags.add(normalized)
                            }
                        }
                    }
                }
                hashtagMapping = mapping
                allHashtags = tags.toList().sorted()
            }
            .onFailure {
                // В случае ошибки просто не показываем подсказки по хэштегам
            }
        isLoadingHashtags = false
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Папки") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Создать папку", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Описание (опционально)") },
                modifier = Modifier.fillMaxWidth()
            )

            // Блок выбора хэштегов для папки (настройка рекомендаций)
            Text(
                text = "Хэштеги для этой папки (рекомендации)",
                style = MaterialTheme.typography.bodyMedium
            )
            if (isLoadingHashtags) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else if (allHashtags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(allHashtags) { tag ->
                        val isSelected = selectedHashtags.contains(tag)
                        // Нормализуем для отображения, чтобы не было двойных решёток
                        val displayTag = tag.trim().trim('"').trimStart('#')
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedHashtags = if (isSelected) {
                                    selectedHashtags - tag
                                } else {
                                    selectedHashtags + tag
                                }
                            },
                            label = { Text("#$displayTag") }
                        )
                    }
                }
            } else {
                Text(
                    text = "Пока нет хэштегов для подсказок — они появятся по мере загрузки видео.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    // Преобразуем выбранные нормализованные хэштеги в формат с # для отправки на бэкенд
                    // Всегда отправляем только варианты с # в начале
                    val hashtagsWithHash = mutableSetOf<String>()
                    selectedHashtags.forEach { normalized ->
                        // Гарантируем формат #хештег
                        val hashtagWithHash = if (normalized.startsWith("#")) {
                            normalized
                        } else {
                            "#$normalized"
                        }
                        hashtagsWithHash.add(hashtagWithHash)
                    }
                    if (editingFolderId == null) {
                        viewModel.create(name, description, hashtagsWithHash.toList())
                    } else {
                        viewModel.update(editingFolderId!!, name, description, hashtagsWithHash.toList())
                    }
                },
                enabled = name.isNotBlank() && !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (editingFolderId == null) "Создать" else "Сохранить")
                }
            }

            if (uiState.errorMessage != null) {
                Text(
                    text = uiState.errorMessage!!,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Список папок", style = MaterialTheme.typography.titleMedium)

            when {
                uiState.isLoading && uiState.folders.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }
                uiState.folders.isEmpty() -> {
                    Text("Папок пока нет")
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.folders) { folder ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenFolder(folder.id) },
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(folder.name, style = MaterialTheme.typography.titleMedium)
                                        FolderMoreMenu(
                                            onOpen = { onOpenFolder(folder.id) },
                                            onEdit = {
                                                editingFolderId = folder.id
                                                name = folder.name
                                                description = folder.description.orEmpty()
                                                val pref = folder.preference
                                                val allowed = pref?.allowedHashtags
                                                if (allowed != null && allowed.isNotEmpty()) {
                                                    val normalizedSelected = allowed.map { it.trim().trim('"').trimStart('#') }
                                                    selectedHashtags = normalizedSelected.toSet()
                                                } else {
                                                    selectedHashtags = emptySet()
                                                }
                                            },
                                            onArchive = { viewModel.archive(folder.id) },
                                            onDelete = { viewModel.delete(folder.id) },
                                            onRegenerate = { viewModel.regenerateFeed(folder.id) }
                                        )
                                    }
                                    folder.description?.let {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(it, style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Статус: ${folder.status}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

