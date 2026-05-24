package com.example.vibeclip_frontend.util

import android.content.Context
import android.net.Uri
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun buildImagePart(
    context: Context,
    uri: Uri,
    partName: String,
    defaultMime: String = "image/*"
): MultipartBody.Part {
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: defaultMime
    val inputStream = contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("Не удалось открыть файл")
    val tempFile = File.createTempFile("upload", null, context.cacheDir)
    tempFile.outputStream().use { output ->
        inputStream.use { input -> input.copyTo(output) }
    }
    val requestBody = tempFile.asRequestBody(mimeType.toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, tempFile.name, requestBody)
}
