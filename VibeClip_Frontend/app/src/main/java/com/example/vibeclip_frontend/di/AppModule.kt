package com.example.vibeclip_frontend.di

import android.content.Context
import com.example.vibeclip_frontend.data.repository.AuthRepository
import com.example.vibeclip_frontend.data.repository.AdminModerationRepository
import com.example.vibeclip_frontend.data.repository.CommentRepository
import com.example.vibeclip_frontend.data.repository.FolderRepository
import com.example.vibeclip_frontend.data.repository.ReactionRepository
import com.example.vibeclip_frontend.data.repository.ReportRepository
import com.example.vibeclip_frontend.data.repository.SubscriptionRepository
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.data.repository.VideoRepository
import com.example.vibeclip_frontend.util.ReportTrackerStore
import com.example.vibeclip_frontend.util.SubscribersStore
import com.example.vibeclip_frontend.util.SubscriptionsStore
import com.example.vibeclip_frontend.util.TokenManager
import com.example.vibeclip_frontend.util.VideoFeedVisibilityFilter

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
    
    lateinit var reactionRepository: ReactionRepository
        private set

    lateinit var reportRepository: ReportRepository
        private set

    lateinit var adminModerationRepository: AdminModerationRepository
        private set
    
    lateinit var commentRepository: CommentRepository
        private set

    lateinit var subscriptionRepository: SubscriptionRepository
        private set

    lateinit var subscriptionsStore: SubscriptionsStore
        private set

    lateinit var subscribersStore: SubscribersStore
        private set

    lateinit var reportTrackerStore: ReportTrackerStore
        private set

    lateinit var videoFeedVisibilityFilter: VideoFeedVisibilityFilter
        private set
    
    fun initialize(context: Context) {
        tokenManager = TokenManager(context)
        authRepository = AuthRepository()
        videoRepository = VideoRepository()
        folderRepository = FolderRepository()
        userRepository = UserRepository()
        reactionRepository = ReactionRepository()
        reportTrackerStore = ReportTrackerStore(context)
        reportRepository = ReportRepository(reportTrackerStore)
        adminModerationRepository = AdminModerationRepository(reportTrackerStore)
        commentRepository = CommentRepository()
        subscriptionRepository = SubscriptionRepository()
        subscriptionsStore = SubscriptionsStore(context)
        subscribersStore = SubscribersStore(context)
        videoFeedVisibilityFilter = VideoFeedVisibilityFilter(userRepository, subscriptionsStore)
    }
}


