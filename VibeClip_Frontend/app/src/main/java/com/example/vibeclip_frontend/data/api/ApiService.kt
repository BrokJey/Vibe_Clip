package com.example.vibeclip_frontend.data.api

import com.example.vibeclip_frontend.data.model.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    // Auth endpoints
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>
    
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>
    
    // Video endpoints
    @GET("videos")
    suspend fun getVideos(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("recommended") recommended: Boolean? = null,
        @Query("randomPercentage") randomPercentage: Double? = null
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
        @Part thumbnail: MultipartBody.Part?,
        @Part("title") title: String?,
        @Part("description") description: String?,
        @Part("hashtags") hashtags: String?,
        @Part("durationSeconds") durationSeconds: Int
    ): Response<VideoResponse>
    
    @GET("videos/{id}/share-url")
    suspend fun getShareUrl(@Path("id") id: String): Response<Map<String, String>>
    
    @GET("videos/my")
    suspend fun getMyVideos(
        @Header("Authorization") token: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("status") status: String? = null
    ): Response<VideoListResponse>
    
    @PUT("videos/{id}")
    suspend fun updateVideo(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body request: VideoRequest
    ): Response<VideoResponse>
    
    @DELETE("videos/{id}")
    suspend fun deleteVideo(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
    
    // Reaction endpoints
    @POST("reactions")
    suspend fun createReaction(
        @Header("Authorization") token: String,
        @Body request: ReactionRequest
    ): Response<ReactionResponse>
    
    @DELETE("reactions/video/{videoId}")
    suspend fun deleteReaction(
        @Header("Authorization") token: String,
        @Path("videoId") videoId: String,
        @Query("reactionType") reactionType: String
    ): Response<Unit>
    
    @GET("reactions/video/{videoId}")
    suspend fun getVideoReactions(
        @Header("Authorization") token: String,
        @Path("videoId") videoId: String
    ): Response<List<ReactionResponse>>
    
    // Comment endpoints
    @GET("videos/{id}/comments")
    suspend fun getVideoComments(
        @Header("Authorization") token: String,
        @Path("id") videoId: String
    ): Response<List<CommentResponse>>
    
    @POST("videos/{id}/comments")
    suspend fun createComment(
        @Header("Authorization") token: String,
        @Path("id") videoId: String,
        @Body request: CommentRequest
    ): Response<CommentResponse>
    
    // Folder endpoints
    @GET("folders")
    suspend fun getFolders(@Header("Authorization") token: String): Response<List<FolderResponse>>
    
    @POST("folders")
    suspend fun createFolder(
        @Header("Authorization") token: String,
        @Body request: FolderRequest
    ): Response<FolderResponse>

    @GET("folders/{id}")
    suspend fun getFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: String
    ): Response<FolderResponse>
    
    @PUT("folders/{id}")
    suspend fun updateFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: String,
        @Body request: FolderRequest
    ): Response<FolderResponse>
    
    @DELETE("folders/{id}")
    suspend fun deleteFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: String
    ): Response<Unit>
    
    @POST("folders/{id}/archive")
    suspend fun archiveFolder(
        @Header("Authorization") token: String,
        @Path("id") folderId: String
    ): Response<Unit>
    
    @POST("folders/{id}/regenerate")
    suspend fun regenerateFolderFeed(
        @Header("Authorization") token: String,
        @Path("id") folderId: String,
        @Query("limit") limit: Int = 20
    ): Response<FolderFeedResponse>
    
    @GET("folders/{folderId}/feed")
    suspend fun getFolderFeed(
        @Header("Authorization") token: String,
        @Path("folderId") folderId: String,
        @Query("limit") limit: Int = 20
    ): Response<FolderFeedResponse>
    
    // User endpoints
    @GET("users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): Response<UserResponse>
}

