package com.client.xvideos.r.ui.video.player_row_mini.atom

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.videoplayer.host.MediaPlayerEvent
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.StaticPlayer
import com.client.xvideos.r.ui.video.CanvasTimeDurationLine1


private val CLICK_OVERLAY_BOTTOM_PADDING = 48.dp
private val TIMELINE_HORIZONTAL_PADDING = 2.dp
private val TIMELINE_OFFSET_Y = 5.dp
private const val FADE_DURATION_MS = 300

private val ENTER_FADE = fadeIn(animationSpec = tween(FADE_DURATION_MS))
private val EXIT_FADE = fadeOut(animationSpec = tween(FADE_DURATION_MS))

/**
 * Превьюшка для режима в два столбика
 */
@Composable
fun Red_Video_Lite_Row2(
    url: String,
    play: Boolean = true,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    poster: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    var time by remember { mutableFloatStateOf(0f) }
    var duration by remember { mutableIntStateOf(0) }

    var isBuffering by remember { mutableStateOf(false) }

    val playerHost = remember(url) { MediaPlayerHost(mediaUrl = url, isPaused = !play, isMuted = true) }

    LaunchedEffect(play) { if (play) playerHost.play() else playerHost.pause() }
    LaunchedEffect(Unit) { playerHost.videoFitMode = ScreenResize.FIT; playerHost.mute() }

    LaunchedEffect(Unit) {
        playerHost.onEvent = { event ->
            when (event) {
                is MediaPlayerEvent.CurrentTimeChange -> { time = event.currentTime }
                is MediaPlayerEvent.TotalTimeChange -> { duration = event.totalTime }
                is MediaPlayerEvent.BufferChange -> { isBuffering = event.isBuffering }
                else -> {}
            }
        }
    }

    LaunchedEffect(playerHost.poster) { poster(playerHost.poster) }

    val onSeek: (Float) -> Unit = remember(playerHost) {
        { seekTime -> playerHost.seekTo(seekTime) }
    }
    val onSeekFinished: () -> Unit = remember(playerHost) {
        { playerHost.play() }
    }

    Box(modifier = modifier.fillMaxWidth()) {

        StaticPlayer(playerHost, false)

        Box(
            modifier = Modifier
                .padding(bottom = CLICK_OVERLAY_BOTTOM_PADDING)
                .fillMaxSize()
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
        )

        AnimatedVisibility(
            visible = !playerHost.poster,
            enter = ENTER_FADE,
            exit = EXIT_FADE,
            modifier = Modifier.align(Alignment.BottomEnd).fillMaxWidth(),
        ) {
            Box(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.BottomCenter
            ) {
                CanvasTimeDurationLine1(
                    time,
                    duration,
                    timeA = 0f,
                    timeB = 0f,
                    timeABEnable = false,
                    visibleAB = false,
                    play = play,
                    onSeek = onSeek,
                    onSeekFinished = onSeekFinished,
                    modifier = Modifier
                        .padding(horizontal = TIMELINE_HORIZONTAL_PADDING)
                        .fillMaxWidth()
                        .offset(y = TIMELINE_OFFSET_Y),
                    isVisibleTime = true,
                    isVisibleStep = false,
                    isBuffering = isBuffering
                )
            }
        }

    }

}
