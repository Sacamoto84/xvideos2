package com.client.xvideos.common.videoplayer.host

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.util.AudioTrack
import com.client.xvideos.common.videoplayer.util.M3U8Helper
import com.client.xvideos.common.videoplayer.util.SubtitleTrack
import com.client.xvideos.common.videoplayer.util.VideoQuality
import com.client.xvideos.common.util.launchCatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext

class MediaPlayerHost(
    mediaUrl: String = "",
    isPaused: Boolean = false,
    isMuted: Boolean = false,
    initialSpeed: PlayerSpeed = PlayerSpeed.X1,
    initialVideoFitMode: ScreenResize = ScreenResize.FILL,
    isLooping: Boolean = true,
    startTimeInSeconds: Float? = null,
    isFullScreen: Boolean = false,
    headers: Map<String, String>? = null,
    drmConfig: DrmConfig? = null,
) : RememberObserver {
    var poster by mutableStateOf(true)

    // Internal states
    var url by mutableStateOf(mediaUrl)
    var speed by mutableStateOf(initialSpeed)
    var videoFitMode by mutableStateOf(initialVideoFitMode)
    var seekToTime: Float? by mutableStateOf(null)
    var isSliding by mutableStateOf(false)
    var isPaused by mutableStateOf(isPaused)
    internal var isMuted by mutableStateOf(isMuted)
    var isLooping by mutableStateOf(isLooping)
    var totalTime by mutableStateOf(0) // Total video duration
    var currentTime by mutableFloatStateOf(0f) // Current playback position
    var isBuffering by mutableStateOf(true)
    internal var playFromTime: Float? by mutableStateOf(startTimeInSeconds)
    var volumeLevel by mutableStateOf(if (isMuted) 0f else 1f) // Range 0.0 to 1.0
    internal var isFullScreen by mutableStateOf(isFullScreen)
    var headers by mutableStateOf(headers)
    var drmConfig by mutableStateOf(drmConfig)
    var qualityOptions by mutableStateOf(emptyList<VideoQuality>())
    var selectedQuality by mutableStateOf<VideoQuality?>(null)
    var audioTrackOptions by mutableStateOf(emptyList<AudioTrack>())
    var selectedAudioTrack by mutableStateOf<AudioTrack?>(null)
    var subTitlesOptions by mutableStateOf(emptyList<SubtitleTrack>())
    var selectedsubTitle by mutableStateOf<SubtitleTrack?>(null)

    private var lastVolumeLevel by mutableStateOf(1f)

    private val m3u8Helper = M3U8Helper()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    var onEvent: ((MediaPlayerEvent) -> Unit)? = null
    var onError: ((MediaPlayerError) -> Unit)? = null

    init {
        // Список качеств — украшение: без него плеер играет дорожку по умолчанию.
        // Отказ сети здесь ронял приложение целиком (UnknownHostException на
        // хосте HLS уходил из launch без обработчика в обработчик потока).
        scope.launchCatching(Dispatchers.IO, "Не удалось разобрать HLS: $url") {
            fetchAndUpdateMediaInfo(url)
        }
    }

    // Public actions
    fun loadUrl(mediaUrl: String, headers: Map<String, String>? = null, drmConfig: DrmConfig? = null) {
        this.headers = headers
        this.drmConfig = drmConfig
        if (url != mediaUrl) {
            url = mediaUrl
            scope.launchCatching(Dispatchers.IO, "Не удалось разобрать HLS: $mediaUrl") {
                fetchAndUpdateMediaInfo(mediaUrl)
            }
        }
    }

    fun play() {
        isPaused = false
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun pause() {
        isPaused = true
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun togglePlayPause() {
        isPaused = !isPaused
        onEvent?.invoke(MediaPlayerEvent.PauseChange(isPaused))
    }

    fun mute() {
        if (!isMuted) {
            lastVolumeLevel = volumeLevel // Store current volume before muting
            volumeLevel = 0f
            isMuted = true
            onEvent?.invoke(MediaPlayerEvent.MuteChange(isMuted))
        }
    }

    fun unmute() {
        if (isMuted) {
            volumeLevel = lastVolumeLevel // Restore previous volume
            isMuted = false
            onEvent?.invoke(MediaPlayerEvent.MuteChange(isMuted))
        }
    }

    fun toggleMuteUnmute() {
        if (isMuted) {
            unmute()
        } else {
            mute()
        }
    }

//    fun setSpeed(speed: PlayerSpeed) {
//        this.speed = speed
//    }

    @Deprecated(
        message = "Use seekTo(seconds: Float?) instead for better precision.",
        replaceWith = ReplaceWith("seekTo(seconds.toFloat())")
    )
    fun seekTo(seconds: Int?) {
        isSliding = true
        seekToTime = seconds?.toFloat()
        isSliding = false
    }

    fun seekTo(seconds: Float?) {
        isSliding = true
        seekToTime = seconds
        isSliding = false
    }

//    fun setVideoFitMode(mode: ScreenResize) {
//        videoFitMode = mode
//    }

//    fun setLooping(isLooping: Boolean) {
//        this.isLooping = isLooping
//    }

    fun toggleLoop() {
        this.isLooping = !this.isLooping
    }

    fun setVolume(level: Float) {
        volumeLevel = level.coerceIn(0f, 1f)
        if (!isMuted) {
            lastVolumeLevel = volumeLevel // Update last volume only if not muted
        }
    }

    fun setFullScreen(isFullScreen: Boolean) {
        this.isFullScreen = isFullScreen
        onEvent?.invoke(MediaPlayerEvent.FullScreenChange(isFullScreen))
    }

    fun toggleFullScreen() {
        this.isFullScreen = !this.isFullScreen
        onEvent?.invoke(MediaPlayerEvent.FullScreenChange(this.isFullScreen))
    }
    fun setVideoQuality(quality: VideoQuality?) {
        this.selectedQuality = quality
    }

    fun updateVideoQualityOptions(options: List<VideoQuality>) {
        this.qualityOptions = options
    }

    fun setAudioTrack(track: AudioTrack?) {
        this.selectedAudioTrack = track
    }

    fun updateAudioTrackOptions(options: List<AudioTrack>) {
        this.audioTrackOptions = options
    }

    fun setSubTitle(subTitle: SubtitleTrack?) {
        this.selectedsubTitle = subTitle
    }

    fun updateSubTitleOptions(options: List<SubtitleTrack>) {
        this.subTitlesOptions = options
    }

    fun setBufferingStatus(isBuffering: Boolean) {
        this.isBuffering = isBuffering
        onEvent?.invoke(MediaPlayerEvent.BufferChange(isBuffering))
    }

    // Internal-only setters for time values
    fun updateTotalTime(time: Int) {
        if (totalTime != time) {
            totalTime = time
            onEvent?.invoke(MediaPlayerEvent.TotalTimeChange(totalTime))
        }
    }

    fun updateCurrentTime(time: Float) {
        if(currentTime != time) {
            currentTime = time
            onEvent?.invoke(MediaPlayerEvent.CurrentTimeChange(currentTime))
        }
    }

    fun triggerMediaEnd() {
        onEvent?.invoke(MediaPlayerEvent.MediaEnd)
    }

    fun triggerError(error: MediaPlayerError) {
        onError?.invoke(error)
    }

    /**
     * P2: освобождает ресурсы хоста — отменяет его [scope], чтобы незавершённые
     * `fetchAndUpdateMediaInfo`/корутины не утекали. Идемпотентно.
     */
    fun dispose() {
        onEvent = null
        onError = null
        scope.cancel()
    }

    // RememberObserver: Compose сам зовёт onForgotten()/onAbandoned() при выходе
    // экземпляра из композиции (в т.ч. при смене ключа remember(url){ ... }).
    override fun onRemembered() { /* no-op */ }
    override fun onForgotten() { dispose() }
    override fun onAbandoned() { dispose() }

    private suspend fun fetchAndUpdateMediaInfo(videoUrl: String) {
        // P5: запись Compose-стейта выполняем только на главном потоке.
        withContext(Dispatchers.Main) {
            setVideoQuality(null)
            setAudioTrack(null)
            setSubTitle(null)
        }
        if (videoUrl.endsWith(".m3u8", ignoreCase = true)) {
            val m3u8Data = m3u8Helper.fetchM3U8Data(videoUrl, headers)

            withContext(Dispatchers.Main) {
                updateVideoQualityOptions(m3u8Data.videoQualities)
                updateAudioTrackOptions(m3u8Data.audioTracks)
                updateSubTitleOptions(m3u8Data.subtitleTracks)
            }
        } else {
            withContext(Dispatchers.Main) {
                updateVideoQualityOptions(emptyList())
                updateAudioTrackOptions(emptyList())
                updateSubTitleOptions(emptyList())
            }
        }
    }
}
