package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vibeclip_frontend.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(top = 8.dp),
                title = { Text("О разработчиках") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Gray.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Текст о разработчиках
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Приложение VibeClip разработано начинающими разработчиками-студентами\nТГПУ им. Л. Н. Толстого 3 курса ИПИТ\nгрупп 1520531 - Д. И. Минка и 1521731 - А. А. Астахова.\n\nБэкенд-разработчик, проектировщик и разработчик баз данных:\nД. И. Минка\n\nФронтенд-разработчик, дизайнер и проектировщик пользовательского интерфейса (UI/UX):\nА. А. Астахова",
                        fontSize = 18.sp,
                        textAlign = TextAlign.Start,
                        lineHeight = 30.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Логотип и кнопка назад
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Логотип
                    Image(
                        painter = painterResource(R.drawable.vc_logo),
                        contentDescription = "VibeClip Logo",
                        modifier = Modifier.size(120.dp)
                    )
                    
                    // Кнопка назад
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(0.6f)
                    ) {
                        Text("Назад")
                    }
                }
            }
        }
    }
}

