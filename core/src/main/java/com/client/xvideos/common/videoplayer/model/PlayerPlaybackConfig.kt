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
    val url: String = "",
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
    val autoRotate: Boolean = false,
    val playFromTime: Float? = null,
) {
    val isValid: Boolean get() = url.isNotBlank()
    val isEmpty: Boolean get() = url.isEmpty()
    val isNotEmpty: Boolean get() = url.isNotEmpty()
    val isMuted: Boolean get() = volume <= 0f
    val isAudioEnabled: Boolean get() = !isMuted
    val isPlaying: Boolean get() = isNotEmpty && !isPause
    val hasHeaders: Boolean get() = !headers.isNullOrEmpty()
    val hasDrm: Boolean get() = drmConfig != null
    val isSeeking: Boolean get() = isSliding || seekToTime != null

    fun withVolume(newVolume: Float): PlayerPlaybackConfig =
        copy(volume = newVolume.coerceIn(0f, 1f))

    fun withSpeed(newSpeed: PlayerSpeed): PlayerPlaybackConfig =
        copy(speed = newSpeed)

    fun withSeek(time: Float?): PlayerPlaybackConfig =
        copy(seekToTime = time?.coerceAtLeast(0f))

    fun withPlayFromTime(time: Float?): PlayerPlaybackConfig =
        copy(playFromTime = time?.coerceAtLeast(0f))

    fun toggledPlayPause(): PlayerPlaybackConfig =
        copy(isPause = !isPause)

    fun toggledLoop(): PlayerPlaybackConfig =
        copy(loop = !loop)

    fun toggledScreenResize(): PlayerPlaybackConfig =
        copy(size = size.toggle())

    fun sanitized(): PlayerPlaybackConfig = copy(
        volume = volume.coerceIn(0f, 1f),
        seekToTime = seekToTime?.coerceAtLeast(0f),
        playFromTime = playFromTime?.coerceAtLeast(0f)
    )

    companion object {
        val EMPTY = PlayerPlaybackConfig()
    }
}

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
) {
    companion object {
        val EMPTY = PlayerPlaybackCallbacks()
    }
}

/**
 * Конвертирует состояние [MediaPlayerHost] в неизменяемый [PlayerPlaybackConfig].
 */
fun MediaPlayerHost.toPlaybackConfig(autoRotate: Boolean = false): PlayerPlaybackConfig =
    PlayerPlaybackConfig(
        url = url,
        isPause = isPaused,
        isSliding = isSliding,
        seekToTime = seekToTime,
        playFromTime = playFromTime,
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
