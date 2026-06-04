package com.redgifs.common.video.player_with_menu.atom


import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.util.CMPPlayer2

@Composable
fun StaticPlayer(
    playerHost: MediaPlayerHost,
    autoRotate: Boolean
){
    // Video player component
    CMPPlayer2(
        modifier = Modifier.fillMaxSize(),
        url = playerHost.url,
        isPause = playerHost.isPaused,
        totalTime = { playerHost.updateTotalTime(it) }, // Update total time of the video
        currentTime = {
            if (playerHost.isSliding.not()) {
                playerHost.updateCurrentTime(it)  // Update current playback time
                playerHost.seekToTime = null // Reset slider time if not sliding
            }
        },
        isSliding = playerHost.isSliding, // Pass seek bar sliding state
        seekToTime = playerHost.seekToTime, // Pass seek bar slider time
        speed = playerHost.speed, // Pass selected playback speed
        size = playerHost.videoFitMode,
        bufferCallback = { playerHost.setBufferingStatus(it) },
        didEndVideo = {
            playerHost.triggerMediaEnd()
            if (!playerHost.isLooping) {
                playerHost.togglePlayPause()
            }
        },
        loop = playerHost.isLooping,
        volume = playerHost.volumeLevel,
        isLiveStream = false,
        error = { playerHost.triggerError(it) },
        headers = playerHost.headers,
        drmConfig = playerHost.drmConfig,
        selectedQuality = playerHost.selectedQuality,
        autoRotate = autoRotate,
        poster = {
            playerHost.poster = it
        }
    )
}
