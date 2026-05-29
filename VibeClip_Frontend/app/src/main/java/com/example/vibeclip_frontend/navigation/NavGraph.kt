package com.example.vibeclip_frontend.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.vibeclip_frontend.ui.screen.FeedScreen
import com.example.vibeclip_frontend.ui.screen.FolderFeedScreen
import com.example.vibeclip_frontend.ui.screen.FoldersScreen
import com.example.vibeclip_frontend.ui.screen.LoginScreen
import com.example.vibeclip_frontend.ui.screen.ProfileScreen
import com.example.vibeclip_frontend.ui.screen.RegisterScreen
import com.example.vibeclip_frontend.ui.screen.VideoUploadScreen
import com.example.vibeclip_frontend.ui.screen.SingleVideoScreen
import com.example.vibeclip_frontend.ui.screen.UserProfileFeedScreen
import com.example.vibeclip_frontend.ui.screen.UserProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Feed : Screen("feed")
    data class FeedWithVideo(val videoId: String) : Screen("feed/$videoId") {
        companion object {
            const val routePattern = "feed/{videoId}"
        }
    }
    object Upload : Screen("upload")
    object Folders : Screen("folders")
    object Profile : Screen("profile")
    data class FolderFeed(val folderId: String) : Screen("folder_feed/$folderId") {
        companion object {
            const val routePattern = "folder_feed/{folderId}"
        }
    }

    data class UserProfile(val username: String) : Screen("user/{username}") {
        companion object {
            const val routePattern = "user/{username}"
            fun createRoute(username: String): String = "user/${Uri.encode(username)}"
        }
    }

    data class UserProfileFeed(val username: String, val videoId: String) :
        Screen("user/{username}/video/{videoId}") {
        companion object {
            const val routePattern = "user/{username}/video/{videoId}"
            fun createRoute(username: String, videoId: String): String =
                "user/${Uri.encode(username)}/video/$videoId"
        }
    }

    data class OwnVideo(val videoId: String) : Screen("my_video/{videoId}") {
        companion object {
            const val routePattern = "my_video/{videoId}"
            fun createRoute(videoId: String): String = "my_video/$videoId"
        }
    }
}

fun NavController.navigateToUserProfile(username: String) {
    navigate(Screen.UserProfile.createRoute(username)) {
        launchSingleTop = true
    }
}

@Composable
fun NavGraph(
    navController: NavHostController,
    token: String?,
    onTokenUpdated: (String) -> Unit,
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = if (token != null) Screen.Feed.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = { newToken ->
                    onTokenUpdated(newToken)
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = { newToken ->
                    onTokenUpdated(newToken)
                    navController.navigate(Screen.Feed.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Feed.route) {
            if (token != null) {
                FeedScreen(
                    token = token,
                    navController = navController,
                    initialVideoId = null,
                    onLogout = {
                        onLogout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Feed.route) { inclusive = true }
                        }
                    }
                )
            }
        }
        
        composable(Screen.FeedWithVideo.routePattern) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId")
            if (token != null && videoId != null) {
                FeedScreen(
                    token = token,
                    navController = navController,
                    initialVideoId = videoId,
                    onLogout = {
                        onLogout()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Feed.route) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Screen.Upload.route) {
            if (token != null) {
                VideoUploadScreen(
                    token = token,
                    onUploaded = { navController.navigate(Screen.Feed.route) { launchSingleTop = true } }
                )
            }
        }

        composable(Screen.Folders.route) {
            if (token != null) {
                FoldersScreen(
                    token = token,
                    onOpenFolder = { folderId ->
                        navController.navigate(Screen.FolderFeed(folderId).route)
                    }
                )
            }
        }

        composable(Screen.FolderFeed.routePattern) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId")
            if (token != null && folderId != null) {
                FolderFeedScreen(
                    token = token,
                    folderId = folderId,
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Profile.route) {
            if (token != null) {
                ProfileScreen(
                    token = token,
                    onLogout = onLogout,
                    onVideoClick = { video ->
                        navController.navigate(Screen.OwnVideo.createRoute(video.id)) {
                            launchSingleTop = true
                        }
                    },
                    onNavigateToUser = { username ->
                        navController.navigateToUserProfile(username)
                    }
                )
            }
        }

        composable(Screen.OwnVideo.routePattern) { backStackEntry ->
            val videoId = backStackEntry.arguments?.getString("videoId")
            if (token != null && !videoId.isNullOrBlank()) {
                SingleVideoScreen(
                    token = token,
                    videoId = videoId,
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.UserProfile.routePattern) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username")?.let { Uri.decode(it) }

            if (token != null && !username.isNullOrBlank()) {
                UserProfileScreen(
                    token = token,
                    username = username,
                    onBack = { navController.popBackStack() },
                    onVideoClick = { video ->
                        navController.navigate(
                            Screen.UserProfileFeed.createRoute(username, video.id)
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }

        composable(Screen.UserProfileFeed.routePattern) { backStackEntry ->
            val profileUsername = backStackEntry.arguments?.getString("username")?.let { Uri.decode(it) }
            val videoId = backStackEntry.arguments?.getString("videoId")

            if (token != null && !profileUsername.isNullOrBlank() && !videoId.isNullOrBlank()) {
                UserProfileFeedScreen(
                    token = token,
                    username = profileUsername,
                    initialVideoId = videoId,
                    navController = navController,
                    onBackToMainFeed = {
                        navController.navigate(Screen.Feed.route) {
                            popUpTo(Screen.Feed.route) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}


