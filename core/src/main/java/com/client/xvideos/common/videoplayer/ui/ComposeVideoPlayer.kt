package com.client.xvideos.common.videoplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.swipeSeekDirection
import com.client.xvideos.common.videoplayer.ui.component.SwipeSeekBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Единый Compose-плеер (общий с R/L).
 *
 * Рендерит видео-поверхность ([StaticPlayer] → `CMPPlayer2` → ExoPlayer) внутри
 * зум-области, поверх показывает индикатор буферизации, всплывающий HUD зума
 * и пользовательский [overlay] (теги, нижняя панель управления и т.п.).
 *
 * Освобождением ExoPlayer занимается сам [MediaPlayerHost] (RememberObserver),
 * создаваемый вызывающей стороной — здесь ресурсы не держим.
 *
 * @param onTap одиночный тап по видео (обычно play/pause или переключение оверлея).
 * @param onZoomChanged уведомление вызывающей стороны об активности масштабирования.
 * @param swipeSeekSeconds шаг перемотки свайпом по видео влево-вправо, в
 *                секундах; `null` — свайпа нет. При увеличенной картинке свайп
 *                двигает её, а не перематывает.
 * @param overlay UI поверх видео; выполняется в [BoxScope] корневого Box,
 *                поэтому внутри доступен `Modifier.align(...)`.
 */
@Composable
fun ComposeVideoPlayer(
    playerHost: MediaPlayerHost,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {},
    autoRotate: Boolean = false,
    zoomEnabled: Boolean = true,
    resetZoomTrigger: Int = 0,
    onZoomChanged: ((Boolean) -> Unit)? = null,
    swipeSeekSeconds: Int? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState(maxScale = 3f)

    // Значок шага свайпа. Номер свайпа перезапускает таймер скрытия, чтобы у
    // свайпов подряд значок не гас посреди серии.
    var swipeCount by remember { mutableIntStateOf(0) }
    var swipeLabel by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(swipeCount) {
        if (swipeLabel != null) {
            delay(600) // столько виден значок
            swipeLabel = null
        }
    }
    val onSwipeSeek: (Int) -> Unit = remember(playerHost, swipeSeekSeconds) {
        { direction ->
            val seconds = swipeSeekSeconds ?: 0
            playerHost.seekBy((direction * seconds).toFloat())
            swipeLabel = if (direction > 0) "+$seconds с" else "−$seconds с"
            swipeCount++
        }
    }

    // При смене режима вписывания (FIT/FILL) или смене видеопотока сбрасываем зум.
    LaunchedEffect(playerHost.videoFitMode, playerHost.url) {
        zoomState.reset()
    }

    val isZoomed by remember(zoomState) {
        derivedStateOf { isZoomActive(zoomState.scale) }
    }

    LaunchedEffect(isZoomed) {
        onZoomChanged?.invoke(isZoomed)
    }

    LaunchedEffect(resetZoomTrigger) {
        if (resetZoomTrigger > 0) {
            coroutineScope.launch {
                zoomState.changeScale(1.0f, Offset.Zero)
            }
        }
    }

    Box(modifier = modifier.clipToBounds()) {

        // Видео-поверхность с зум-жестом. Одиночный и двойной тапы обрабатываются zoomable.
        // Свайп — снаружи zoomable: тот при масштабе 1 перетаскивание не забирает.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .swipeSeekGesture(
                    enabled = swipeSeekSeconds != null,
                    isZoomed = { isZoomActive(zoomState.scale) },
                    onSwipe = onSwipeSeek,
                )
                .zoomable(
                    zoomState = zoomState,
                    zoomEnabled = zoomEnabled,
                    enableOneFingerZoom = false,
                    onTap = { onTap() },
                    onDoubleTap = { tapOffset ->
                        coroutineScope.launch {
                            if (zoomState.scale > 1.05f) {
                                zoomState.changeScale(1.0f, Offset.Zero)
                            } else {
                                zoomState.changeScale(2.5f, tapOffset)
                            }
                        }
                    }
                )
        ) {
            StaticPlayer(playerHost = playerHost, autoRotate = autoRotate)
        }

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

        // Индикатор буферизации поверх видео.
        if (playerHost.isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.LightGray
                )
            }
        }

        // Шаг перемотки свайпом — над серединой: в самой середине после
        // перемотки ролика из сети крутится индикатор буферизации.
        swipeLabel?.let { label ->
            SwipeSeekBadge(
                text = label,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 120.dp)
            )
        }

        // Слой UI поверх видео (теги/панель управления) — в scope корневого Box.
        overlay()
    }
}

/**
 * Свайп по видео одним пальцем: по отпусканию — направление из
 * [swipeSeekDirection] (порог 48dp), если картинка не увеличена. Касания не
 * забирает: тапы и двойной тап остаются у `zoomable`, а тот при масштабе 1
 * перетаскивание не забирает. Касание двумя пальцами (щипок) — не свайп.
 */
private fun Modifier.swipeSeekGesture(
    enabled: Boolean,
    isZoomed: () -> Boolean,
    onSwipe: (Int) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(onSwipe) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var last = down.position
            var multiTouch = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                if (event.changes.count { it.pressed } > 1) multiTouch = true
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                last = change.position
                if (!change.pressed) break
            }
            if (multiTouch || isZoomed()) return@awaitEachGesture
            val direction = swipeSeekDirection(
                dx = last.x - down.position.x,
                dy = last.y - down.position.y,
                // Короче — тап, а не свайп.
                minDistancePx = 48.dp.toPx(),
            )
            if (direction != 0) onSwipe(direction)
        }
    }
}
