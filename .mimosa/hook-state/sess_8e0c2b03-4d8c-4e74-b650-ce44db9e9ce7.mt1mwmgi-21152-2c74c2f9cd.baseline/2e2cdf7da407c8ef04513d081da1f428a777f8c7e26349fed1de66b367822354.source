package com.client.xvideos.common.videoplayer.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver {
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> handleOnResume(
                exoPlayer,
                isPause,
                wasAppInBackground,
                setWasAppInBackground
            )

            Lifecycle.Event.ON_PAUSE -> handleOnPause(exoPlayer, setWasAppInBackground)
            Lifecycle.Event.ON_STOP -> handleOnStop(exoPlayer, setWasAppInBackground)
            // P3: release() сюда НЕ относится — освобождением владеет создатель плеера
            // (DisposableEffect в rememberExoPlayerWithLifecycle). Иначе двойной владелец.
            else -> { /* No-op */
            }
        }
    }
}

private fun handleOnResume(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
) {
    if (wasAppInBackground) {
        exoPlayer.playWhenReady = !isPause
    }
    setWasAppInBackground(false)
}

private fun handleOnPause(
    exoPlayer: ExoPlayer,
    setWasAppInBackground: (Boolean) -> Unit
) {
    exoPlayer.playWhenReady = false
    setWasAppInBackground(true)
}

private fun handleOnStop(
    exoPlayer: ExoPlayer,
    setWasAppInBackground: (Boolean) -> Unit
) {
    exoPlayer.playWhenReady = false
    setWasAppInBackground(true)
}
