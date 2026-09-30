package com.client.xvideos.l.ui.screens.screenFullScreen.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun FullScreenTopControls(
    visible: Boolean,
    verticalPager: Boolean,
    videoMuted: Boolean,
    onRotateToggle: () -> Unit,
    onVerticalPagerToggle: () -> Unit,
    onVideoMutedToggle: () -> Unit,
    onShowInfoDialog: () -> Unit,
    modifier: Modifier = Modifier,
    expandMenuContent: @Composable () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                IconButton(onClick = onRotateToggle) {
                    Icon(
                        Icons.Default.ScreenRotation,
                        contentDescription = "Повернуть изображение",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onVerticalPagerToggle) {
                    Icon(
                        if (verticalPager) Icons.Default.SwapVert else Icons.Default.SwapHoriz,
                        contentDescription = if (verticalPager) "Листать по горизонтали" else "Листать по вертикали",
                        tint = Color.White
                    )
                }
                IconButton(onClick = onVideoMutedToggle) {
                    Icon(
                        if (videoMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (videoMuted) "Включить звук" else "Выключить звук",
                        tint = Color.White
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onShowInfoDialog) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Информация о картинке",
                        tint = Color.White
                    )
                }
                expandMenuContent()
            }
        }
    }
}

@Preview
@Composable
private fun FullScreenTopControlsPreview() {
    FullScreenTopControls(
        visible = true,
        verticalPager = false,
        videoMuted = true,
        onRotateToggle = {},
        onVerticalPagerToggle = {},
        onVideoMutedToggle = {},
        onShowInfoDialog = {}
    )
}
