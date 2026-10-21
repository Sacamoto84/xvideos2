package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoSizeSelectLarge
import androidx.compose.material.icons.filled.PhotoSizeSelectSmall
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.videoplayer.model.XSeekPreviewMode

/**
 * ВРЕМЕННО: кнопка для теста вида превью при перемотке — меняет A ↔ B прямо на
 * экране плеера. После того как владелец выберет вид, удалить; выбор остаётся в
 * «Настройках» (спека 2026-10-21-x-seek-preview).
 */
@Composable
fun SeekPreviewModeToggle(
    mode: XSeekPreviewMode,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Icon(
        imageVector = if (mode == XSeekPreviewMode.FOLLOW) {
            Icons.Filled.PhotoSizeSelectSmall
        } else {
            Icons.Filled.PhotoSizeSelectLarge
        },
        contentDescription = mode.label,
        tint = Color.White,
        modifier = modifier
            .size(28.dp)
            .clickable(onClick = onToggle)
    )
}

@Preview
@Composable
private fun SeekPreviewModeTogglePreview() {
    SeekPreviewModeToggle(
        mode = XSeekPreviewMode.FOLLOW,
        onToggle = {}
    )
}
