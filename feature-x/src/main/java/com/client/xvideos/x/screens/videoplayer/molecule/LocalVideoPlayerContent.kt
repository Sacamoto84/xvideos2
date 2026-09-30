package com.client.xvideos.x.screens.videoplayer.molecule

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * Основной компонент воспроизведения локального видеофайла.
 *
 * @param fileUrl URL локального файла (file://...).
 * @param resumePosition Позиция возобновления в секундах.
 * @param onSaveProgress Колбэк сохранения позиции воспроизведения.
 * @param onPopBack Колбэк возврата назад.
 */
@OptIn(UnstableApi::class)
@Composable
fun LocalVideoPlayerContent(
    fileUrl: String,
    resumePosition: Float?,
    onSaveProgress: (currentTimeSeconds: Float, totalTimeSeconds: Int) -> Unit,
    onPopBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.fillMaxSize().background(Color(0xFF040404)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "Local Video Preview", color = Color.White)
        }
        return
    }

    var resumeNoticeText by remember(fileUrl) {
        mutableStateOf(
            resumePosition?.let { sec ->
                "Возобновлено с ${formatTime(sec.toInt())}"
            }
        )
    }

    val host = remember(fileUrl) {
        MediaPlayerHost(
            mediaUrl = fileUrl,
            isMuted = true, // видео X всегда без звука
            isLooping = false,
            startTimeInSeconds = resumePosition,
        )
    }

    // Авто-скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(resumeNoticeText) {
        if (resumeNoticeText != null) {
            delay(4000L)
            resumeNoticeText = null
        }
    }

    // Периодическое сохранение прогресса во время активного воспроизведения
    LaunchedEffect(host.isPaused) {
        if (!host.isPaused) {
            onSaveProgress(host.currentTime, host.totalTime)
            while (isActive) {
                delay(3000L)
                onSaveProgress(host.currentTime, host.totalTime)
            }
        }
    }

    // Финальное сохранение при закрытии экрана
    DisposableEffect(host) {
        onDispose {
            onSaveProgress(host.currentTime, host.totalTime)
        }
    }

    var isZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }

    // Нажатие кнопки «Назад» при зуме сбрасывает масштаб, иначе выходит из плеера
    BackHandler(enabled = isZoomed, onBack = onResetZoom)
    BackHandler(enabled = !isZoomed, onBack = onPopBack)

    val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
    val onTap: () -> Unit = remember(host) { { host.togglePlayPause() } }
    val onRestartPlayback: () -> Unit = remember(host) {
        {
            host.seekTo(0f)
            resumeNoticeText = null
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF040404))) {
        ComposeVideoPlayer(
            playerHost = host,
            modifier = Modifier.fillMaxSize(),
            resetZoomTrigger = resetZoomTrigger,
            onZoomChanged = onZoomChanged,
            onTap = onTap,
            overlay = {
                // Плашка возобновления
                AnimatedVisibility(
                    visible = resumeNoticeText != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 76.dp)
                ) {
                    resumeNoticeText?.let { notice ->
                        ResumePlaybackPill(
                            text = notice,
                            onRestart = onRestartPlayback
                        )
                    }
                }

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    // Локальный файл — отдельный полноэкранный режим не требуется
                    X_PlayerBottomBar(host = host)
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun LocalVideoPlayerContentPreview() {
    LocalVideoPlayerContent(
        fileUrl = "file:///sample.mp4",
        resumePosition = 45f,
        onSaveProgress = { _, _ -> },
        onPopBack = {}
    )
}
