package com.client.xvideos.common.videoplayer.local.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

/**
 * Кнопка звука в нижней панели плеера скачанного файла. Что она сделает,
 * говорит подсказка по долгому нажатию.
 *
 * @param muted звук выключен.
 */
@Composable
fun PlayerMuteButton(
    muted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = muteButtonHint(muted)
    HintTooltip(text = hint, modifier = modifier) {
        Icon(
            imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = hint,
            tint = Color.White,
            modifier = Modifier
                .size(26.dp)
                .clickable(onClick = onClick)
        )
    }
}

/** Подсказка кнопки звука: что кнопка сделает. */
fun muteButtonHint(muted: Boolean): String = if (muted) "Включить звук" else "Выключить звук"

@Preview
@Composable
private fun PlayerMuteButtonPreview() {
    PlayerMuteButton(muted = true, onClick = {})
}
