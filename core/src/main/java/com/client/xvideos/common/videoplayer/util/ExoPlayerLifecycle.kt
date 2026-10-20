package com.client.xvideos.common.videoplayer.util

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer

/**
 * Создает наблюдатель жизненного цикла для управления паузой/возобновлением ExoPlayer.
 * Внутреннее состояние [wasAppInBackground] инкапсулировано внутри экземпляра наблюдателя,
 * что исключает лишние рекомпозиции и переподписки в Compose при смене фона.
 *
 * [isAppLocked] читается в момент `ON_RESUME`: вернувшаяся под замок Activity проходит
 * `onResume`, пока её не накрыл экран замка, и без этой проверки плеер успевал
 * запуститься (проход 201, S1).
 */
fun getExoPlayerLifecycleObserver(
    exoPlayer: ExoPlayer,
    isPause: () -> Boolean,
    isAppLocked: () -> Boolean = { false },
): LifecycleEventObserver {
    var wasAppInBackground = false
    return LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (wasAppInBackground) {
                    exoPlayer.playWhenReady = !isPause() && !isAppLocked()
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
