package com.client.xvideos.r.ui.fullscreen.atom

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.feature.r.R

@Composable
fun PlayPauseButton(
    play: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(if (play) R.drawable.select_1 else R.drawable.rg_button),
            contentDescription = if (play) "Пауза" else "Воспроизведение",
            tint = Color.White,
            modifier = if (play) Modifier.rotate(90f) else Modifier
        )
    }
}

@Preview
@Composable
private fun PlayPauseButtonPreview() {
    PlayPauseButton(
        play = true,
        onClick = {}
    )
}
