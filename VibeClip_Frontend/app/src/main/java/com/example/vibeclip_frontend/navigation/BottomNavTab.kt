package com.example.vibeclip_frontend.navigation

/**
 * Вкладки нижней навигации и соответствие им экранов NavGraph.
 */
enum class BottomNavTab(val route: String) {
    Feed(Screen.Feed.route),
    Upload(Screen.Upload.route),
    Folders(Screen.Folders.route),
    Profile(Screen.Profile.route)
}

fun bottomNavTabForRoute(route: String?): BottomNavTab? {
    if (route.isNullOrBlank()) return null
    return when {
        isFeedTabRoute(route) -> BottomNavTab.Feed
        route == Screen.Upload.route -> BottomNavTab.Upload
        isFoldersTabRoute(route) -> BottomNavTab.Folders
        isProfileTabRoute(route) -> BottomNavTab.Profile
        else -> null
    }
}

private fun isFeedTabRoute(route: String): Boolean =
    route == Screen.Feed.route ||
        route == Screen.FeedWithVideo.routePattern ||
        route.startsWith("feed/") ||
        route == Screen.UserProfile.routePattern ||
        route.startsWith("user/")

private fun isFoldersTabRoute(route: String): Boolean =
    route == Screen.Folders.route ||
        route == Screen.FolderFeed.routePattern ||
        route.startsWith("folder_feed/")

private fun isProfileTabRoute(route: String): Boolean =
    route == Screen.Profile.route ||
        route == Screen.OwnVideo.routePattern ||
        route.startsWith("my_video/")
