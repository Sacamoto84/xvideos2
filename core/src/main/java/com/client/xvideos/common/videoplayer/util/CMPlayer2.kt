package com.client.xvideos.common.videoplayer.util

import android.view.View
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.ui.compose.ContentFrame
import com.client.xvideos.common.videoplayer.host.DrmConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerError
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.rememberExoPlayerWithLifecycle
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
    poster: (Boolean) -> Unit
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
        val rotateEffect = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(if (autoRotate) -90f else 0f).build()
        exoPlayer.setVideoEffects(listOf(rotateEffect))
    }

    // Раньше эти четыре строки жили в `update` у AndroidView. Теперь это обычные
    // эффекты: применяются при изменении своего входа, а не на каждый layout.
    LaunchedEffect(exoPlayer, isPause) { exoPlayer.playWhenReady = !isPause }
    LaunchedEffect(exoPlayer, volume) { exoPlayer.volume = volume }
    LaunchedEffect(exoPlayer, speed) { exoPlayer.setPlaybackSpeed(speed.toFloat()) }
    LaunchedEffect(exoPlayer, seekToTime) {
        seekToTime?.let { exoPlayer.seekTo((it * 1000).toLong()) }
    }

    // Экран не гасим только пока реально идёт воспроизведение. Флаг живёт на
    // host-view всей Compose-иерархии, а плееров на экране может быть несколько
    // (списки с UrlVideoLite), поэтому считаем играющих: снимать флаг можно лишь
    // когда замолчал последний. Раньше он висел на своём PlayerView и такой
    // проблемы не было.
    val view = LocalView.current
    DisposableEffect(view, isPause) {
        if (isPause) {
            onDispose { }
        } else {
            KeepScreenOnCounter.acquire(view)
            onDispose { KeepScreenOnCounter.release(view) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        ContentFrame(
            player = exoPlayer,
            modifier = modifier,
            contentScale = when (size) {
                ScreenResize.FIT -> ContentScale.Fit
                ScreenResize.FILL -> ContentScale.Crop
            },
            keepContentOnReset = true,
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
                // release() выполняет создатель плеера (rememberExoPlayerWithLifecycle).
                // Здесь только снимаем слушатель и останавливаем воспроизведение.
                exoPlayer.stop()
                exoPlayer.clearMediaItems()
                exoPlayer.removeListener(listener)
            }
        }
    }
}

/**
 * Счётчик играющих плееров на одном host-view: `keepScreenOn` — свойство view,
 * а не плеера, поэтому владельцев у флага может быть несколько.
 * Только главный поток — Compose-эффекты выполняются на нём.
 */
private object KeepScreenOnCounter {

    private val counts = mutableMapOf<View, Int>()

    fun acquire(view: View) {
        val next = (counts[view] ?: 0) + 1
        counts[view] = next
        view.keepScreenOn = true
    }

    fun release(view: View) {
        val next = (counts[view] ?: 1) - 1
        if (next <= 0) {
            counts.remove(view)
            view.keepScreenOn = false
        } else {
            counts[view] = next
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
