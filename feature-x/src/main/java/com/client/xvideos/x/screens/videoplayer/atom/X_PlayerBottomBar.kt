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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.PlayerSpeed
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.component.CustomSeekBar
import com.client.xvideos.common.videoplayer.ui.component.PlaybackSpeedMenu
import java.util.Locale

private val FIT_MODE_CORNER = 4.dp
private val FIT_MODE_SHAPE = RoundedCornerShape(FIT_MODE_CORNER)
private val BOTTOM_BAR_BG = Color(0x73000000)

private val PLAY_PAUSE_ICON_SIZE = 28.dp
private val FULLSCREEN_ICON_SIZE = 28.dp
private val SEEK_BAR_THUMB_RADIUS = 6.dp
private val SEEK_BAR_TRACK_HEIGHT = 3.dp
private val BAR_HORIZONTAL_PADDING = 8.dp
private val BAR_VERTICAL_PADDING = 6.dp
private val BAR_CONTROL_SPACING = 8.dp
private val FIT_MODE_HORIZONTAL_PADDING = 4.dp
private val FIT_MODE_VERTICAL_PADDING = 2.dp
private val TIME_FONT_SIZE = 11.sp
private val CONTROL_ICON_TINT = Color.White
private val TIME_TEXT_COLOR = Color.White
private const val SAFE_MAX_PROGRESS_FALLBACK = 0.1f
private val ICON_PLAY = Icons.Filled.PlayArrow
private val ICON_PAUSE = Icons.Filled.Pause
private val ICON_FULLSCREEN = Icons.Filled.Fullscreen
private val ICON_FULLSCREEN_EXIT = Icons.Filled.FullscreenExit
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically

private val TIME_TEXT_STYLE = TextStyle(
    color = TIME_TEXT_COLOR,
    fontFamily = FontFamily.SansSerif,
    fontSize = TIME_FONT_SIZE
)

private val FIT_MODE_TEXT_STYLE = TextStyle(
    color = TIME_TEXT_COLOR,
    fontFamily = FontFamily.SansSerif,
    fontWeight = FontWeight.Bold,
    fontSize = TIME_FONT_SIZE
)

private const val CD_PLAY = "Play"
private const val CD_PAUSE = "Pause"
private const val CD_FULLSCREEN = "Fullscreen"
private const val CD_EXIT_FULLSCREEN = "Exit Fullscreen"
private const val LABEL_FIT = "Fit"
private const val LABEL_FILL = "Fill"

private val BOTTOM_BAR_BASE_MODIFIER = Modifier
    .fillMaxWidth()
    .background(BOTTOM_BAR_BG)
    .padding(horizontal = BAR_HORIZONTAL_PADDING, vertical = BAR_VERTICAL_PADDING)

private val PLAY_PAUSE_ICON_MODIFIER = Modifier.size(PLAY_PAUSE_ICON_SIZE)
private val FULLSCREEN_ICON_MODIFIER = Modifier.size(FULLSCREEN_ICON_SIZE)
private val FIT_MODE_FULL_MODIFIER = Modifier
    .clip(FIT_MODE_SHAPE)
    .padding(
        horizontal = FIT_MODE_HORIZONTAL_PADDING,
        vertical = FIT_MODE_VERTICAL_PADDING
    )
private val BAR_HORIZONTAL_ARRANGEMENT = Arrangement.spacedBy(BAR_CONTROL_SPACING)

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
        modifier = if (modifier == Modifier) BOTTOM_BAR_BASE_MODIFIER else modifier.then(BOTTOM_BAR_BASE_MODIFIER),
        verticalAlignment = ROW_VERTICAL_ALIGNMENT,
        horizontalArrangement = BAR_HORIZONTAL_ARRANGEMENT
    ) {

        // Play / Pause
        Icon(
            imageVector = if (host.isPaused) ICON_PLAY else ICON_PAUSE,
            contentDescription = if (host.isPaused) CD_PLAY else CD_PAUSE,
            tint = CONTROL_ICON_TINT,
            modifier = PLAY_PAUSE_ICON_MODIFIER.clickable(onClick = onTogglePlayPause)
        )

        // Текущее время
        val safeCurrentTimeSec = host.currentTime
            .takeIf { it.isFinite() && it >= 0f }
            ?.toInt()
            ?: 0
        Text(
            text = formatTime(safeCurrentTimeSec),
            style = TIME_TEXT_STYLE
        )

        // Прогресс-бар
        val safeTotalTime = host.totalTime.coerceAtLeast(0).toFloat()
        val safeCurrentTime = host.currentTime
            .takeIf { it.isFinite() }
            ?.coerceIn(0f, safeTotalTime)
            ?: 0f
        val safeMaxProgress = if (safeTotalTime > 0f) safeTotalTime else SAFE_MAX_PROGRESS_FALLBACK

        CustomSeekBar(
            modifier = Modifier.weight(1f),
            progress = safeCurrentTime,
            maxProgress = safeMaxProgress,
            onValueChange = onSeekBarValueChange,
            onValueChangeFinished = onSeekBarValueChangeFinished,
            thumbRadius = SEEK_BAR_THUMB_RADIUS,
            trackHeight = SEEK_BAR_TRACK_HEIGHT,
        )

        // Общее время
        Text(
            text = formattedTotalTime,
            style = TIME_TEXT_STYLE
        )

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
                imageVector = if (isFullScreen) ICON_FULLSCREEN_EXIT else ICON_FULLSCREEN,
                contentDescription = if (isFullScreen) CD_EXIT_FULLSCREEN else CD_FULLSCREEN,
                tint = CONTROL_ICON_TINT,
                modifier = FULLSCREEN_ICON_MODIFIER.clickable(onClick = onFullScreenClick)
            )
        }
    }
}

@Composable
private fun FitModeToggle(
    videoFitMode: ScreenResize,
    onToggleFitMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fitModeText = if (videoFitMode == ScreenResize.FILL) LABEL_FILL else LABEL_FIT
    val baseModifier = if (modifier == Modifier) FIT_MODE_FULL_MODIFIER else modifier.then(FIT_MODE_FULL_MODIFIER)
    Text(
        text = fitModeText,
        style = FIT_MODE_TEXT_STYLE,
        modifier = baseModifier.clickable(onClick = onToggleFitMode)
    )
}

private const val MAX_FORMATTED_SECONDS = 86400 * 7
private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
private const val TIME_FORMAT_WITH_HOURS = "%d:%02d:%02d"
private const val TIME_FORMAT_MINUTES_ONLY = "%d:%02d"

/** Секунды → `M:SS` (или `H:MM:SS` для длинных видео). */
internal fun formatTime(totalSeconds: Int): String {
    val validSeconds = totalSeconds.coerceIn(0, MAX_FORMATTED_SECONDS)
    val hours = validSeconds / SECONDS_PER_HOUR
    val minutes = (validSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = validSeconds % SECONDS_PER_MINUTE
    return if (hours > 0) String.format(Locale.US, TIME_FORMAT_WITH_HOURS, hours, minutes, seconds)
    else String.format(Locale.US, TIME_FORMAT_MINUTES_ONLY, minutes, seconds)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun X_PlayerBottomBarPreview() {
    val host = remember {
        MediaPlayerHost().apply {
            totalTime = 600
            currentTime = 125f
        }
    }
    X_PlayerBottomBar(
        host = host,
        isFullScreen = true,
        onFullScreen = {}
    )
}
