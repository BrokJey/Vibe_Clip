package com.example.vibeclip_frontend.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.vibeclip_frontend.ui.components.ProfileAvatar

private val Purple = Color(0xFF9C88FF)
private val SaveGray = Color(0xFFBDBDBD)

@Composable
fun AvatarEditSheet(
    visible: Boolean,
    initialAvatarUrl: String?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (pickedAvatarUri: Uri) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .fillMaxSize()
            .zIndex(10f),
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it })
    ) {
        var pickedAvatarUri by remember(visible) { mutableStateOf<Uri?>(null) }

        val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) pickedAvatarUri = uri
        }

        val hasChanges = pickedAvatarUri != null

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(enabled = !isSaving) { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.92f)
                    .align(Alignment.CenterEnd)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(enabled = false) { }
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss, enabled = !isSaving) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Закрыть")
                    }
                    Text(
                        text = "Фото профиля",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (pickedAvatarUri != null) {
                        AsyncImage(
                            model = pickedAvatarUri,
                            contentDescription = "Новый аватар",
                            modifier = Modifier
                                .size(160.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        ProfileAvatar(avatarUrl = initialAvatarUrl, size = 160.dp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    OutlinedButton(
                        onClick = { pickAvatar.launch("image/*") },
                        enabled = !isSaving
                    ) {
                        Text("Выбрать фото")
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    }
                }

                Button(
                    onClick = { pickedAvatarUri?.let { onSave(it) } },
                    enabled = hasChanges && !isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (hasChanges) Purple else SaveGray,
                        disabledContainerColor = SaveGray,
                        disabledContentColor = Color.White.copy(alpha = 0.8f)
                    )
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text("Сохранить", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
