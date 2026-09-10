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
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.component.CustomSeekBar

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
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Запоминаем последнюю позицию слайдера, чтобы зафиксировать её по отпусканию.
    var sliderValue by remember { mutableFloatStateOf(0f) }

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
        Text(
            text = formatTime(host.currentTime.toInt()),
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontSize = 11.sp
        )

        // Прогресс-бар
        CustomSeekBar(
            modifier = Modifier.weight(1f),
            progress = host.currentTime.coerceIn(0f, host.totalTime.toFloat()),
            maxProgress = host.totalTime.toFloat().coerceAtLeast(0.1f),
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

        // Полный экран
        Icon(
            imageVector = Icons.Filled.Fullscreen,
            contentDescription = "Fullscreen",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .clickable { onFullScreen() }
        )
    }
}

/** Секунды → `M:SS` (или `H:MM:SS` для длинных видео). */
private fun formatTime(totalSeconds: Int): String {
    val s = totalSeconds.coerceAtLeast(0)
    val h = s / 3600
    val m = (s % 3600) / 60
    val sec = s % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, sec)
    else String.format("%d:%02d", m, sec)
}
