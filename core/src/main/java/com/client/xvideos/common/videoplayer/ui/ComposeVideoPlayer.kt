package com.client.xvideos.common.videoplayer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.padding
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
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
    onZoomChanged: ((Boolean) -> Unit)? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val zoomState = rememberZoomState(maxScale = 3f)

    // При смене режима вписывания (FIT/FILL) или смене видеопотока сбрасываем зум.
    LaunchedEffect(playerHost.videoFitMode, playerHost.url) {
        zoomState.reset()
    }

    LaunchedEffect(zoomState.scale) {
        onZoomChanged?.invoke(isZoomActive(zoomState.scale))
    }

    Box(modifier = modifier.clipToBounds()) {

        // Видео-поверхность с зум-жестом. Одиночный и двойной тапы обрабатываются zoomable.
        Box(
            modifier = Modifier
                .fillMaxSize()
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

        // Слой UI поверх видео (теги/панель управления) — в scope корневого Box.
        overlay()
    }
}
