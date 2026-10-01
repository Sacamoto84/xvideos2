package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.ui.atom.DownloadIndicator
import com.client.xvideos.r.ui.video.CanvasTimeDurationLine1

@Composable
fun RedFullScreenScaffold(
    currentTime: Float,
    duration: Int,
    timeA: Float,
    timeB: Float,
    timeABEnable: Boolean,
    play: Boolean,
    onSeek: (Float) -> Unit,
    isVideoBuffering: Boolean,
    percentDownload: Float,
    controlsContent: @Composable () -> Unit,
    content: @Composable (bottomPadding: Dp) -> Unit
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.background(Theme.R.colorCommonBackground)) {
                Box(
                    Modifier
                        .padding(bottom = 1.dp)
                        .clip(RoundedCornerShape(0))
                        .height(32.dp)
                        .fillMaxWidth()
                        .background(Theme.tabLevel0),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    CanvasTimeDurationLine1(
                        currentTime = currentTime,
                        duration = duration,
                        timeA = timeA,
                        timeB = timeB,
                        timeABEnable = timeABEnable,
                        play = play,
                        onSeek = onSeek,
                        onSeekFinished = {},
                        modifier = Modifier.padding(horizontal = 0.dp),
                        isBuffering = isVideoBuffering
                    )
                }

                Box(modifier = Modifier.background(Theme.tabLevel1)) {
                    controlsContent()
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        DownloadIndicator(percentDownload)
                    }
                }
            }
        }
    ) { padding ->
        content(padding.calculateBottomPadding() / 2)
    }
}

@Preview
@Composable
private fun RedFullScreenScaffoldPreview() {
    RedFullScreenScaffold(
        currentTime = 10f,
        duration = 60,
        timeA = 0f,
        timeB = 60f,
        timeABEnable = false,
        play = true,
        onSeek = {},
        isVideoBuffering = false,
        percentDownload = 0f,
        controlsContent = {},
        content = {}
    )
}
