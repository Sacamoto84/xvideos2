package com.client.xvideos.common.videoplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import kotlin.math.absoluteValue

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerWithMenuContent(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    onClick: () -> Unit = {},
    autoRotate: Boolean,
    /**
     * Перемотка горизонтальным свайпом по нижней трети плеера.
     * Выключать, если плеер лежит внутри контейнера, который сам листается
     * по горизонтали: зона перемотки — дочерний элемент, она получает жест
     * первой и полностью блокирует листание.
     */
    seekDragEnabled: Boolean = true,
    resetZoomTrigger: Int = 0,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState(maxScale = 3f)
    LaunchedEffect(playerHost.videoFitMode, playerHost.url) { zoomState.reset() }

    val isZoomed by remember(zoomState) {
        derivedStateOf { isZoomActive(zoomState.scale) }
    }
    LaunchedEffect(isZoomed) {
        onZoomChanged(isZoomed)
    }

    LaunchedEffect(resetZoomTrigger) {
        if (resetZoomTrigger > 0) {
            coroutineScope.launch {
                zoomState.changeScale(1.0f, Offset.Zero)
            }
        }
    }

    var seekDragAmount by remember { mutableFloatStateOf(0f) }
    val seekDragModifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { seekDragAmount = 0f },
            onDragEnd = {
                val dx = if (seekDragAmount.absoluteValue > 400) 1f else 1 / 30f
                playerHost.isSliding = true
                playerHost.seekTo((playerHost.currentTime + (if (seekDragAmount > 0) dx else -dx)).coerceIn(0f, playerHost.totalTime.toFloat()))
                playerHost.isSliding = false
            },
            onDragCancel = { },
            onHorizontalDrag = { _, dragAmount -> seekDragAmount += dragAmount }
        )
    }

    Box(modifier = modifier.clipToBounds()) {

        Box(
            modifier = modifier.zoomable(
                zoomState = zoomState,
                zoomEnabled = true,
                enableOneFingerZoom = false,
                onTap = { onClick.invoke() },
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
            StaticPlayer(playerHost, autoRotate)
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

        // Нижняя сенсорная часть перемотки (отключается при активном увеличении кадра)
        if (seekDragEnabled && !isZoomed) {
            Box(
                modifier = Modifier
                    .fillMaxHeight(1 / 3f)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .then(seekDragModifier)
                    .alpha(0.5f)
                    .background(Color.Transparent)
            )
        }

        if (playerHost.isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(40.dp), color = Color.LightGray)
            }
        }
    }
}
