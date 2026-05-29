package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.vibeclip_frontend.data.model.ModerationVideoItem
import com.example.vibeclip_frontend.ui.components.ErrorContent
import com.example.vibeclip_frontend.util.MediaUrlResolver

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AdminReportsSheet(
    visible: Boolean,
    isLoading: Boolean,
    isActionLoading: Boolean,
    items: List<ModerationVideoItem>,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onRejectReports: (String) -> Unit,
    onBlockVideo: (String) -> Unit,
    onClearAllReports: () -> Unit,
    onOpenUser: (String) -> Unit,
    onRetry: () -> Unit
) {
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var reportersDialog by remember { mutableStateOf<Pair<String, List<String>>?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Список жалоб на видео",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClearAllReports, enabled = items.isNotEmpty() && !isActionLoading) {
                    Icon(Icons.Default.Block, contentDescription = "Сбросить все жалобы")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    ErrorContent(
                        message = errorMessage,
                        modifier = Modifier.fillMaxWidth(),
                        showRetry = true,
                        onRetry = onRetry
                    )
                }
                items.isEmpty() -> {
                    Text(
                        text = "Жалоб пока нет",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(vertical = 18.dp)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(items, key = { it.id }) { item ->
                            ReportVideoRow(
                                item = item,
                                isActionLoading = isActionLoading,
                                onReject = { onRejectReports(item.id) },
                                onBlock = { onBlockVideo(item.id) },
                                onShowReporters = {
                                    reportersDialog = item.title to item.reporterUsernames
                                },
                                onOpenAuthor = { onOpenUser(item.authorUsername) }
                            )
                        }
                    }
                }
            }
        }
    }

    reportersDialog?.let { (videoTitle, usernames) ->
        ReportersDialog(
            title = videoTitle,
            usernames = usernames,
            onDismiss = { reportersDialog = null },
            onOpenUser = onOpenUser
        )
    }
}

@Composable
private fun ReportVideoRow(
    item: ModerationVideoItem,
    isActionLoading: Boolean,
    onReject: () -> Unit,
    onBlock: () -> Unit,
    onShowReporters: () -> Unit,
    onOpenAuthor: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val purple = Color(0xFF9C88FF)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, purple, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumb = remember(item.thumbnailUrl) { MediaUrlResolver.resolve(item.thumbnailUrl) }
        Box(
            modifier = Modifier
                .size(width = 72.dp, height = 112.dp)
                .border(2.dp, purple, RoundedCornerShape(10.dp))
                .background(Color.Black, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (thumb != null) {
                AsyncImage(
                    model = thumb,
                    contentDescription = "Превью",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text("Нет\nпревью", color = Color.White, maxLines = 2)
            }
        }

        Spacer(modifier = Modifier.size(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "@${item.authorUsername}",
                color = Color(0xFF9C88FF),
                modifier = Modifier.clickable(onClick = onOpenAuthor)
            )
        }

        Spacer(modifier = Modifier.size(8.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .background(purple, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = item.reportCount.toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }

        Box {
            IconButton(onClick = { expanded = true }, enabled = !isActionLoading) {
                Icon(Icons.Default.MoreVert, contentDescription = "Опции")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Отклонить жалобы") },
                    onClick = {
                        expanded = false
                        onReject()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Заблокировать видео") },
                    onClick = {
                        expanded = false
                        onBlock()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Посмотреть авторов жалоб") },
                    onClick = {
                        expanded = false
                        onShowReporters()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReportersDialog(
    title: String,
    usernames: List<String>,
    onDismiss: () -> Unit,
    onOpenUser: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Авторы жалоб",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть")
                }
            }
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (usernames.isEmpty()) {
                Text(
                    "Список жалобщиков пуст. Имена появятся после жалоб из приложения на этом устройстве.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    items(usernames.distinct(), key = { it }) { username ->
                        TextButton(
                            onClick = {
                                if (username.isNotBlank()) onOpenUser(username)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "@$username",
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
