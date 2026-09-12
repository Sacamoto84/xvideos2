package com.client.xvideos.common.videoplayer.model

import androidx.compose.runtime.Immutable
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.util.VideoQuality

/**
 * Конфигурация воспроизведения для [com.client.xvideos.common.videoplayer.util.CMPPlayer2].
 *
 * Группирует входные параметры состояния плеера, устраняя проблему LongParameterList в detekt.
 */
@Immutable
data class PlayerPlaybackConfig(
    val url: String,
    val isPause: Boolean = false,
    val isSliding: Boolean = false,
    val seekToTime: Float? = null,
    val speed: PlayerSpeed = PlayerSpeed.X1,
    val size: ScreenResize = ScreenResize.FIT,
    val loop: Boolean = false,
    val volume: Float = 1f,
    val isLiveStream: Boolean = false,
    val headers: Map<String, String>? = null,
    val drmConfig: DrmConfig? = null,
    val selectedQuality: VideoQuality? = null,
    val autoRotate: Boolean = false
)

/**
 * Колбэки событий воспроизведения для [com.client.xvideos.common.videoplayer.util.CMPPlayer2].
 */
@Immutable
data class PlayerPlaybackCallbacks(
    val totalTime: (Int) -> Unit = {},
    val currentTime: (Float) -> Unit = {},
    val bufferCallback: (Boolean) -> Unit = {},
    val didEndVideo: () -> Unit = {},
    val error: (MediaPlayerError) -> Unit = {},
    val poster: (Boolean) -> Unit = {}
)

/**
 * Конвертирует состояние [MediaPlayerHost] в неизменяемый [PlayerPlaybackConfig].
 */
fun MediaPlayerHost.toPlaybackConfig(autoRotate: Boolean = false): PlayerPlaybackConfig =
    PlayerPlaybackConfig(
        url = url,
        isPause = isPaused,
        isSliding = isSliding,
        seekToTime = seekToTime,
        speed = speed,
        size = videoFitMode,
        loop = isLooping,
        volume = volumeLevel,
        isLiveStream = false,
        headers = headers,
        drmConfig = drmConfig,
        selectedQuality = selectedQuality,
        autoRotate = autoRotate
    )
