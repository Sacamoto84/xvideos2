package com.client.xvideos.common.videoplayer.util

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.ui.compose.ContentFrame
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.playerBufferWindow
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.applock.LocalAppLockActive
import com.client.xvideos.common.util.KeepScreenOn
import com.client.xvideos.common.videoplayer.rememberExoPlayerWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@OptIn(UnstableApi::class)
@Composable
fun CMPPlayer2(
    modifier: Modifier = Modifier,
    config: PlayerPlaybackConfig,
    callbacks: PlayerPlaybackCallbacks = PlayerPlaybackCallbacks()
) {
    val context = LocalContext.current
    // Буфер берётся при создании плеера (remember(context)), поэтому превью
    // из отрезков приходит сюда уже со списком отрезков (LocalVideoPreview
    // создаёт хост после чтения длительности).
    val bufferWindow = playerBufferWindow(hasClips = config.clips.isNotEmpty())
    val minBufferMs = bufferWindow.minMs
    val maxBufferMs = bufferWindow.maxMs

    val exoPlayer = rememberExoPlayerWithLifecycle(
        config.url,
        context,
        config.isPause,
        config.isLiveStream,
        config.loop,
        config.headers,
        config.drmConfig,
        callbacks.error,
        minBufferMs = minBufferMs,
        maxBufferMs = maxBufferMs,
        bufferForPlaybackMs = 50,
        bufferForPlaybackAfterRebufferM = 100,
        playFromTime = config.playFromTime,
        clips = config.clips,
    )

    val currentCallbacks by rememberUpdatedState(callbacks)
    var isBuffering by remember { mutableStateOf(false) }

    LaunchedEffect(isBuffering) {
        currentCallbacks.bufferCallback(isBuffering)
    }

    LaunchedEffect(exoPlayer, config.isPause) {
        var lastPosition = Float.NaN
        while (isActive) {
            val position = (exoPlayer.currentPosition / 1000f).coerceAtLeast(0f)
            if (position != lastPosition) {
                lastPosition = position
                currentCallbacks.currentTime(position)
            }
            if (config.isPause) {
                // На паузе позиция не меняется — после первой отправки не крутим 20 Гц опрос
                break
            }
            delay(50)
        }
    }

    // Эффекты ставим, только когда поворот понадобился: непустой список
    // включает конвейер обработки кадров, а раньше он включался у каждого
    // плеера — и у мини-плееров в списках — ради поворота на ноль градусов.
    var rotationEffectsApplied by remember(exoPlayer) { mutableStateOf(false) }
    LaunchedEffect(exoPlayer, config.autoRotate) {
        val degrees = rotationToApply(config.autoRotate, rotationEffectsApplied) ?: return@LaunchedEffect
        val rotateEffect = ScaleAndRotateTransformation.Builder().setRotationDegrees(degrees).build()
        exoPlayer.setVideoEffects(listOf(rotateEffect))
        rotationEffectsApplied = true
    }

    val isAppLockActive = LocalAppLockActive.current.value

    // Раньше эти строки жили в `update` у AndroidView. Теперь это обычные
    // эффекты: применяются при изменении своего входа, а не на каждый layout.
    // При блокировке приложения немедленно снимаем воспроизведение и глушим звук.
    LaunchedEffect(exoPlayer, config.isPause, isAppLockActive) {
        exoPlayer.playWhenReady = !config.isPause && !isAppLockActive
    }
    LaunchedEffect(exoPlayer, config.volume, isAppLockActive) {
        exoPlayer.volume = if (isAppLockActive) 0f else config.volume
    }
    LaunchedEffect(exoPlayer, config.speed) { exoPlayer.setPlaybackSpeed(config.speed.toFloat()) }
    // Перемотка на ходу, пока палец ведёт ползунок. Время на полосе здесь не
    // трогаем: пока ползунок держат, хост позицию от плеера всё равно не берёт.
    LaunchedEffect(exoPlayer, config.seekToTime) {
        config.seekToTime?.let {
            if (it.isFinite() && it >= 0f) {
                exoPlayer.seekTo((it * 1000).toLong())
            }
        }
    }
    // Перемотка из MediaPlayerHost.seekTo — только этим эффектом. Ключ — запрос
    // с номером, а не seekToTime: тот сбрасывает опрос позиции, и перемотка при
    // отпускании ползунка могла не дойти до плеера. После seekTo плеер уже
    // отдаёт новую позицию, и подтверждение снимает запрет на опрос.
    LaunchedEffect(exoPlayer, config.seekRequest) {
        config.seekRequest?.let { request ->
            exoPlayer.seekTo((request.seconds * 1000).toLong())
            currentCallbacks.seekApplied(request.id)
        }
    }

    // Экран не гасим только пока реально идёт воспроизведение и приложение не заблокировано.
    KeepScreenOn(active = !config.isPause && !isAppLockActive)

    // modifier — корню: раньше он уходил рамке видео, а чёрная подложка всегда
    // занимала всё доступное место, каким бы ни был заданный размер.
    Box(modifier = modifier.background(Color.Black)) {

        ContentFrame(
            player = exoPlayer,
            modifier = Modifier.fillMaxSize(),
            contentScale = when (config.size) {
                ScreenResize.FIT -> ContentScale.Fit
                ScreenResize.FILL -> ContentScale.Crop
            },
            keepContentOnReset = true,
        )

        // Dynamic state to prevent capturing stale isSliding value across recompositions
        val currentIsSliding by rememberUpdatedState(config.isSliding)

        // Manage player listener and lifecycle
        DisposableEffect(key1 = exoPlayer) {
            val listener = createPlayerListener(
                isSliding = { currentIsSliding },
                totalTime = { currentCallbacks.totalTime(it) },
                currentTime = {},
                loadingState = { isBuffering = it },
                didEndVideo = { currentCallbacks.didEndVideo() },
                onError = { currentCallbacks.error(it) },
                poster = { currentCallbacks.poster(it) },
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

private fun PlayerSpeed.toFloat(): Float = this.speed

/**
 * На сколько градусов повернуть кадр; `null` — эффекты не трогать.
 *
 * Пока автоповорот ни разу не включали, эффектов у плеера нет вовсе. После
 * выключения поворот сбрасывается в ноль: убрать уже включённый конвейер
 * обработки кадров на ходу нельзя, но кадр возвращается в исходное положение.
 *
 * @param effectsApplied эффекты этому плееру уже ставили.
 */
fun rotationToApply(autoRotate: Boolean, effectsApplied: Boolean): Float? = when {
    autoRotate -> -90f
    effectsApplied -> 0f
    else -> null
}
