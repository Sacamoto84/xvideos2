package com.client.xvideos.common.videoplayer.ui


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.toPlaybackConfig
import com.client.xvideos.common.videoplayer.util.CMPPlayer2

@Composable
fun StaticPlayer(
    playerHost: MediaPlayerHost,
    autoRotate: Boolean
) {
    CMPPlayer2(
        modifier = Modifier.fillMaxSize(),
        config = playerHost.toPlaybackConfig(autoRotate),
        callbacks = PlayerPlaybackCallbacks(
            totalTime = { playerHost.updateTotalTime(it) },
            currentTime = {
                if (!playerHost.isSliding) {
                    playerHost.updateCurrentTime(it)
                    playerHost.seekToTime = null
                }
            },
            bufferCallback = { playerHost.setBufferingStatus(it) },
            didEndVideo = {
                playerHost.triggerMediaEnd()
                if (!playerHost.isLooping) {
                    playerHost.togglePlayPause()
                }
            },
            error = { playerHost.triggerError(it) },
            poster = { playerHost.poster = it }
        )
    )
}
