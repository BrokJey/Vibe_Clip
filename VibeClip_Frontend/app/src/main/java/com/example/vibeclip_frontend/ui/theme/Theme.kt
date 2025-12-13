package com.example.vibeclip_frontend.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Тёмно-фиолетовая цветовая схема
private val DarkPurpleColorScheme = darkColorScheme(
    primary = MediumPurple, // Основной фиолетовый для акцентов
    onPrimary = White, // Белый текст на фиолетовом
    secondary = LightPurple, // Светло-фиолетовый для вторичных элементов
    onSecondary = White,
    tertiary = DarkPurple, // Тёмно-фиолетовый для третичных элементов
    onTertiary = White,
    background = Black, // Чёрный фон
    onBackground = White, // Белый текст на чёрном
    surface = DarkGray, // Тёмно-серый для поверхностей (карточки, панели)
    onSurface = White, // Белый текст на поверхностях
    surfaceVariant = DarkGray.copy(alpha = 0.8f), // Вариант поверхности
    onSurfaceVariant = White.copy(alpha = 0.8f),
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = White,
    outline = MediumPurple.copy(alpha = 0.5f) // Фиолетовая обводка
)

// Светлая тема (тоже тёмная для единообразия)
private val LightColorScheme = darkColorScheme(
    primary = MediumPurple,
    onPrimary = White,
    secondary = LightPurple,
    onSecondary = White,
    tertiary = DarkPurple,
    onTertiary = White,
    background = Black,
    onBackground = White,
    surface = DarkGray,
    onSurface = White,
    surfaceVariant = DarkGray.copy(alpha = 0.8f),
    onSurfaceVariant = White.copy(alpha = 0.8f),
    error = androidx.compose.ui.graphics.Color(0xFFCF6679),
    onError = White,
    outline = MediumPurple.copy(alpha = 0.5f)
)

@Composable
fun VibeClip_FrontendTheme(
    darkTheme: Boolean = true, // Всегда тёмная тема
    // Dynamic color отключен, используем кастомную тему
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Отключаем dynamic color, используем только нашу кастомную тему
        darkTheme -> DarkPurpleColorScheme
        else -> LightColorScheme // Даже светлая тема будет тёмной
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}