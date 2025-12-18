package com.example.vibeclip_frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import com.example.vibeclip_frontend.di.AppModule
import com.example.vibeclip_frontend.navigation.NavGraph
import com.example.vibeclip_frontend.navigation.Screen
import com.example.vibeclip_frontend.ui.theme.VibeClip_FrontendTheme

class MainActivity : ComponentActivity() {
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                enableEdgeToEdge()
        
                AppModule.initialize(this)
                val tokenManager = AppModule.tokenManager
        
                setContent {
                        VibeClip_FrontendTheme {
                                var token by remember { mutableStateOf(tokenManager.getToken()) }
                                val navController = rememberNavController()
                
                                Surface(
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.background
                                            ) {
                                        val bottomItems = listOf(
                                                Screen.Feed,
                                                Screen.Upload,
                                                Screen.Folders,
                                                Screen.Profile
                                                    )
                    
                                        Scaffold(
                                                bottomBar = {
                                                        if (token != null) {
                                                                NavigationBar(
                                                                        containerColor = Color.Black,
                                                                        contentColor = Color.White
                                                                ) {
                                                                        val currentRoute = navController.currentDestination?.route
                                                                        bottomItems.forEach { screen ->
                                                                                val isSelected = currentRoute == screen.route
                                                                                NavigationBarItem(
                                                                                        selected = isSelected,
                                                                                        onClick = {
                                                                                                navController.navigate(screen.route) {
                                                                                                        popUpTo(Screen.Feed.route) { inclusive = false }
                                                                                                        launchSingleTop = true
                                                                                                    }
                                                                                            },
                                                                                        label = { 
                                                                                                Text(
                                                                                                        text = screenLabel(screen),
                                                                                                        color = if (isSelected) Color(0xFF9C88FF) else Color.White.copy(alpha = 0.6f)
                                                                                                ) 
                                                                                        },
                                                                                        icon = {
                                                                                                NavigationIcon(
                                                                                                        screen = screen,
                                                                                                        isSelected = isSelected
                                                                                                )
                                                                                            },
                                                                                        colors = NavigationBarItemDefaults.colors(
                                                                                                selectedIconColor = Color(0xFF9C88FF),
                                                                                                selectedTextColor = Color(0xFF9C88FF),
                                                                                                indicatorColor = Color(0xFF9C88FF).copy(alpha = 0.3f),
                                                                                                unselectedIconColor = Color.White.copy(alpha = 0.6f),
                                                                                                unselectedTextColor = Color.White.copy(alpha = 0.6f)
                                                                                        )
                                                                                            )
                                                                                                            }
                                                                    }
                                                            }
                                                    }
                                                    ) { paddingValues ->
                                                Box(modifier = Modifier.padding(paddingValues)) {
                                                        NavGraph(
                                                                navController = navController,
                                                                token = token,
                                                                onTokenUpdated = { token = it },
                                                                onLogout = {
                                                                        tokenManager.clearToken()
                                                                        token = null
                                                                        navController.navigate(Screen.Login.route) {
                                                                                popUpTo(Screen.Feed.route) { inclusive = true }
                                                                            }
                                                                    }
                                                                )
                                                    }
                                            }
                                    }
                            }
                    }
            }
    }

@Composable
private fun screenLabel(screen: Screen): String = when (screen) {
        Screen.Feed -> "Лента"
        is Screen.FeedWithVideo -> "Лента"
        Screen.Upload -> "Загрузка"
        Screen.Folders -> "Папки"
        Screen.Profile -> "Профиль"
        Screen.Login, Screen.Register -> screen.route
        is Screen.FolderFeed -> "Папка"
    }

@Composable
private fun NavigationIcon(screen: Screen, isSelected: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Используем иконки из папки images/icons/footer/
    // Файлы скопированы в корень drawable для использования в Android
    val iconResId = when (screen) {
        Screen.Feed -> {
            val resId = context.resources.getIdentifier("home", "drawable", context.packageName)
            if (resId != 0) resId else android.R.drawable.ic_menu_view
        }
        Screen.Upload -> {
            val resId = context.resources.getIdentifier("export", "drawable", context.packageName)
            if (resId != 0) resId else android.R.drawable.ic_menu_upload
        }
        Screen.Folders -> {
            val resId = context.resources.getIdentifier("folders", "drawable", context.packageName)
            if (resId != 0) resId else android.R.drawable.ic_menu_manage
        }
        Screen.Profile -> {
            val resId = context.resources.getIdentifier("profile", "drawable", context.packageName)
            if (resId != 0) resId else android.R.drawable.ic_menu_myplaces
        }
        else -> android.R.drawable.ic_menu_view
    }
    
    // Цвет для активной иконки - фиолетовый, для неактивной - полупрозрачный белый
    val iconColor = if (isSelected) {
        Color(0xFF9C88FF) // Фиолетовый цвет для активной иконки
    } else {
        Color.White.copy(alpha = 0.6f) // Полупрозрачный белый для неактивной иконки
    }
    
    Image(
        painter = painterResource(id = iconResId),
        contentDescription = null,
        modifier = Modifier.size(28.dp), // Размер иконки
        colorFilter = ColorFilter.tint(iconColor)
    )
}
