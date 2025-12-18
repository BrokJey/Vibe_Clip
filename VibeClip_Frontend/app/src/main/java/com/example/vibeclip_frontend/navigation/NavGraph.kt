package com.example.vibeclip_frontend.navigation

import androidx.compose.runtime.Composable
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
                        // При клике на видео переходим к ленте с конкретным видео
                        navController.navigate(Screen.FeedWithVideo(video.id).route) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}


