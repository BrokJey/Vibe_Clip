package com.example.vibeclip_frontend.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vibeclip_frontend.R
import com.example.vibeclip_frontend.data.model.VideoResponse
import com.example.vibeclip_frontend.util.MediaUrlResolver

@Composable
fun ProfileHeader(
    username: String,
    avatarUrl: String?,
    subtitle: String? = null,
    showAvatarEdit: Boolean = false,
    onAvatarEditClick: (() -> Unit)? = null,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            ProfileAvatar(avatarUrl = avatarUrl, size = 120.dp)
            if (showAvatarEdit && onAvatarEditClick != null) {
                IconButton(
                    onClick = onAvatarEditClick,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .zIndex(1f)
                        .size(40.dp)
                        .background(Color(0xFF9C88FF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Изменить фото",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Text(
            text = "@$username",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        action?.invoke()
    }
}

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 120.dp
) {
    val resolvedAvatar = remember(avatarUrl) { MediaUrlResolver.resolve(avatarUrl) }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.Gray.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (resolvedAvatar != null) {
            AsyncImage(
                model = resolvedAvatar,
                contentDescription = "Аватар",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.profile),
                placeholder = painterResource(R.drawable.profile)
            )
        } else {
            Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = "Аватар",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ProfileVideoGrid(
    videos: List<VideoResponse>,
    isLoading: Boolean,
    emptyMessage: String,
    readOnly: Boolean,
    onVideoClick: (VideoResponse) -> Unit,
    onDeleteClick: ((VideoResponse) -> Unit)? = null,
    onEditClick: ((VideoResponse) -> Unit)? = null,
    onLoadMore: (() -> Unit)? = null,
    hasMore: Boolean = false
) {
    when {
        isLoading && videos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        videos.isEmpty() -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emptyMessage,
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
                itemsIndexed(videos) { index, video ->
                    if (index >= videos.size - 4 && hasMore && onLoadMore != null) {
                        LaunchedEffect(index, videos.size) { onLoadMore() }
                    }
                    ProfileVideoGridItem(
                        video = video,
                        readOnly = readOnly,
                        onVideoClick = { onVideoClick(video) },
                        onDeleteClick = onDeleteClick?.let { { it(video) } },
                        onEditClick = onEditClick?.let { { it(video) } }
                    )
                }
                if (isLoading) {
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

@Composable
fun ProfileVideoGridItem(
    video: VideoResponse,
    readOnly: Boolean,
    onVideoClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    onEditClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onVideoClick)
    ) {
        val thumbnailUrl = remember(video.thumbnailUrl) {
            MediaUrlResolver.resolve(video.thumbnailUrl)
        }

        if (thumbnailUrl != null) {
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

        if (!readOnly) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onEditClick?.invoke() },
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
                    onClick = { onDeleteClick?.invoke() },
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
        }

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
