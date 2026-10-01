package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.downloader.DownloadRed
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackActions
import com.client.xvideos.r.ui.fullscreen.bottom_bar.FeedPlaybackState
import com.client.xvideos.r.ui.fullscreen.model.RedFullScreenPlayerState

@Composable
fun RedFullScreenFeedScaffold(
    player: RedFullScreenPlayerState,
    downloadRed: DownloadRed,
    isVideoBuffering: Boolean,
    content: @Composable (bottomPadding: Dp) -> Unit
) {
    val percentDownload by downloadRed.downloader.percent.collectAsStateWithLifecycle()

    RedFullScreenScaffold(
        currentTime = player.currentPlayerTime,
        duration = player.currentPlayerDuration,
        timeA = player.timeA,
        timeB = player.timeB,
        timeABEnable = player.enableAB,
        play = player.play,
        onSeek = { player.currentPlayerControls?.seekTo(it) },
        isVideoBuffering = isVideoBuffering,
        percentDownload = percentDownload,
        controlsContent = {
            val actions = remember(player) {
                FeedPlaybackActions(
                    onSetTimeA = player::setTimeA,
                    onSetTimeB = player::setTimeB,
                    onToggleAB = player::toggleAB,
                    onTogglePlay = player::togglePlay,
                    onRewind = { player.rewind() },
                    onForward = { player.forward() },
                    onToggleMute = player::toggleMute,
                    onSpeedChange = { player.speed = it },
                )
            }
            FeedControls_Container_Line0(
                state = FeedPlaybackState(
                    timeA = player.timeA,
                    timeB = player.timeB,
                    enableAB = player.enableAB,
                    play = player.play,
                    mute = player.mute,
                    speed = player.speed,
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
