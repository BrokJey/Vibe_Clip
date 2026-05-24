package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.vibeclip_frontend.data.model.SubscriberListItem
import com.example.vibeclip_frontend.data.model.StoredSubscription
import com.example.vibeclip_frontend.ui.components.ProfileAvatar

@Composable
fun UsersListDialog(
    title: String,
    users: List<StoredSubscription>,
    emptyMessage: String,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit,
    pendingRequests: List<SubscriberListItem> = emptyList(),
    onAcceptRequest: ((String) -> Unit)? = null,
    onRejectRequest: ((String) -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть")
                    }
                }

                val isEmpty = users.isEmpty() && pendingRequests.isEmpty()

                if (isEmpty) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emptyMessage,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (pendingRequests.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Заявки",
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            }
                            items(pendingRequests, key = { "pending_${it.userId}" }) { item ->
                                PendingSubscriberRow(
                                    item = item,
                                    onAccept = onAcceptRequest,
                                    onReject = onRejectRequest
                                )
                            }
                        }

                        if (users.isNotEmpty()) {
                            if (pendingRequests.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Подписчики",
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            items(users, key = { it.userId }) { user ->
                                UserListRow(
                                    username = user.username,
                                    avatarUrl = user.avatarUrl,
                                    isPending = user.isPending,
                                    onClick = { onUserClick(user.username) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserListRow(
    username: String,
    avatarUrl: String?,
    isPending: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileAvatar(avatarUrl = avatarUrl, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "@$username",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (isPending) {
                Text(
                    text = "Ожидает подтверждения",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

@Composable
private fun PendingSubscriberRow(
    item: SubscriberListItem,
    onAccept: ((String) -> Unit)?,
    onReject: ((String) -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProfileAvatar(avatarUrl = item.avatarUrl, size = 44.dp)

        Text(
            text = "@${item.username}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .widthIn(min = 48.dp)
        )

        if (onAccept != null) {
            Button(
                onClick = { onAccept(item.userId) },
                modifier = Modifier
                    .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                    .heightIn(max = 36.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            ) {
                Text("Принять", fontSize = 11.sp, maxLines = 1)
            }
        }
        if (onReject != null) {
            OutlinedButton(
                onClick = { onReject(item.userId) },
                modifier = Modifier
                    .defaultMinSize(minWidth = 1.dp, minHeight = 36.dp)
                    .heightIn(max = 36.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
            ) {
                Text("Отклонить", fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}
