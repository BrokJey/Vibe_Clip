package com.example.vibeclip_frontend.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.vibeclip_frontend.R

@Composable
fun AppLogo(
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    Image(
        painter = painterResource(R.drawable.vc_logo),
        contentDescription = "VibeClip",
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit
    )
}
