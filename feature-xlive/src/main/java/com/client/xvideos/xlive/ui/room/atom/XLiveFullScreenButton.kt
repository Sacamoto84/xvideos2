package com.client.xvideos.xlive.ui.room.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

/**
 * Кнопка полного экрана поверх живого видео.
 *
 * @param isFullScreen открыто ли видео на весь экран.
 * @param onClick переключить полноэкранный режим.
 */
@Composable
fun XLiveFullScreenButton(
    isFullScreen: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = if (isFullScreen) "Выйти из полного экрана" else "На весь экран"
    HintTooltip(text = hint, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0x99000000))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = hint,
                tint = Color.White,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}

@Preview
@Composable
private fun XLiveFullScreenButtonPreview() {
    XLiveFullScreenButton(isFullScreen = false, onClick = {})
}
