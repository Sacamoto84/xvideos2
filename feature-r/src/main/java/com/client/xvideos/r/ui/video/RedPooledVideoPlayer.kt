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
import kotlinx.coroutines.isActive
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.math.absoluteValue

/**
 * Прежний плеер клампил перемотку длительностью (`coerceAtMost(duration)`).
 * Без верхней границы при repeatMode = REPEAT_MODE_ONE перелёт за конец
 * мгновенно перезапускает ролик вместо остановки на последнем кадре.
 * До подготовки длительность неизвестна (C.TIME_UNSET) — тогда не клампим.
 */
private fun ExoPlayer.clampSeekPositionMs(positionMs: Long): Long {
    val floored = positionMs.coerceAtLeast(0L)
    val durationMs = duration
    return if (durationMs == C.TIME_UNSET) floored else floored.coerceAtMost(durationMs)
}

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
    onClick: () -> Unit,
    isBuferring: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(index, url) { feedState.mediaItemFor(index, url) }

    val player: ExoPlayer? = rememberPooledPlayer(
        mediaItem = mediaItem,
        playerPool = feedState.playerPool,
        playerSetup = { exo ->
            exo.setMediaSource(feedState.mediaSourceFor(mediaItem, index))
            exo.prepare()
        },
    )

    var isBuffering by remember(player) { mutableStateOf(true) }
    LaunchedEffect(isBuffering) { isBuferring(isBuffering) }

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
    LaunchedEffect(player, enableAB) {
        if (enableAB) player?.seekTo((timeA * 1000).toLong().coerceAtLeast(0L))
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
            if (enableAB && position >= timeB) exo.seekTo((timeA * 1000).toLong())
            delay(50)
        }
    }

    LaunchedEffect(player, isCurrentPage) {
        val exo = player ?: return@LaunchedEffect
        if (!isCurrentPage) return@LaunchedEffect

        onPlayerControlsReady(object : PlayerControls {
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
                exo.playWhenReady = true
            }
        })
    }

    val zoomState = rememberZoomState(maxScale = 3f)

    // Перемотка горизонтальным драгом по нижней трети экрана — как в прежнем пути ленты
    // (`VideoPlayerWithMenuContent`, seekDragEnabled): размашистый жест (> 400 px) двигает
    // на секунду, короткий — на кадр (1/30 c), направление задаёт знак смещения.
    // Детектор горизонтальный, поэтому вертикальный свайп страницы уходит пейджеру.
    var seekDragAmount by remember { mutableFloatStateOf(0f) }
    val seekDragModifier = Modifier.pointerInput(player) {
        val exo = player ?: return@pointerInput
        detectHorizontalDragGestures(
            onDragStart = { seekDragAmount = 0f },
            onDragEnd = {
                val stepMs = if (seekDragAmount.absoluteValue > 400) 1000L else (1000 / 30f).toLong()
                val deltaMs = if (seekDragAmount > 0) stepMs else -stepMs
                exo.seekTo(exo.clampSeekPositionMs(exo.currentPosition + deltaMs))
            },
            onDragCancel = { },
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
                ),
            contentScale = ContentScale.Fit,
            keepContentOnReset = true,
        )

        // Нижняя сенсорная зона перемотки — та же треть высоты, что и в прежнем плеере.
        Box(
            modifier = Modifier
                .fillMaxHeight(1 / 3f)
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .then(seekDragModifier)
        )

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
