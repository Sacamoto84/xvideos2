package com.client.xvideos.common.videoplayer.local.molecule

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.util.UnstableApi
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.common.videoplayer.ui.component.PlayPauseFeedbackState
import com.client.xvideos.common.videoplayer.local.model.LocalVideoPlayerOverlayActions
import com.client.xvideos.common.videoplayer.local.model.SeekScrub
import com.client.xvideos.common.videoplayer.local.model.rememberStoryboardImage
import com.client.xvideos.common.videoplayer.local.model.shouldAutoHideControls
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
 * @param isFullScreen На весь экран (альбомный режим) — как у плеера ролика из сети.
 * @param title Название ролика для верхней панели на весь экран.
 * @param onToggleFullScreen Кнопка полного экрана в нижней панели.
 * @param onExitFullScreen Выход из полного экрана («назад» сверху и системный «Назад»).
 * @param muted Звук выключен. Ролики X всегда без звука; у записи эфира звук включает кнопка.
 * @param onToggleMute Кнопка звука в нижней панели; `null` — кнопки нет.
 * @param enablePlayPauseFeedback Показывает значок при переключении воспроизведения в обоих режимах экрана.
 */
@OptIn(UnstableApi::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
fun LocalVideoPlayerContent(
    fileUrl: String,
    resumePosition: Float?,
    onSaveProgress: (currentTimeSeconds: Float, totalTimeSeconds: Int) -> Unit,
    onPopBack: () -> Unit,
    modifier: Modifier = Modifier,
    storyboard: File? = null,
    isFullScreen: Boolean = false,
    title: String = "",
    onToggleFullScreen: () -> Unit = {},
    onExitFullScreen: () -> Unit = {},
    muted: Boolean = true,
    onToggleMute: (() -> Unit)? = null,
    showResumeNotice: Boolean = true,
    enablePlayPauseFeedback: Boolean = false,
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

    var resumeNoticeText by remember(fileUrl, showResumeNotice) {
        mutableStateOf(buildResumeNoticeText(showResumeNotice, resumePosition))
    }

    val currentOnSaveProgress by rememberUpdatedState(onSaveProgress)
    val currentOnPopBack by rememberUpdatedState(onPopBack)
    val currentOnToggleFullScreen by rememberUpdatedState(onToggleFullScreen)
    val currentOnExitFullScreen by rememberUpdatedState(onExitFullScreen)
    val currentOnToggleMute by rememberUpdatedState(onToggleMute)
    val currentIsFullScreen by rememberUpdatedState(isFullScreen)
    val currentEnablePlayPauseFeedback by rememberUpdatedState(enablePlayPauseFeedback)

    val host = remember(fileUrl) {
        MediaPlayerHost(
            mediaUrl = fileUrl,
            isMuted = muted, // видео X всегда без звука; запись эфира — по кнопке
            isLooping = false,
            startTimeInSeconds = resumePosition,
            // В портрете Crop срезал бы края кадра; на весь экран — Fill (ниже).
            initialVideoFitMode = ScreenResize.FIT,
        )
    }

    // Хост создаётся по адресу, а кнопка звука меняет только громкость.
    LaunchedEffect(host, muted) {
        if (muted) host.mute() else host.unmute()
    }

    // Видимость панелей: на весь экран они прячутся по таймеру и возвращаются тапом.
    var areControlsVisible by remember { mutableStateOf(true) }

    // Палец ведёт ползунок: позиция под пальцем для превью кадра из сетки,
    // собранной из самого файла. Ролик перематывается один раз — при отпускании.
    // Захват ползунка возвращает панель, чтобы она не дотухла под пальцем.
    var scrub by remember { mutableStateOf<SeekScrub?>(null) }
    var isSpeedMenuOpen by remember { mutableStateOf(false) }
    val onScrub: (SeekScrub?) -> Unit = remember {
        { value ->
            scrub = value
            if (value != null) areControlsVisible = true
        }
    }
    val onSpeedMenuExpandedChange: (Boolean) -> Unit = remember {
        { expanded ->
            isSpeedMenuOpen = expanded
            if (expanded) areControlsVisible = true
        }
    }
    val scrubProvider: () -> SeekScrub? = remember { { scrub } }
    val storyboardImage = rememberStoryboardImage(storyboard)

    // При смене режима панели снова видны.
    LaunchedEffect(isFullScreen) {
        areControlsVisible = true
    }

    // Портрет — Fit, на весь экран — Fill; его переключает FitModeToggle в панели.
    LaunchedEffect(host, isFullScreen) {
        host.videoFitMode = if (isFullScreen) ScreenResize.FILL else ScreenResize.FIT
    }

    // Автоскрытие панелей через 3,5 с на весь экран, пока ролик играет и меню скорости/ползунок не открыты.
    val isScrubbing by remember { derivedStateOf { scrub != null } }
    LaunchedEffect(isFullScreen, areControlsVisible, host.isPaused, isSpeedMenuOpen, isScrubbing) {
        if (shouldAutoHideControls(isFullScreen, areControlsVisible, host.isPaused, isSpeedMenuOpen, isScrubbing)) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Авто-скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(resumeNoticeText) {
        if (resumeNoticeText != null) {
            delay(4000L)
            resumeNoticeText = null
        }
    }

    // Периодическое сохранение прогресса во время воспроизведения — только пока экран
    // запущен и ролик подготовлен (не перетираем сохранённую позицию нулём до старта seek).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(host, host.isPaused, lifecycleOwner) {
        if (!host.isPaused) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (isActive) {
                    delay(3000L)
                    saveHostProgressIfReady(host, currentOnSaveProgress)
                }
            }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        saveHostProgressIfReady(host, currentOnSaveProgress)
    }

    // Финальное сохранение при закрытии экрана
    DisposableEffect(host) {
        onDispose {
            saveHostProgressIfReady(host, currentOnSaveProgress)
        }
    }

    var isZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }

    // «Назад»: зум сбрасывает масштаб, затем выход из полного экрана, затем из плеера.
    BackHandler(enabled = isZoomed, onBack = onResetZoom)
    BackHandler(enabled = !isZoomed && isFullScreen, onBack = currentOnExitFullScreen)
    BackHandler(enabled = !isZoomed && !isFullScreen, onBack = currentOnPopBack)

    val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
    val playbackFeedback = remember(host, enablePlayPauseFeedback) {
        PlayPauseFeedbackState(host, enablePlayPauseFeedback)
    }
    val onTap: () -> Unit = remember(playbackFeedback) {
        {
            if (currentIsFullScreen && !currentEnablePlayPauseFeedback) {
                areControlsVisible = !areControlsVisible
            } else {
                playbackFeedback.togglePlayback()
                if (currentIsFullScreen) areControlsVisible = true
            }
        }
    }
    val onRestartPlayback: () -> Unit = remember(host) {
        {
            host.seekTo(0f)
            resumeNoticeText = null
        }
    }

    val overlayActions = remember(
        onRestartPlayback,
        onScrub,
        onSpeedMenuExpandedChange,
    ) {
        LocalVideoPlayerOverlayActions(
            onExitFullScreen = { currentOnExitFullScreen() },
            onRestartPlayback = onRestartPlayback,
            onToggleFullScreen = { currentOnToggleFullScreen() },
            onScrub = onScrub,
            onToggleMute = currentOnToggleMute?.let { { currentOnToggleMute?.invoke() } },
            onSpeedMenuExpandedChange = onSpeedMenuExpandedChange,
        )
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
                LocalVideoPlayerOverlay(
                    host = host,
                    playbackFeedback = playbackFeedback,
                    isFullScreen = isFullScreen,
                    areControlsVisible = areControlsVisible,
                    title = title,
                    resumeNoticeText = resumeNoticeText,
                    storyboard = storyboardImage,
                    scrubProvider = scrubProvider,
                    muted = muted,
                    actions = overlayActions,
                )
            }
        )
    }
}

private fun buildResumeNoticeText(showResumeNotice: Boolean, resumePosition: Float?): String? {
    if (!showResumeNotice) return null
    val sec = resumePosition ?: return null
    return "Возобновлено с ${formatTime(sec.toInt())}"
}

private fun saveHostProgressIfReady(
    host: MediaPlayerHost,
    onSaveProgress: (Float, Int) -> Unit,
) {
    if (host.totalTime > 0 || host.currentTime > 0f) {
        onSaveProgress(host.currentTime, host.totalTime)
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
