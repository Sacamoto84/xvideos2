package com.client.xvideos.x.screens.channel.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Плашка под вырез экрана: закрывает уехавшую вверх шапку канала.
 *
 * @param height Высота выреза.
 */
@Composable
fun ChannelStatusCover(
    height: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(Color(0xFF040404))
    )
}

@Preview
@Composable
private fun ChannelStatusCoverPreview() {
    ChannelStatusCover(height = 24.dp)
}
