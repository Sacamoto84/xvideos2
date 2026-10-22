package com.client.xvideos.common.videoplayer

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.net.toUri
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ClippingMediaSource
import com.client.xvideos.common.applock.LocalAppLockActive
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.model.MediaClip
import com.client.xvideos.common.videoplayer.model.playerRepeatMode
import com.client.xvideos.common.videoplayer.model.withAudioDisabledForClips
import com.client.xvideos.common.videoplayer.util.createHlsMediaSource
import com.client.xvideos.common.videoplayer.util.createHlsMediaSourceWithDrm
import com.client.xvideos.common.videoplayer.util.createProgressiveMediaSource
import com.client.xvideos.common.videoplayer.util.getExoPlayerLifecycleObserver
import com.client.xvideos.common.videoplayer.util.isHlsUrl
import kotlinx.coroutines.CancellationException

@OptIn(UnstableApi::class)
@Composable
fun rememberExoPlayerWithLifecycle(
    url: String,
    context: Context,
    isPause: Boolean,
    isLiveStream: Boolean,
    isLooping: Boolean,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    error: (MediaPlayerError) -> Unit,
    minBufferMs: Int = 2500,
    maxBufferMs: Int = 30000,
    bufferForPlaybackMs: Int = 500,
    bufferForPlaybackAfterRebufferM: Int = 1000,
    seekBackIncrementMs: Long = 1000L,
    seekForwardIncrementMs: Long = 1000L,
    playFromTime: Float? = null,
    clips: List<MediaClip> = emptyList(),
): ExoPlayer {
    val lifecycleOwner = LocalLifecycleOwner.current

    // P4: не пересоздаём LoadControl на каждой рекомпозиции.
    val loadControl = remember(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferM) {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(minBufferMs, maxBufferMs, bufferForPlaybackMs, bufferForPlaybackAfterRebufferM)
            .build()
    }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setLoadControl(loadControl)
            .setSeekForwardIncrementMs(seekForwardIncrementMs) // Приращение перемотки вперёд (по умолчанию 1 сек)
            .setSeekBackIncrementMs(seekBackIncrementMs)       // Приращение перемотки назад (по умолчанию 1 сек)
            .build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                setHandleAudioBecomingNoisy(true)
            }
    }

    val currentError by rememberUpdatedState(error)

    // P3: единый владелец жизненного цикла плеера — тот, кто его создал.
    // Освобождаем ровно здесь, при выходе из композиции (или смене player).
    DisposableEffect(exoPlayer) {
        val errorListener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentError(MediaPlayerError.PlaybackError(error.message ?: "Playback error"))
            }
        }
        exoPlayer.addListener(errorListener)
        onDispose {
            exoPlayer.removeListener(errorListener)
            exoPlayer.release()
        }
    }

    // С отрезками повторяется весь список, без них — сам ролик.
    LaunchedEffect(isLooping, clips) {
        exoPlayer.repeatMode = playerRepeatMode(isLooping, hasClips = clips.isNotEmpty())
    }

    // Все входы загрузки — в ключах: раньше источник пересобирался только по
    // смене адреса, хотя читает и заголовки, и DRM, и признак прямого эфира.
    LaunchedEffect(exoPlayer, url, headers, drmConfig, isLiveStream, clips) {
        if (url.isBlank()) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            return@LaunchedEffect
        }
        try {
            val mediaItem = MediaItem.fromUri(url.toUri())

            // Источник создаётся заново на каждый вызов: у каждого отрезка свой,
            // один экземпляр в плейлисте дважды не используется.
            val newMediaSource = {
                when {
                    drmConfig != null -> createHlsMediaSourceWithDrm(mediaItem, headers, drmConfig)
                    isLiveStream || isHlsUrl(url) -> createHlsMediaSource(
                        mediaItem,
                        headers
                    )
                    else -> createProgressiveMediaSource(mediaItem, context, headers)
                }
            }

            val initialSeekMs = (playFromTime?.times(1000f)?.toLong() ?: 0L).coerceAtLeast(0L)
            exoPlayer.apply {
                stop()
                clearMediaItems()
                // У превью из отрезков звук не нужен: без звуковой дорожки нет и
                // AudioTrack, который иначе пересоздавался на каждой смене отрезка.
                trackSelectionParameters = trackSelectionParameters.withAudioDisabledForClips(clips.isNotEmpty())
                if (clips.isEmpty()) {
                    setMediaSource(newMediaSource())
                    prepare()
                    seekTo(0, initialSeekMs)
                } else {
                    // Отрезки одного файла друг за другом; начало — первый отрезок.
                    setMediaSources(
                        clips.map { clip ->
                            ClippingMediaSource.Builder(newMediaSource())
                                .setStartPositionMs(clip.startMs)
                                .setEndPositionMs(clip.endMs)
                                .build()
                        }
                    )
                    prepare()
                }
            }

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            currentError(MediaPlayerError.PlaybackError(e.message ?: "Failed to load media"))
        }
    }

    val currentIsPause by rememberUpdatedState(isPause)
    // Объект состояния замка, а не значение: ON_RESUME читает его в момент события.
    val currentAppLockActive by rememberUpdatedState(LocalAppLockActive.current)

    DisposableEffect(lifecycleOwner, exoPlayer) {
        val lifecycleObserver = getExoPlayerLifecycleObserver(
            exoPlayer = exoPlayer,
            isPause = { currentIsPause },
            isAppLocked = { currentAppLockActive.value },
        )
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose { lifecycleOwner.lifecycle.removeObserver(lifecycleObserver) }
    }
    return exoPlayer
}
