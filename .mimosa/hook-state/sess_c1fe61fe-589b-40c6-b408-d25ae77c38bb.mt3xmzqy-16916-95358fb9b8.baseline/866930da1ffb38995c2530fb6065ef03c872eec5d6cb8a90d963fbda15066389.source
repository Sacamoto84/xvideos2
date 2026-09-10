package com.client.xvideos.common.videoplayer.util

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import java.util.concurrent.TimeUnit

internal fun createPlayerListener(
    isSliding: Boolean,
    totalTime: (Int) -> Unit,
    currentTime: (Float) -> Unit,
    loadingState: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    onError: (MediaPlayerError) -> Unit,
    poster: (Boolean) -> Unit,
    sourceUrl: String? = null
): Player.Listener {

    return object : Player.Listener {

        override fun onRenderedFirstFrame() {
            poster(false)
        }

        //
        override fun onEvents(player: Player, events: Player.Events) {
            if (!isSliding) {
                totalTime(
                    TimeUnit.MILLISECONDS.toSeconds(player.duration).coerceAtLeast(0L).toInt()
                )
                currentTime( TimeUnit.MILLISECONDS.toSeconds(player.currentPosition).coerceAtLeast(0L).toFloat() )
            }
        }

        //
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    loadingState(true)
                }

                Player.STATE_READY -> {
                    loadingState(false)
                    //stateReady(true)
                }

                Player.STATE_ENDED -> {
                    loadingState(false)
                    didEndVideo()
                }

                Player.STATE_IDLE -> {
                    loadingState(false)
                }
            }
        }

        //
        override fun onPlayerError(playbackException: PlaybackException) {
            val message = playbackException.message ?: "Unknown playback error"
            onError(MediaPlayerError.PlaybackError(message))
        }

    }
}
