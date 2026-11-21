package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.ui.HintTooltip

@Composable
fun MuteButton(
    mute: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hint = if (mute) "Включить звук" else "Выключить звук"
    HintTooltip(text = hint, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val icon = if (mute) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp
            Icon(
                icon,
                contentDescription = hint,
                tint = if (mute) Color.Gray else Color.White
            )
        }
    }
}

@Preview
@Composable
private fun MuteButtonPreview() {
    MuteButton(
        mute = false,
        onClick = {}
    )
}
