package com.client.xvideos.common.videoplayer.util

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.ui.AspectRatioFrameLayout
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.rememberExoPlayerWithLifecycle
import com.client.xvideos.common.videoplayer.rememberPlayerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun CMPPlayer2(
    modifier: Modifier,
    url: String,
    isPause: Boolean,
    totalTime: (Int) -> Unit,
    currentTime: (Float) -> Unit,
    isSliding: Boolean,
    seekToTime: Float?,
    speed: PlayerSpeed,
    size: ScreenResize,
    bufferCallback: (Boolean) -> Unit,
    didEndVideo: () -> Unit,
    loop: Boolean,
    volume: Float,
    isLiveStream: Boolean,
    error: (MediaPlayerError) -> Unit,
    headers: Map<String, String>?,
    drmConfig: DrmConfig?,
    selectedQuality: VideoQuality?,
    autoRotate: Boolean, // можно менять как нужно
    poster : (Boolean) -> Unit
) {
    val context = LocalContext.current
    val minBufferMs = 12_000
    val maxBufferMs = 45_000

    val exoPlayer = rememberExoPlayerWithLifecycle(
        url,
        context,
        isPause,
        isLiveStream,
        loop,
        headers,
        drmConfig,
        error,
        selectedQuality,
        minBufferMs = minBufferMs,
        maxBufferMs = maxBufferMs,
        bufferForPlaybackMs = 50,
        bufferForPlaybackAfterRebufferM = 100,
    )

    var currentRotate by remember { mutableFloatStateOf(0f) }

    val playerView = rememberPlayerView(exoPlayer, context)

    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(isBuffering) {
        bufferCallback(isBuffering)
    }

    LaunchedEffect(exoPlayer) {
        flow {
            while (isActive) {
                emit((exoPlayer.currentPosition / 1000f).coerceAtLeast(0f))
                delay(50)
            }
        }.collectLatest { currentTime(it) }

    }

    LaunchedEffect(autoRotate) {
        val rotateEffect = ScaleAndRotateTransformation.Builder().setRotationDegrees(if (autoRotate) -90f else 0f).build()
        exoPlayer.setVideoEffects(listOf(rotateEffect))
    }

    // Экран не гасим только пока реально идёт воспроизведение. Раньше флаг
    // выставлялся один раз и не снимался никогда: на паузе (и после ухода с
    // экрана) устройство всё равно не засыпало.
    DisposableEffect(playerView, isPause) {
        playerView.keepScreenOn = !isPause
        onDispose { playerView.keepScreenOn = false }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        AndroidView(
            factory = { playerView },
            modifier = modifier,
            update = {
                exoPlayer.playWhenReady = !isPause
                exoPlayer.volume = volume
                seekToTime?.let { exoPlayer.seekTo((it * 1000).toLong()) }
                exoPlayer.setPlaybackSpeed(speed.toFloat())
                playerView.resizeMode = when (size) {
                    ScreenResize.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    ScreenResize.FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        )

        // Manage player listener and lifecycle
        DisposableEffect(key1 = exoPlayer) {
            val listener = createPlayerListener(
                isSliding,
                totalTime,
                currentTime = {},
                loadingState = { isBuffering = it },
                didEndVideo,
                error,
                poster,
                sourceUrl = url
            )

            exoPlayer.addListener(listener)

            onDispose {
                // P3: release() выполняет создатель плеера (rememberExoPlayerWithLifecycle).
                // Здесь только снимаем слушатель и останавливаем воспроизведение.
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.removeListener(listener)
            }
        }

    }
}

private fun PlayerSpeed.toFloat(): Float {
    return when (this) {
        PlayerSpeed.X0_5 -> 0.5f
        PlayerSpeed.X1 -> 1f
        PlayerSpeed.X1_5 -> 1.5f
        PlayerSpeed.X2 -> 2f
    }
}
