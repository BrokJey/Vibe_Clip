package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.layout.*
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.ui.viewmodel.VideoUploadViewModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import android.media.MediaMetadataRetriever
import okhttp3.RequestBody

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoUploadScreen(
    token: String,
    onUploaded: () -> Unit,
    viewModel: VideoUploadViewModel = viewModel { VideoUploadViewModel(VideoRepository(), token) }
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var hashtags by remember { mutableStateOf("") }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var thumbUri by remember { mutableStateOf<Uri?>(null) }
    var durationSec by remember { mutableStateOf<Int?>(null) }
    var durationError by remember { mutableStateOf<String?>(null) }

    val pickVideo = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        videoUri = uri
        durationError = null
        durationSec = uri?.let { getDurationSeconds(context, it) }
        if (durationSec != null && durationSec!! > 180) {
            durationError = "Видео длиннее 3 минут"
        } else if (durationSec == null) {
            durationError = "Не удалось определить длительность"
        }
    }

    val pickThumb = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        thumbUri = uri
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.created) {
        if (uiState.created != null) {
            onUploaded()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Загрузка видео", style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Заголовок") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Описание") },
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { pickVideo.launch("video/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (videoUri != null) "Видео выбрано" else "Выбрать видео файл")
        }
        if (videoUri != null) {
            Text("Видео: ${videoUri?.lastPathSegment}", style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { pickThumb.launch("image/*") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (thumbUri != null) "Превью выбрано" else "Выбрать превью")
        }
        if (thumbUri != null) {
            Text("Превью: ${thumbUri?.lastPathSegment}", style = MaterialTheme.typography.bodySmall)
        }

        if (durationError != null) {
            Text(durationError!!, color = MaterialTheme.colorScheme.error)
        } else if (durationSec != null) {
            Text("Длительность: ${durationSec} сек", style = MaterialTheme.typography.bodySmall)
        }

        OutlinedTextField(
            value = hashtags,
            onValueChange = { hashtags = it },
            label = { Text("Хэштеги через запятую") },
            modifier = Modifier.fillMaxWidth()
        )

        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage!!,
                color = MaterialTheme.colorScheme.error
            )
        }

        Button(
            onClick = {
                val video = videoUri
                val thumb = thumbUri
                val dur = durationSec
                if (video == null) {
                    Toast.makeText(context, "Выберите видео", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (thumb == null) {
                    Toast.makeText(context, "Выберите превью", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (dur == null || dur <= 0 || dur > 180) {
                    Toast.makeText(context, "Неверная длительность", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val videoPart = buildFilePart(context, video, "file", "video/*")
                val thumbPart = buildFilePart(context, thumb, "thumbnail", "image/*")

                viewModel.upload(
                    title = title,
                    description = description,
                    hashtags = hashtags,
                    duration = dur,
                    filePart = videoPart,
                    thumbnailPart = thumbPart
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            enabled = !uiState.isLoading && title.isNotBlank() && videoUri != null && thumbUri != null && durationError == null
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Загрузить")
            }
        }
    }
}

private fun getDurationSeconds(context: Context, uri: Uri): Int? {
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(context, uri)
        val durMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        retriever.release()
        durMs?.let { (it / 1000).toInt() }
    } catch (_: Exception) {
        null
    }
}

private fun buildFilePart(context: Context, uri: Uri, name: String, defaultMime: String): MultipartBody.Part {
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: defaultMime
    val input = cr.openInputStream(uri) ?: throw IllegalStateException("Не удалось открыть файл")
    val bytes = input.readBytes()
    input.close()
    val requestBody: RequestBody = bytes.toRequestBody(mime.toMediaTypeOrNull())
    val filename = uri.lastPathSegment ?: "upload.bin"
    return MultipartBody.Part.createFormData(name, filename, requestBody)
}

