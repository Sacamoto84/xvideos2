package com.client.xvideos.l.ui.screens.screenFullScreen.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.l.model.isLVideoFileUrl

@Composable
fun LFullScreenVideoPoster(
    previewUrl: String,
    albumName: String,
    modifier: Modifier = Modifier
) {
    if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
        UrlImage(
            url = previewUrl,
            contentScale = ContentScale.Fit,
            modifier = modifier,
            albumName = albumName,
            autoPlay = false,
            isAnimated = false
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF202020)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
        }
    }
}

@Preview
@Composable
private fun LFullScreenVideoPosterPreview() {
    LFullScreenVideoPoster(
        previewUrl = "",
        albumName = "Sample Album",
        modifier = Modifier.fillMaxSize()
    )
}
