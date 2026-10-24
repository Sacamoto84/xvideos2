package com.client.xvideos.xlive.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import com.client.xvideos.common.theme.Theme

/**
 * Корневой экран раздела XLive.
 *
 * Пока пустой: основа раздела, которую наполняют следующими шагами.
 */
class ScreenXLive : Screen {

    override val key: ScreenKey = "ScreenXLive"

    @Composable
    override fun Content() {
        ScreenXLiveContent()
    }
}

@Composable
fun ScreenXLiveContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Theme.backgroundAppRoot),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "XLive", color = Color.White)
    }
}

@Preview(device = "id:pixel_9_pro")
@Composable
private fun ScreenXLivePreview() {
    ScreenXLiveContent()
}
