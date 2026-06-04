package com.client.xvideos.common.videoplayer.util

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.model.PlayerSpeed

import java.util.concurrent.TimeUnit

private fun PlayerSpeed.toFloat(): Float {
    return when (this) {
        PlayerSpeed.X0_5 -> 0.5f
        PlayerSpeed.X1 -> 1f
        PlayerSpeed.X1_5 -> 1.5f
        PlayerSpeed.X2 -> 2f
    }
}

fun createPlayerListener(
    totalTime: (Int) -> Unit,
    currentTime: (Int) -> Unit,
    loadingState: (Boolean) -> Unit,
    didEndAudio: () -> Unit,
    isSliding: Boolean,
    exoPlayer: ExoPlayer,
    error: (MediaPlayerError) -> Unit
): Player.Listener {

    return object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            super.onEvents(player, events)
            if (!isSliding) {
                totalTime(
                    TimeUnit.MILLISECONDS.toSeconds(player.duration).coerceAtLeast(0L).toInt()
                )
                currentTime(
                    TimeUnit.MILLISECONDS.toSeconds(player.currentPosition).coerceAtLeast(0L)
                        .toInt()
                )
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> loadingState(true)
                Player.STATE_READY -> loadingState(false)
                Player.STATE_ENDED -> {
                    exoPlayer.seekTo(0)
                    didEndAudio()
                }

                Player.STATE_IDLE -> { /* No-op */
                }
            }
        }
        override fun onPlayerError(error: PlaybackException) {
            error(MediaPlayerError.PlaybackError(error.message ?: "Unknown playback error"))
        }
    }
}
