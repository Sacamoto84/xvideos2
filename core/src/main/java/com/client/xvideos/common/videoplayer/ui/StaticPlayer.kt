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
            currentTime = { playerHost.onPlayerPosition(it) },
            seekApplied = { playerHost.onSeekApplied(it) },
            bufferCallback = { playerHost.setBufferingStatus(it) },
            didEndVideo = { playerHost.onMediaEnded() },
            error = { playerHost.triggerError(it) },
            poster = { playerHost.poster = it }
        )
    )
}
