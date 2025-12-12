package com.example.vibeclip_frontend.di

import android.content.Context
import com.example.vibeclip_frontend.data.repository.AuthRepository
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.util.TokenManager

object AppModule {
    lateinit var tokenManager: TokenManager
        private set
    
    lateinit var authRepository: AuthRepository
        private set
    
    lateinit var videoRepository: VideoRepository
        private set

    lateinit var folderRepository: FolderRepository
        private set

    lateinit var userRepository: UserRepository
        private set
    
    fun initialize(context: Context) {
        tokenManager = TokenManager(context)
        authRepository = AuthRepository()
        videoRepository = VideoRepository()
        folderRepository = FolderRepository()
        userRepository = UserRepository()
    }
}


