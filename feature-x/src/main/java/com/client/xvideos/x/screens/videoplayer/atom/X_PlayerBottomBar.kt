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
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.component.CustomSeekBar
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import java.util.Locale

private val FIT_MODE_SHAPE = RoundedCornerShape(4.dp)
private val BOTTOM_BAR_BG = Color(0x73000000)

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

    val onTogglePlayPause = remember(host) { { host.togglePlayPause() } }
    val onSeekBarValueChange: (Float) -> Unit = remember(host) {
        { v ->
            sliderValue = v
            host.isSliding = true
            host.seekToTime = v
        }
    }
    val onSeekBarValueChangeFinished: () -> Unit = remember(host) {
        {
            host.seekTo(sliderValue)
        }
    }
    val onSpeedSelected: (PlayerSpeed) -> Unit = remember(host) {
        { newSpeed -> host.speed = newSpeed }
    }
    val onToggleFitMode = remember(host) {
        {
            host.videoFitMode = if (host.videoFitMode == ScreenResize.FIT) {
                ScreenResize.FILL
            } else {
                ScreenResize.FIT
            }
        }
    }
    val onFullScreenClick = remember(onFullScreen) {
        onFullScreen?.let { action -> { action() } }
    }
    val formattedTotalTime = remember(host.totalTime) { formatTime(host.totalTime) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BOTTOM_BAR_BG)
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
                .clickable(onClick = onTogglePlayPause)
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
            onValueChange = onSeekBarValueChange,
            onValueChangeFinished = onSeekBarValueChangeFinished,
            thumbRadius = 6.dp,
            trackHeight = 3.dp,
        )

        // Общее время
        Text(
            text = formattedTotalTime,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp
        )

        // Меню выбора скорости воспроизведения
        PlaybackSpeedMenu(
            currentSpeed = host.speed,
            onSpeedSelected = onSpeedSelected
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
                    .clip(FIT_MODE_SHAPE)
                    .clickable(onClick = onToggleFitMode)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Полный экран (если поддержан экраном)
        if (onFullScreenClick != null) {
            Icon(
                imageVector = if (isFullScreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
                contentDescription = if (isFullScreen) "Exit Fullscreen" else "Fullscreen",
                tint = Color.White,
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onFullScreenClick)
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
