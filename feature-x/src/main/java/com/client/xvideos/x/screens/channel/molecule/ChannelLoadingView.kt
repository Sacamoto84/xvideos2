package com.client.xvideos.x.screens.channel.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview

/**
 * Первая загрузка профиля, пока не известно даже имя канала.
 */
@Composable
fun ChannelLoadingView(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF040404)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color(0xFFDE2600))
    }
}

@Preview
@Composable
private fun ChannelLoadingViewPreview() {
    ChannelLoadingView()
}
