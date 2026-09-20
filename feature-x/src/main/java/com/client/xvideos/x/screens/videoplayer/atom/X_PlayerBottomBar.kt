package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.component.CustomSeekBar
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import java.util.Locale

/**
 * Нижняя панель управления X-плеером поверх видео.
 *
 * Время в [MediaPlayerHost] хранится в секундах: `currentTime` — Float, `totalTime` — Int.
 * Перемотка реализована через тот же контракт, что и в R-плеере: на время перетаскивания
 * выставляем `isSliding = true` и пишем превью-позицию в `seekToTime`, а по отпусканию
 * фиксируем её через [MediaPlayerHost.seekTo].
 *
 * @param onFullScreen переход в полноэкранный режим (текущую позицию прокидывает экран).
 */
@Composable
fun X_PlayerBottomBar(
    host: MediaPlayerHost,
    isFullScreen: Boolean = false,
    onFullScreen: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    // Запоминаем последнюю позицию слайдера, чтобы зафиксировать её по отпусканию.
    var sliderValue by remember(host) { mutableFloatStateOf(0f) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.45f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Play / Pause
        Icon(
            imageVector = if (host.isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = if (host.isPaused) "Play" else "Pause",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { host.togglePlayPause() }
        )

        // Текущее время
        val safeCurrentTimeSec = host.currentTime
            .takeIf { it.isFinite() && it >= 0f }
            ?.toInt()
            ?: 0
        Text(
            text = formatTime(safeCurrentTimeSec),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp
        )

        // Прогресс-бар
        val safeTotalTime = host.totalTime.coerceAtLeast(0).toFloat()
        val safeCurrentTime = host.currentTime
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, safeTotalTime)
            ?: 0f
        val safeMaxProgress = if (safeTotalTime > 0f) safeTotalTime else 0.1f

        CustomSeekBar(
            modifier = Modifier.weight(1f),
            progress = safeCurrentTime,
            maxProgress = safeMaxProgress,
            onValueChange = { v ->
                sliderValue = v
                host.isSliding = true
                host.seekToTime = v
            },
            onValueChangeFinished = {
                host.seekTo(sliderValue)
            },
            thumbRadius = 6.dp,
            trackHeight = 3.dp,
        )

        // Общее время
        Text(
            text = formatTime(host.totalTime),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp
        )

        // Меню выбора скорости воспроизведения
        PlaybackSpeedMenu(
            currentSpeed = host.speed,
            onSpeedSelected = { host.speed = it }
        )

        // Переключатель режима масштабирования Fit / Fill
        if (isFullScreen) {
            Text(
                text = if (host.videoFitMode == ScreenResize.FILL) "Fill" else "Fit",
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable {
                        host.videoFitMode = if (host.videoFitMode == ScreenResize.FIT) {
                            ScreenResize.FILL
                        } else {
                            ScreenResize.FIT
                        }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Полный экран (если поддержан экраном)
        if (onFullScreen != null) {
            Icon(
                imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Fullscreen",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable { onFullScreen() }
            )
        }
    }
}

private const val MAX_FORMATTED_SECONDS = 86400 * 7

/** Секунды → `M:SS` (или `H:MM:SS` для длинных видео). */
internal fun formatTime(totalSeconds: Int): String {
    val validSeconds = totalSeconds.coerceIn(0, MAX_FORMATTED_SECONDS)
    val hours = validSeconds / 3600
    val minutes = (validSeconds % 3600) / 60
    val seconds = validSeconds % 60
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}
