package com.client.xvideos.common.videoplayer.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer

/**
 * Создает наблюдатель жизненного цикла для управления паузой/возобновлением ExoPlayer.
 * Внутреннее состояние [wasAppInBackground] инкапсулировано внутри экземпляра наблюдателя,
 * что исключает лишние рекомпозиции и переподписки в Compose при смене фона.
 */
fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: () -> Boolean,
): LifecycleEventObserver {
    var wasAppInBackground = false
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (wasAppInBackground) {
                    exoPlayer.playWhenReady = !isPause()
                }
                wasAppInBackground = false
            }
            Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_STOP -> {
                exoPlayer.playWhenReady = false
                wasAppInBackground = true
            }
            else -> { /* No-op */ }
        }
    }
}

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: () -> Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver {
    var inBg = wasAppInBackground
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (inBg) {
                    exoPlayer.playWhenReady = !isPause()
                }
                inBg = false
                setWasAppInBackground(false)
            }

            Lifecycle.Event.ON_PAUSE,
            Lifecycle.Event.ON_STOP -> {
                exoPlayer.playWhenReady = false
                inBg = true
                setWasAppInBackground(true)
            }
            else -> { /* No-op */ }
        }
    }
}

fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: Boolean,
    wasAppInBackground: Boolean,
    setWasAppInBackground: (Boolean) -> Unit
): LifecycleEventObserver = getExoPlayerLifecycleObserver(
    exoPlayer = exoPlayer,
    isPause = { isPause },
    wasAppInBackground = wasAppInBackground,
    setWasAppInBackground = setWasAppInBackground
)
