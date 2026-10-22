package com.client.xvideos.common.videoplayer.host

import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.client.xvideos.common.videoplayer.model.MediaClip
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.model.SeekRequest

/**
 * Состояние плеера, которым управляет экран.
 *
 * Плейлист хост не запрашивает: его качает и разбирает сам плеер. Раньше хост
 * делал второй запрос того же плейлиста ради списков качества и дорожек,
 * которые не читал ни один экран.
 */
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
    /** Отрезки файла вместо файла целиком (превью сохранённого ролика X); пусто — весь файл. */
    val clips: List<MediaClip> = emptyList(),
) : RememberObserver {
    var poster by mutableStateOf(true)

    // Internal states
    var url by mutableStateOf(mediaUrl)
    var speed by mutableStateOf(initialSpeed)
    var videoFitMode by mutableStateOf(initialVideoFitMode)
    /**
     * Перемотка на ходу, пока палец ведёт ползунок (плеер скачанного ролика X).
     * [seekTo] её не пишет: у него свой канал [seekRequest], иначе плеер
     * перематывал бы дважды.
     */
    var seekToTime: Float? by mutableStateOf(null)

    /**
     * Перемотка из [seekTo], которую плеер ещё не выполнил. Отдельно от
     * [seekToTime]: тот сбрасывает опрос позиции, и если опрос успевал до
     * перекомпоновки, перемотка при отпускании ползунка терялась.
     */
    var seekRequest: SeekRequest? by mutableStateOf(null)
        private set
    private var lastSeekRequestId = 0L

    var isSliding by mutableStateOf(false)
    var isPaused by mutableStateOf(isPaused)
    internal var isMuted by mutableStateOf(isMuted)
    var isLooping by mutableStateOf(isLooping)
    var totalTime by mutableIntStateOf(0) // Total video duration
    var currentTime by mutableFloatStateOf(0f) // Current playback position
    var isBuffering by mutableStateOf(true)
    internal var playFromTime: Float? by mutableStateOf(startTimeInSeconds)
    var volumeLevel by mutableFloatStateOf(if (isMuted) 0f else 1f) // Range 0.0 to 1.0
    internal var isFullScreen by mutableStateOf(isFullScreen)
    var headers by mutableStateOf(headers)
    var drmConfig by mutableStateOf(drmConfig)

    private var lastVolumeLevel by mutableFloatStateOf(1f)

    var onEvent: ((MediaPlayerEvent) -> Unit)? = null
    var onError: ((MediaPlayerError) -> Unit)? = null

    // Public actions
    fun loadUrl(mediaUrl: String, headers: Map<String, String>? = null, drmConfig: DrmConfig? = null) {
        this.headers = headers
        this.drmConfig = drmConfig
        url = mediaUrl
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


    fun seekTo(seconds: Float?) {
        val validSeconds = seconds?.takeIf { it.isFinite() && it >= 0f }
        isSliding = true
        validSeconds?.let {
            currentTime = it
            seekRequest = SeekRequest(it, ++lastSeekRequestId)
        }
        isSliding = false
    }

    /**
     * Перемотка на [deltaSeconds] от текущего времени (свайп по видео), в
     * пределах ролика. Считает от [currentTime], который [seekTo] сдвигает
     * сразу, поэтому свайпы подряд складываются, даже пока плеер не выполнил
     * прошлую перемотку. Длительность ещё неизвестна — ничего.
     */
    fun seekBy(deltaSeconds: Float) {
        if (totalTime <= 0) return
        seekTo((currentTime + deltaSeconds).coerceIn(0f, totalTime.toFloat()))
    }

    /**
     * Позиция от плеера (опрос раз в 50 мс). Пока ползунок держат или
     * перемотка ещё не дошла до плеера, позиция старая — её не берём, иначе
     * бегунок прыгал бы назад.
     */
    fun onPlayerPosition(seconds: Float) {
        if (isSliding || seekRequest != null) return
        updateCurrentTime(seconds)
        seekToTime = null
    }

    /** Плеер выполнил перемотку [requestId]; более ранний номер новый запрос не снимает. */
    fun onSeekApplied(requestId: Long) {
        if (seekRequest?.id == requestId) seekRequest = null
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

    fun setBufferingStatus(isBuffering: Boolean) {
        this.isBuffering = isBuffering
        onEvent?.invoke(MediaPlayerEvent.BufferChange(isBuffering))
    }

    // Internal-only setters for time values
    fun updateTotalTime(time: Int) {
        val validTime = if (time >= 0) time else 0
        if (totalTime != validTime) {
            totalTime = validTime
            onEvent?.invoke(MediaPlayerEvent.TotalTimeChange(totalTime))
        }
    }

    fun updateCurrentTime(time: Float) {
        val validTime = if (time.isFinite() && time >= 0f) time else 0f
        if (currentTime != validTime) {
            currentTime = validTime
            onEvent?.invoke(MediaPlayerEvent.CurrentTimeChange(currentTime))
        }
    }

    fun triggerMediaEnd() {
        onEvent?.invoke(MediaPlayerEvent.MediaEnd)
    }

    /**
     * Ролик доиграл: событие экрану и пауза, если ролик не зациклен. Именно
     * пауза, а не переключение: перемотка на паузе к самому концу (свайп)
     * снимала бы паузу.
     */
    fun onMediaEnded() {
        triggerMediaEnd()
        if (!isLooping) pause()
    }

    fun triggerError(error: MediaPlayerError) {
        onError?.invoke(error)
    }

    /** Отвязывает колбэки экрана. Идемпотентно. */
    fun dispose() {
        onEvent = null
        onError = null
    }

    // RememberObserver: Compose сам зовёт onForgotten()/onAbandoned() при выходе
    // экземпляра из композиции (в т.ч. при смене ключа remember(url){ ... }).
    override fun onRemembered() { /* no-op */ }
    override fun onForgotten() { dispose() }
    override fun onAbandoned() { dispose() }
}
