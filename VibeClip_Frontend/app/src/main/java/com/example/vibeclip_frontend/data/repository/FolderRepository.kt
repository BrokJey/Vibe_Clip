package com.example.vibeclip_frontend.data.repository

import com.example.vibeclip_frontend.data.RetrofitClient
import com.example.vibeclip_frontend.data.model.FolderFeedResponse
import com.example.vibeclip_frontend.data.model.FolderRequest
import com.example.vibeclip_frontend.data.model.FolderResponse

class FolderRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun list(token: String): Result<List<FolderResponse>> = runCatching {
        val resp = apiService.getFolders("Bearer $token")
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun create(token: String, request: FolderRequest): Result<FolderResponse> = runCatching {
        val resp = apiService.createFolder("Bearer $token", request)
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }

    suspend fun get(token: String, folderId: String): Result<FolderResponse> = runCatching {
        val resp = apiService.getFolder("Bearer $token", folderId)
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
    
    suspend fun update(token: String, folderId: String, request: FolderRequest): Result<FolderResponse> = runCatching {
        val resp = apiService.updateFolder("Bearer $token", folderId, request)
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
    
    suspend fun delete(token: String, folderId: String): Result<Unit> = runCatching {
        val resp = apiService.deleteFolder("Bearer $token", folderId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
    
    suspend fun archive(token: String, folderId: String): Result<Unit> = runCatching {
        val resp = apiService.archiveFolder("Bearer $token", folderId)
        if (resp.isSuccessful) Unit
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
    
    suspend fun regenerateFeed(token: String, folderId: String, limit: Int = 20): Result<FolderFeedResponse> = runCatching {
        val resp = apiService.regenerateFolderFeed("Bearer $token", folderId, limit)
        if (resp.isSuccessful && resp.body() != null) resp.body()!!
        else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
    }
    
    suspend fun feed(token: String, folderId: String, page: Int = 0, size: Int = 20): Result<FolderFeedResponse> =
        runCatching {
            val resp = apiService.getFolderFeed("Bearer $token", folderId, page, size)
            if (resp.isSuccessful && resp.body() != null) resp.body()!!
            else throw Exception(resp.errorBody()?.string().orEmpty().ifBlank { resp.message() })
        }
}

