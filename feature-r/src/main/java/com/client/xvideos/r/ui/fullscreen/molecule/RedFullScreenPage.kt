package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.model.RedFullScreenPlayerState
import com.client.xvideos.r.ui.fullscreen.redVideoUrl
import com.client.xvideos.r.ui.video.RedPooledVideoPlayer

@Composable
fun RedFullScreenPage(
    item: GifsInfo,
    player: RedFullScreenPlayerState,
    feedState: FeedPlayerState,
    downloadedKeys: Set<String>,
    index: Int,
    bottomPadding: Dp,
    play: Boolean,
    isCurrentPage: Boolean,
    showOverlay: Boolean,
    onBuffering: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    resetZoomTrigger: Int = 0,
    overlay: @Composable () -> Unit = {},
) {
    val videoUri = remember(item.id, item.userName, downloadedKeys) { redVideoUrl(item, downloadedKeys) }

    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = index,
            url = videoUri,
            modifier = Modifier.padding(bottom = bottomPadding),
            play = play,
            isMute = player.mute,
            isCurrentPage = isCurrentPage,
            autoRotate = player.autoRotate,
            timeA = player.timeA,
            timeB = player.timeB,
            enableAB = player.enableAB,
            speed = player.speed,
            onTimeChanged = { position, duration ->
                if (isCurrentPage) {
                    player.currentPlayerTime = position
                    player.currentPlayerDuration = duration
                }
            },
            onPlayerControlsReady = { controls ->
                if (isCurrentPage) {
                    player.currentPlayerControls = controls
                }
            },
            onPlayerControlsRelease = { controls ->
                // Сравнение по ссылке: страница отзывает только свои controls и не
                // затирает те, что успела выставить пришедшая ей на смену.
                if (player.currentPlayerControls === controls) player.currentPlayerControls = null
            },
            onClick = { if (isCurrentPage) player.togglePlay() },
            onBufferingChanged = { buffering ->
                if (isCurrentPage) {
                    onBuffering(buffering)
                }
            },
            onZoomChanged = onZoomChanged,
            resetZoomTrigger = resetZoomTrigger,
        )

        if (showOverlay) {
            overlay()
        }
    }
}

@Preview
@Composable
private fun RedFullScreenPagePreview() {
    val feedState = rememberFeedPlayerState(poolCapacity = 1)
    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = 0,
            url = "",
            play = false,
            isMute = true,
            isCurrentPage = true,
            autoRotate = false,
            timeA = 0f,
            timeB = 0f,
            enableAB = false,
            speed = com.client.xvideos.common.videoplayer.model.PlayerSpeed.DEFAULT,
            onTimeChanged = { _, _ -> },
            onPlayerControlsReady = {},
            onPlayerControlsRelease = {},
            onClick = {},
            onBufferingChanged = {},
            onZoomChanged = {},
            resetZoomTrigger = 0
        )
    }
}
