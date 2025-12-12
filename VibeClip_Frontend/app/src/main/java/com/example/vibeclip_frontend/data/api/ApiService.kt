package com.example.vibeclip_frontend.data.api

import com.example.vibeclip_frontend.data.model.*
import retrofit2.Response
import retrofit2.http.*
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface ApiService {
    
    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // User endpoints
    @GET("users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserResponse>
    
    // Video endpoints
    @GET("videos")
    suspend fun getVideos(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<VideoListResponse>
    
    @GET("videos/{id}")
    suspend fun getVideo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<VideoResponse>
    
    @POST("videos")
    suspend fun createVideo(
        @Header("Authorization") token: String,
        @Body request: VideoRequest
    ): Response<VideoResponse>

    @Multipart
    @POST("videos/upload")
    suspend fun uploadVideo(
        @Header("Authorization") token: String,
        @Part file: MultipartBody.Part,
        @Part thumbnail: MultipartBody.Part,
        @Part("title") title: String?,
        @Part("description") description: String?,
        @Part("hashtags") hashtags: String?,
        @Part("durationSeconds") durationSeconds: Int
    ): Response<VideoResponse>
    
    // Folder endpoints
    @GET("folders")
    suspend fun getFolders(@Header("Authorization") token: String): Response<List<FolderResponse>>
    
    @GET("folders/{id}")
    suspend fun getFolder(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<FolderResponse>
    
    @POST("folders")
    suspend fun createFolder(
        @Header("Authorization") token: String,
        @Body request: FolderRequest
    ): Response<FolderResponse>
    
    @GET("folders/{id}/feed")
    suspend fun getFolderFeed(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<FolderFeedResponse>
    
    // Reaction endpoints
    @POST("reactions")
    suspend fun createReaction(
        @Header("Authorization") token: String,
        @Body request: ReactionRequest
    ): Response<ReactionResponse>
    
    @DELETE("reactions/{id}")
    suspend fun deleteReaction(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
}


