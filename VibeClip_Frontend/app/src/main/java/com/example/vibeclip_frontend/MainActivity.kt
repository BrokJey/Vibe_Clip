package com.example.vibeclip_frontend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
                                                                NavigationBar {
                                                                        val currentRoute = navController.currentDestination?.route
                                                                        bottomItems.forEach { screen ->
                                                                                NavigationBarItem(
                                                                                        selected = currentRoute == screen.route,
                                                                                        onClick = {
                                                                                                navController.navigate(screen.route) {
                                                                                                        popUpTo(Screen.Feed.route) { inclusive = false }
                                                                                                        launchSingleTop = true
                                                                                                    }
                                                                                            },
                                                                                        label = { Text(screenLabel(screen)) },
                                                                                        icon = { Icon(Icons.Filled.Circle, contentDescription = null) }
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
        Screen.Upload -> "Загрузка"
        Screen.Folders -> "Папки"
        Screen.Profile -> "Профиль"
        Screen.Login, Screen.Register -> screen.route
        is Screen.FolderFeed -> "Папка"
    }
