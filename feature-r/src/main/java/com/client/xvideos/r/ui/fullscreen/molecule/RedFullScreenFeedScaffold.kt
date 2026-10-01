package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackActions
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackState

@Composable
fun RedFullScreenFeedScaffold(
    vm: ScreenRedFullScreenSM,
    isVideoBuffering: Boolean,
    content: @Composable (bottomPadding: Dp) -> Unit
) {
    val percentDownload by vm.downloadRed.downloader.percent.collectAsStateWithLifecycle()

    RedFullScreenScaffold(
        currentTime = vm.currentPlayerTime,
        duration = vm.currentPlayerDuration,
        timeA = vm.timeA,
        timeB = vm.timeB,
        timeABEnable = vm.enableAB,
        play = vm.play,
        onSeek = { vm.currentPlayerControls?.seekTo(it) },
        isVideoBuffering = isVideoBuffering,
        percentDownload = percentDownload,
        controlsContent = {
            val actions = remember(vm) {
                FeedPlaybackActions(
                    onSetTimeA = vm::setTimeA,
                    onSetTimeB = vm::setTimeB,
                    onToggleAB = vm::toggleAB,
                    onTogglePlay = vm::togglePlay,
                    onRewind = { vm.rewind() },
                    onForward = { vm.forward() },
                    onToggleMute = vm::toggleMute,
                    onSpeedChange = { vm.speed = it },
                )
            }
            FeedControls_Container_Line0(
                state = FeedPlaybackState(
                    timeA = vm.timeA,
                    timeB = vm.timeB,
                    enableAB = vm.enableAB,
                    play = vm.play,
                    mute = vm.mute,
                    speed = vm.speed,
                ),
                actions = actions,
            )
        },
        content = content
    )
}

@Preview
@Composable
private fun RedFullScreenFeedScaffoldPreview() {
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
