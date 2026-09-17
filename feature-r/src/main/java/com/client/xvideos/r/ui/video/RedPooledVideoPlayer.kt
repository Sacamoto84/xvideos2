package com.client.xvideos.r.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.lifecycle.rememberPooledPlayer
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.r.common.video.PlayerControls
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import com.client.xvideos.common.videoplayer.ui.VideoZoomHud
import com.client.xvideos.common.videoplayer.ui.isZoomActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.math.absoluteValue

/**
 * Прежний плеер клампил перемотку длительностью (`coerceAtMost(duration)`).
 * Без верхней границы при repeatMode = REPEAT_MODE_ONE перелёт за конец
 * мгновенно перезапускает ролик вместо остановки на последнем кадре.
 * До подготовки длительность неизвестна (C.TIME_UNSET) — тогда не клампим.
 */
internal fun clampSeekPositionMs(positionMs: Long, durationMs: Long): Long {
    val floored = positionMs.coerceAtLeast(0L)
    return if (durationMs == C.TIME_UNSET) floored else floored.coerceAtMost(durationMs)
}

internal fun calculateDragDeltaMs(seekDragAmount: Float): Long {
    if (seekDragAmount == 0f || !seekDragAmount.isFinite()) return 0L
    val stepMs = if (seekDragAmount.absoluteValue > 400f) 1000L else (1000f / 30f).toLong()
    return if (seekDragAmount > 0f) stepMs else -stepMs
}

internal fun isValidABRange(enableAB: Boolean, timeA: Float, timeB: Float): Boolean {
    return enableAB && timeA.isFinite() && timeB.isFinite() && timeB > timeA
}

private fun ExoPlayer.clampSeekPositionMs(positionMs: Long): Long =
    com.client.xvideos.r.ui.video.clampSeekPositionMs(positionMs, duration)

/**
 * Тик времени плеера ленты.
 *
 * `fun interface`, а не `(Float, Int) -> Unit`: у Kotlin-функциональных типов
 * параметры генерик, `Function2<Float, Integer, Unit>` боксит оба примитива на
 * каждом вызове — а вызовов здесь 20 в секунду. У `fun interface` сигнатура
 * компилируется в `onTime(float, int)`, без бокса и без промежуточного `Pair`.
 */
fun interface FeedTimeListener {
    fun onTime(positionSeconds: Float, durationSeconds: Int)
}

/**
 * Страница ленты, работающая на общем пуле плееров [FeedPlayerState].
 *
 * Отличие от прежнего плеера ленты: `ExoPlayer` не создаётся на каждую страницу,
 * а берётся из пула (`rememberPooledPlayer`) и возвращается туда же при уходе
 * страницы из композиции. Медиа-источник приходит от preload-менеджера, то есть
 * соседние ролики уже частично загружены к моменту свайпа.
 */
@OptIn(UnstableApi::class)
@Suppress("LongParameterList", "CyclomaticComplexMethod", "LongMethod")
@Composable
fun RedPooledVideoPlayer(
    feedState: FeedPlayerState,
    index: Int,
    url: String,
    play: Boolean,
    isMute: Boolean,
    isCurrentPage: Boolean,
    autoRotate: Boolean,
    timeA: Float,
    timeB: Float,
    enableAB: Boolean,
    onTimeChanged: FeedTimeListener,
    onPlayerControlsReady: (PlayerControls) -> Unit,
    onPlayerControlsRelease: (PlayerControls) -> Unit,
    onClick: () -> Unit,
    onBufferingChanged: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(index, url) { feedState.mediaItemFor(index, url) }

    val player: ExoPlayer? = rememberPooledPlayer(
        mediaItem = mediaItem,
        playerPool = feedState.playerPool,
        // playerTeardown намеренно не передаём: см. KDoc у FeedPlayerState.playerPool.
        // Всё, что страница ставит на плеер, она выставляет заново при получении —
        // эффектами с ключом `player` ниже, а не снимает при возврате в пул.
        playerSetup = { exo ->
            exo.setMediaSource(feedState.mediaSourceFor(mediaItem, index))
            exo.prepare()
        },
    )

    var isBuffering by remember(player) { mutableStateOf(true) }
    LaunchedEffect(isBuffering) { onBufferingChanged(isBuffering) }

    // Именно LifecycleStartEffect, а не LaunchedEffect: прежний путь ленты вешал
    // `LifecycleEventObserver` (см. `ExoPlayerLifecycle.rememberExoPlayerWithLifecycle`)
    // и снимал playWhenReady на ON_PAUSE/ON_STOP. Без этого свёрнутое приложение
    // продолжает играть звук ленты, а setForegroundMode(true) ещё и удерживает декодеры.
    LifecycleStartEffect(player, play, isCurrentPage) {
        player?.playWhenReady = play && isCurrentPage
        onStopOrDispose { player?.playWhenReady = false }
    }

    LaunchedEffect(player, isMute) {
        player?.volume = if (isMute) 0f else 1f
    }

    LaunchedEffect(player, autoRotate) {
        val rotate = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(if (autoRotate) -90f else 0f)
            .build()
        player?.setVideoEffects(listOf(rotate))
    }

    DisposableEffect(player) {
        val exo = player
        if (exo == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                }
            }
            exo.addListener(listener)
            isBuffering = exo.playbackState == Player.STATE_BUFFERING
            onDispose { exo.removeListener(listener) }
        }
    }

    // Включили A-B — сразу встаём на точку A, как делал прежний плеер
    // (`LaunchedEffect(enableAB) { playerHost.seekTo(timeA) }`). Без этого первый
    // проход шёл бы от текущей позиции, а не от начала петли.
    LaunchedEffect(player, enableAB, timeA, timeB) {
        if (isValidABRange(enableAB, timeA, timeB)) {
            player?.seekTo((timeA * 1000).toLong().coerceAtLeast(0L))
        }
    }

    // Время/длительность и петля A-B. Шаг 50 мс — как в прежнем CMPPlayer2,
    // чтобы поведение полосы времени и A-B не изменилось.
    LaunchedEffect(player, isCurrentPage, enableAB, timeA, timeB) {
        val exo = player ?: return@LaunchedEffect
        // Нетекущие страницы не играют (playWhenReady = play && isCurrentPage), время на них
        // не движется — крутить на них опрос смысла нет. Без этого выхода при трёх живых
        // страницах работали бы три корутины по 20 Гц.
        if (!isCurrentPage) return@LaunchedEffect
        // Аналог прежнего `distinctUntilChanged()`: на паузе значение не меняется,
        // и апстрим не дёргается 20 раз в секунду впустую. Примитивные локальные
        // переменные вместо Pair — на 20 Гц это единственная аллокация в цикле.
        // NaN не равен ничему, включая себя, поэтому первый проход всегда репортит.
        var lastPosition = Float.NaN
        var lastDuration = -1
        while (isActive) {
            val position = (exo.currentPosition / 1000f).coerceAtLeast(0f)
            val durationMs = exo.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            val duration = (durationMs / 1000).toInt()
            if (position != lastPosition || duration != lastDuration) {
                lastPosition = position
                lastDuration = duration
                onTimeChanged.onTime(position, duration)
            }
            if (isValidABRange(enableAB, timeA, timeB) && position >= timeB) {
                exo.seekTo((timeA * 1000).toLong().coerceAtLeast(0L))
            }
            delay(50)
        }
    }

    // DisposableEffect, а не LaunchedEffect: страница обязана отозвать свои controls
    // при уходе из композиции. PlayerPool.yield() к этому моменту уже сделал плееру
    // stop() и clearMediaItems(), и оставленная снаружи ссылка дёргала бы пустой плеер.
    DisposableEffect(player, isCurrentPage) {
        val exo = player
        if (exo == null || !isCurrentPage) {
            onDispose { }
        } else {
            val controls = object : PlayerControls {
                override fun forward(seconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition + (seconds * 1000).toLong()))
                }

                override fun rewind(seconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition - (seconds * 1000).toLong()))
                }

                override fun seekTo(positionSeconds: Float) {
                    exo.seekTo(exo.clampSeekPositionMs((positionSeconds * 1000).toLong()))
                }

                override fun stop() {
                    exo.playWhenReady = false
                    exo.seekTo(0L)
                }

                override fun pause() {
                    exo.playWhenReady = false
                }

                override fun play() {
                    if (isCurrentPage) {
                        exo.playWhenReady = true
                    }
                }
            }
            onPlayerControlsReady(controls)
            onDispose { onPlayerControlsRelease(controls) }
        }
    }

    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState(maxScale = 3f)

    // При уходе страницы из фокуса сбрасываем зум, чтобы соседние видео не оставались увеличенными
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            zoomState.reset()
        }
    }

    val isZoomed = isZoomActive(zoomState.scale)
    LaunchedEffect(isZoomed) {
        onZoomChanged(isZoomed)
    }

    // Перемотка горизонтальным драгом по нижней трети экрана — как в прежнем пути ленты
    // (`VideoPlayerWithMenuContent`, seekDragEnabled): размашистый жест (> 400 px) двигает
    // на секунду, короткий — на кадр (1/30 c), направление задаёт знак смещения.
    // Детектор горизонтальный, поэтому вертикальный свайп страницы уходит пейджеру.
    var seekDragAmount by remember { mutableFloatStateOf(0f) }
    val seekDragModifier = Modifier.pointerInput(player, isCurrentPage) {
        val exo = player ?: return@pointerInput
        if (!isCurrentPage) return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = { seekDragAmount = 0f },
            onDragEnd = {
                val deltaMs = calculateDragDeltaMs(seekDragAmount)
                if (deltaMs != 0L) {
                    exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition + deltaMs))
                }
            },
            onDragCancel = { seekDragAmount = 0f },
            onHorizontalDrag = { _, dragAmount -> seekDragAmount += dragAmount }
        )
    }

    // clipToBounds — как в прежнем `Box(modifier.clipToBounds())`: увеличенный зумом кадр
    // не должен выезжать поверх оверлея и нижней панели.
    Box(modifier = modifier.fillMaxSize().clipToBounds().background(Color.Black)) {
        ContentFrame(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onTap = { onClick() },
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (zoomState.scale > 1.05f) {
                                zoomState.changeScale(1.0f, Offset.Zero)
                            } else {
                                zoomState.changeScale(2.5f, tapOffset)
                            }
                        }
                    },
                ),
            contentScale = ContentScale.Fit,
            keepContentOnReset = true,
        )

        // Всплывающий индикатор масштаба (HUD)
        VideoZoomHud(
            scale = zoomState.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp),
            onReset = {
                coroutineScope.launch {
                    zoomState.changeScale(1.0f, Offset.Zero)
                }
            }
        )

        // Нижняя сенсорная зона перемотки — отключается при активном увеличении кадра
        if (!isZoomed) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(1 / 3f)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .then(seekDragModifier)
            )
        }

        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.LightGray,
                )
            }
        }
    }
}
