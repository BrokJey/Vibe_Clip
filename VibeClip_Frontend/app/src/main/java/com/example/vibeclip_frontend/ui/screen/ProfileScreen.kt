package com.example.vibeclip_frontend.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vibeclip_frontend.data.repository.UserRepository
import com.example.vibeclip_frontend.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    token: String,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = viewModel { ProfileViewModel(UserRepository(), token) }
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = uiState.user

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Профиль") },
                actions = {
                    TextButton(onClick = onLogout) { Text("Выход") }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(uiState.errorMessage!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { viewModel.load() }) { Text("Повторить") }
                }
            }
            user != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Email: ${user.email}", style = MaterialTheme.typography.titleMedium)
                    Text("Username: ${user.username}", style = MaterialTheme.typography.titleMedium)
                    Text("ID: ${user.id}", style = MaterialTheme.typography.bodySmall)
                    Text("Создан: ${user.createdAt}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

