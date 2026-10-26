package com.client.xvideos.x.screens.videoplayer.molecule

import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
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
import com.client.xvideos.common.videoplayer.local.atom.ResumePlaybackPill
import com.client.xvideos.common.videoplayer.local.model.SeekScrub
import com.client.xvideos.common.videoplayer.local.model.rememberStoryboardImage
import com.client.xvideos.common.videoplayer.local.model.shouldAutoHideControls
import com.client.xvideos.common.videoplayer.local.molecule.PlayerBottomBar
import com.client.xvideos.common.videoplayer.local.molecule.SeekPreviewOverlay
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

// Отступы для корректного отображения контента с учётом выреза под камеру (cutout) сверху и слева
private val CutoutTopStartInsets: WindowInsets
    @Composable get() = WindowInsets.displayCutout.only(
        WindowInsetsSides.Top + WindowInsetsSides.Start
    )

/**
 * Основной компонент воспроизведения видео (Stateless).
 * Содержит плеер [ComposeVideoPlayer], оверлеи тегов, уведомления и нижнюю панель управления.
 *
 * @param passedHLS HLS URL видеопотока (.m3u8).
 * @param resumePositionSeconds Позиция возобновления в секундах (из истории), либо null.
 * @param isFullScreen Флаг отображения на весь экран (альбомная ориентация).
 * @param resumeNoticeText Текст плашки о возобновлении просмотра.
 * @param tags Список тегов видео.
 * @param onPlaybackError Обработчик ошибки плеера.
 * @param onExitFullScreen Выход из полноэкранного режима.
 * @param onPopBack Возврат на предыдущий экран.
 * @param onDismissResumeNotice Закрытие плашки о возобновлении.
 * @param onTagClick Нажатие на тег.
 * @param onChannelClick Нажатие на канал.
 * @param onPornstarClick Нажатие на порнозвезду/модель.
 * @param onRestartFromBeginning Перезапуск видео с начала.
 * @param onToggleFullScreen Переключение полноэкранного режима.
 * @param onSaveProgress Периодическое сохранение прогресса просмотра в историю.
 * @param storyboardUrl Раскадровка ролика (`thumbSlideBig`) для превью кадра при перемотке.
 */
@OptIn(UnstableApi::class)
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod")
@Composable
fun VideoPlayerContentView(
    passedHLS: String,
    resumePositionSeconds: Float?,
    isFullScreen: Boolean,
    resumeNoticeText: String?,
    tags: TagsModel,
    onPlaybackError: () -> Unit,
    onExitFullScreen: () -> Unit,
    onPopBack: () -> Unit,
    onDismissResumeNotice: () -> Unit,
    onTagClick: (String) -> Unit,
    onRestartFromBeginning: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onSaveProgress: (positionSeconds: Float, durationSeconds: Int) -> Unit,
    modifier: Modifier = Modifier,
    videoTitle: String = "",
    isFavorite: Boolean = false,
    onFavoriteAdd: () -> Unit = {},
    onFavoriteRemove: () -> Unit = {},
    onDownload: () -> Unit = {},
    onSaveToGallery: () -> Unit = {},
    onChannelClick: (TagsMainUploaderPornstar) -> Unit = {},
    onPornstarClick: (TagsMainUploaderPornstar) -> Unit = {},
    storyboardUrl: String? = null,
) {
    // Режим предпросмотра в IDE (Android Studio @Preview или Layout Inspector).
    // Реальный ExoPlayer (MediaPlayerHost) и системные декодеры не могут инициализироваться в JVM среды IDE.
    // Если активен режим инспекции, отрисовываем легковесный статический макет-заглушку и выходим из функции.
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF040404))) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Video Preview", color = Color.White)
            }

            // Теги для предпросмотра расположения верстки
            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(CutoutTopStartInsets)
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                        .fillMaxWidth()
                ) {
                    ComposeTags(
                        tags = tags,
                        onClick = onTagClick,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Плашка возобновления для предпросмотра
            if (resumeNoticeText != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isFullScreen) 68.dp else 84.dp)
                ) {
                    ResumePlaybackPill(
                        text = resumeNoticeText,
                        onRestart = onRestartFromBeginning
                    )
                }
            }
        }
        return
    }

    // Единый Compose-плеер (общий с R/L). Хост сам освобождает ExoPlayer
    // при выходе из композиции (RememberObserver).
    val host = remember(passedHLS) {
        MediaPlayerHost(
            mediaUrl = passedHLS,
            isMuted = true, // видео X всегда без звука
            isLooping = false,
            startTimeInSeconds = resumePositionSeconds,
        ).apply {
            onError = {
                onPlaybackError()
            }
        }
    }

    // Состояния видимости элементов управления и масштабирования (pinch-to-zoom)
    var areControlsVisible by remember { mutableStateOf(true) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    // Палец ведёт ползунок: позиция под пальцем для превью кадра. Раскадровка
    // грузится вместе с роликом, чтобы при первом касании кадр уже был.
    var scrub by remember { mutableStateOf<SeekScrub?>(null) }
    // Захват ползунка возвращает панель: если таймер автоскрытия сработал в тот же
    // кадр, панель не дотухнет и не уйдёт из композиции под пальцем.
    val onScrub: (SeekScrub?) -> Unit = remember {
        { value ->
            scrub = value
            if (value != null) areControlsVisible = true
        }
    }
    val storyboard = rememberStoryboardImage(storyboardUrl)

    val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }

    // Иерархия «Назад»:
    // 1. При активном зуме сбрасывает масштаб до 1.0x (как в обычном, так и в ландшафтном режиме)
    // 2. В ландшафтном полноэкранном режиме возвращает в портретный режим
    // 3. Выходит из экрана плеера
    BackHandler(enabled = isZoomed, onBack = onResetZoom)
    BackHandler(enabled = !isZoomed && isFullScreen, onBack = onExitFullScreen)
    BackHandler(enabled = !isZoomed && !isFullScreen, onBack = onPopBack)

    // При смене режима экрана (полноэкранный/обычный) сбрасываем видимость контроллеров в активное состояние
    LaunchedEffect(isFullScreen) {
        areControlsVisible = true
    }

    // Портрет — Fit: окно выше кадра, Crop растянул бы видео по высоте и срезал края.
    // Полноэкранный — Fill, его переключает FitModeToggle (в портрете переключателя нет).
    LaunchedEffect(host, isFullScreen) {
        host.videoFitMode = if (isFullScreen) ScreenResize.FILL else ScreenResize.FIT
    }

    // Автоматическое скрытие контроллеров через 3.5 секунды неактивности при воспроизведении на полном экране
    // (если меню не открыто и палец не ведёт ползунок)
    val isScrubbing = scrub != null
    LaunchedEffect(isFullScreen, areControlsVisible, host.isPaused, isMenuExpanded, isScrubbing) {
        if (shouldAutoHideControls(isFullScreen, areControlsVisible, host.isPaused, isMenuExpanded, isScrubbing)) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Авто-скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(resumeNoticeText) {
        if (resumeNoticeText != null) {
            delay(4000)
            onDismissResumeNotice()
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

    // Финальное сохранение текущей позиции при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            onSaveProgress(host.currentTime, host.totalTime)
        }
    }

    val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }

    // Обработка одиночного тапа по видео:
    // - В полноэкранном режиме: показать / скрыть контроллеры
    // - В портретном режиме: переключить воспроизведение / паузу
    val onTap: () -> Unit = remember(isFullScreen, host) {
        {
            if (isFullScreen) {
                areControlsVisible = !areControlsVisible
            } else {
                host.togglePlayPause()
            }
        }
    }

    // Пауза перед переходом по тегу, чтобы видео не продолжало проигрываться
    val handleTagClick: (String) -> Unit = remember(host) {
        { tag ->
            host.pause()
            onTagClick(tag)
        }
    }

    // Пауза перед переходом в канал автора
    val handleChannelClick: (TagsMainUploaderPornstar) -> Unit = remember(host, onChannelClick) {
        { channel ->
            host.pause()
            onChannelClick(channel)
        }
    }

    // Пауза перед переходом на страницу модели
    val handlePornstarClick: (TagsMainUploaderPornstar) -> Unit = remember(host, onPornstarClick) {
        { pornstar ->
            host.pause()
            onPornstarClick(pornstar)
        }
    }

    // Перемотка в начало (0 секунд) и сброс уведомления о возобновлении
    val onRestartPlayback: () -> Unit = remember(host) {
        {
            host.seekTo(0f)
            onRestartFromBeginning()
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
                // Верхняя панель:
                // В полноэкранном режиме: Back, Название ролика, Кнопка 3-точки с выпадающим меню.
                // В портретном режиме: теги/каналы слева и кнопка 3-точки справа.
                if (isFullScreen) {
                    AnimatedVisibility(
                        visible = areControlsVisible,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                    ) {
                        PlayerFullScreenTopBar(
                            title = videoTitle,
                            isFavorite = isFavorite,
                            onFavoriteAdd = onFavoriteAdd,
                            onFavoriteRemove = onFavoriteRemove,
                            onDownload = onDownload,
                            onSaveToGallery = onSaveToGallery,
                            onExpandedChange = { isMenuExpanded = it },
                            onBack = onExitFullScreen,
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(CutoutTopStartInsets)
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(modifier = Modifier.weight(1f, fill = false)) {
                            ComposeTags(
                                tags = tags,
                                onChannelClick = handleChannelClick,
                                onPornstarClick = handlePornstarClick,
                                onClick = handleTagClick,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp, top = 2.dp)
                                .clip(CircleShape)
                                .background(Color(0x80000000))
                        ) {
                            X_DashboardExpandMenu(
                                isFavorite = isFavorite,
                                onFavoriteAdd = onFavoriteAdd,
                                onFavoriteRemove = onFavoriteRemove,
                                onDownload = onDownload,
                                onSaveToGallery = onSaveToGallery,
                                onExpandedChange = { isMenuExpanded = it },
                            )
                        }
                    }
                }

                // Всплывающее уведомление о возобновлении с кнопкой «С начала»
                AnimatedVisibility(
                    visible = resumeNoticeText != null && (!isFullScreen || areControlsVisible),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (isFullScreen) 68.dp else 84.dp)
                ) {
                    resumeNoticeText?.let { notice ->
                        ResumePlaybackPill(
                            text = notice,
                            onRestart = onRestartPlayback
                        )
                    }
                }

                // Превью кадра при перемотке — и в портрете, и на весь экран (решение
                // владельца от 21.10.2026); ниже нижней панели, чтобы полоса была поверх.
                SeekPreviewOverlay(
                    storyboard = storyboard,
                    scrub = scrub,
                    durationSec = host.totalTime
                )

                // Панель управления снизу с автоскрытием в полноэкранном режиме.
                // Видео перематывается только при отпускании пальца: куда попадёшь,
                // показывает превью.
                AnimatedVisibility(
                    visible = !isFullScreen || areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    PlayerBottomBar(
                        host = host,
                        isFullScreen = isFullScreen,
                        onFullScreen = onToggleFullScreen,
                        onScrub = onScrub
                    )
                }
            }
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun VideoPlayerContentViewPreview() {
    VideoPlayerContentView(
        passedHLS = "https://example.com/video.m3u8",
        resumePositionSeconds = 120f,
        isFullScreen = false,
        resumeNoticeText = "Возобновлено с 02:00",
        tags = TagsModel(
            tags = listOf("sample_tag_1", "sample_tag_2")
        ),
        onPlaybackError = {},
        onExitFullScreen = {},
        onPopBack = {},
        onDismissResumeNotice = {},
        onTagClick = {},
        onRestartFromBeginning = {},
        onToggleFullScreen = {},
        onSaveProgress = { _, _ -> },
    )
}
