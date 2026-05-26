package com.example.vibeclip_frontend.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorContent(
    message: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.error,
    showRetry: Boolean = false,
    onRetry: (() -> Unit)? = null,
    retryLabel: String = "Повторить"
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        if (showRetry && onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth(0.7f)) {
                Text(retryLabel)
            }
        }
    }
}
