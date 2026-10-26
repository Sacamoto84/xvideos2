package com.client.xvideos.common.videoplayer.local.molecule

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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.component.CustomSeekBar
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import com.client.xvideos.common.videoplayer.local.atom.FitModeToggle
import com.client.xvideos.common.videoplayer.local.atom.PlayerMuteButton
import com.client.xvideos.common.videoplayer.local.model.SeekScrub
import com.client.xvideos.common.videoplayer.local.model.finishSeekDrag
import com.client.xvideos.common.videoplayer.local.model.finishSeekDragIfActive
import com.client.xvideos.common.videoplayer.local.model.panelTouchTarget
import java.util.Locale

/**
 * Нижняя панель управления плеером поверх видео: ролики X (из сети и
 * скачанные) и записи эфиров XLive.
 *
 * Время в [MediaPlayerHost] хранится в секундах: `currentTime` — Float, `totalTime` — Int.
 * Пока палец ведёт ползунок, видео стоит (`isSliding = true`), а позицию под
 * пальцем видно по превью кадра; по отпусканию — одна перемотка через
 * [MediaPlayerHost.seekTo]. Перемотка на ходу убрана и у плеера скачанного
 * ролика: на каждое движение пальца он залипал в буферизации.
 *
 * @param onFullScreen переход в полноэкранный режим (текущую позицию прокидывает экран).
 * @param onScrub позиция под пальцем и границы полосы, пока палец ведёт ползунок;
 * `null` — палец отпущен.
 * @param muted звук выключен — для кнопки звука.
 * @param onToggleMute кнопка звука; `null` — кнопки нет (ролики X всегда без звука).
 */
@Composable
fun PlayerBottomBar(
    host: MediaPlayerHost,
    isFullScreen: Boolean = false,
    onFullScreen: (() -> Unit)? = null,
    onScrub: (SeekScrub?) -> Unit = {},
    modifier: Modifier = Modifier,
    muted: Boolean = true,
    onToggleMute: (() -> Unit)? = null,
) {
    // Запоминаем последнюю позицию слайдера, чтобы зафиксировать её по отпусканию.
    var sliderValue by remember(host) { mutableFloatStateOf(0f) }
    // Границы полосы в координатах корня: по ним оверлей ставит кадр A над бегунком.
    var trackStartPx by remember { mutableFloatStateOf(0f) }
    var trackWidthPx by remember { mutableFloatStateOf(0f) }

    val currentOnScrub by rememberUpdatedState(onScrub)

    val onTogglePlayPause = remember(host) { { host.togglePlayPause() } }
    val onSeekBarValueChange: (Float) -> Unit = remember(host) {
        { v ->
            sliderValue = v
            host.isSliding = true
            currentOnScrub(SeekScrub(v, trackStartPx, trackWidthPx))
        }
    }
    val onSeekBarValueChangeFinished: () -> Unit = remember(host) {
        { host.finishSeekDrag(sliderValue, currentOnScrub) }
    }
    // Панель может уйти из композиции прямо под пальцем, и жест тогда не
    // доходит до onValueChangeFinished — завершаем начатое перетаскивание сами.
    DisposableEffect(host) {
        onDispose { host.finishSeekDragIfActive(sliderValue, currentOnScrub) }
    }
    val onSeekBarPositioned: (LayoutCoordinates) -> Unit = remember {
        { trackStartPx = it.positionInRoot().x; trackWidthPx = it.size.width.toFloat() }
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
    // Позиция плеера приходит 20 раз в секунду. Раньше панель читала её здесь и
    // перекомпоновывалась на каждое обновление (проход 217, T2). Теперь текст
    // зависит от целых секунд, а полоса читает позицию только в отрисовке.
    val currentTimeSec by remember(host) {
        derivedStateOf { host.currentTime.takeIf { it.isFinite() && it >= 0f }?.toInt() ?: 0 }
    }
    val safeTotalTime = host.totalTime.coerceAtLeast(0).toFloat()
    val currentProgress: () -> Float = remember(host, safeTotalTime) {
        { host.currentTime.takeIf { it.isFinite() }?.coerceIn(0f, safeTotalTime) ?: 0f }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0x73000000))
            // Касания по панели не уходят к видео (свайп ±5 с, тап).
            .panelTouchTarget()
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
        Text(
            text = formatTime(currentTimeSec),
            style = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp
            )
        )

        // Прогресс-бар
        val safeMaxProgress = if (safeTotalTime > 0f) safeTotalTime else 0.1f

        CustomSeekBar(
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned(onSeekBarPositioned),
            progress = currentProgress,
            maxProgress = safeMaxProgress,
            onValueChange = onSeekBarValueChange,
            onValueChangeFinished = onSeekBarValueChangeFinished,
            thumbRadius = 6.dp,
            trackHeight = 3.dp,
            // Бегунок виден всегда: и в портрете, и на весь экран (решение владельца
            // от 21.10.2026 — раньше на весь экран он появлялся, только пока тянут).
            showThumbAlways = true,
        )

        // Общее время
        Text(
            text = formattedTotalTime,
            style = TextStyle(
                color = Color.White,
                fontFamily = FontFamily.SansSerif,
                fontSize = 11.sp
            )
        )

        // Звук — только у плеера, которому он нужен (записи эфиров).
        if (onToggleMute != null) PlayerMuteButton(muted = muted, onClick = onToggleMute)

        // Меню выбора скорости воспроизведения
        PlaybackSpeedMenu(
            currentSpeed = host.speed,
            onSpeedSelected = onSpeedSelected
        )

        // Переключатель режима масштабирования Fit / Fill
        if (isFullScreen) {
            FitModeToggle(
                videoFitMode = host.videoFitMode,
                onToggleFitMode = onToggleFitMode
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

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60

/** Секунды → `M:SS` (или `H:MM:SS` для длинных видео). */
fun formatTime(totalSeconds: Int): String {
    val validSeconds = totalSeconds.coerceIn(0, 86400 * 7)
    val hours = validSeconds / SECONDS_PER_HOUR
    val minutes = (validSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = validSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%d:%02d", minutes, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PlayerBottomBarPreview() {
    val host = remember {
        MediaPlayerHost().apply {
            totalTime = 600
            currentTime = 125f
        }
    }
    PlayerBottomBar(
        host = host,
        isFullScreen = true,
        onFullScreen = {}
    )
}
