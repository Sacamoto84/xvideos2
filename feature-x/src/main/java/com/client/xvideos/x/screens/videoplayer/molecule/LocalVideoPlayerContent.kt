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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import com.client.xvideos.x.screens.videoplayer.model.SeekScrub
import com.client.xvideos.x.screens.videoplayer.model.rememberStoryboardImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File

/**
 * Основной компонент воспроизведения локального видеофайла.
 *
 * @param fileUrl URL локального файла (file://...).
 * @param resumePosition Позиция возобновления в секундах.
 * @param onSaveProgress Колбэк сохранения позиции воспроизведения.
 * @param onPopBack Колбэк возврата назад.
 * @param storyboard Сетка кадров ролика для превью при перемотке; `null` — её
 * ещё собирают или собрать не удалось, превью показывает только время.
 */
@OptIn(UnstableApi::class)
@Composable
fun LocalVideoPlayerContent(
    fileUrl: String,
    resumePosition: Float?,
    onSaveProgress: (currentTimeSeconds: Float, totalTimeSeconds: Int) -> Unit,
    onPopBack: () -> Unit,
    modifier: Modifier = Modifier,
    storyboard: File? = null,
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
            // Переключателя Fit/Fill здесь нет, а Crop срезал бы края кадра в портрете
            initialVideoFitMode = ScreenResize.FIT,
        )
    }

    // Палец ведёт ползунок: позиция под пальцем для превью кадра из сетки,
    // собранной из самого файла. Ролик перематывается один раз — при отпускании.
    var scrub by remember { mutableStateOf<SeekScrub?>(null) }
    val onScrub: (SeekScrub?) -> Unit = remember { { scrub = it } }
    val storyboardImage = rememberStoryboardImage(storyboard)

    // Авто-скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(resumeNoticeText) {
        if (resumeNoticeText != null) {
            delay(4000L)
            resumeNoticeText = null
        }
    }

    // Периодическое сохранение прогресса во время воспроизведения — только пока экран
    // запущен. `isPaused` — пауза с экрана, уход в фон её не ставит: раньше цикл и в
    // свёрнутом приложении каждые 3 с переписывал ту же запись истории на диск
    // (проход 204, T1). На уходе прогресс сохраняется один раз (`ON_STOP`).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(host.isPaused, lifecycleOwner) {
        if (!host.isPaused) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                onSaveProgress(host.currentTime, host.totalTime)
                while (isActive) {
                    delay(3000L)
                    onSaveProgress(host.currentTime, host.totalTime)
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        onSaveProgress(host.currentTime, host.totalTime)
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
            // Свайп по видео влево-вправо — ±5 с.
            swipeSeekSeconds = 5,
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

                // Превью кадра при перемотке — ниже панели, чтобы полоса была поверх.
                SeekPreviewOverlay(
                    storyboard = storyboardImage,
                    scrub = scrub,
                    durationSec = host.totalTime
                )

                Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                    // Локальный файл — отдельный полноэкранный режим не требуется
                    X_PlayerBottomBar(host = host, onScrub = onScrub)
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
