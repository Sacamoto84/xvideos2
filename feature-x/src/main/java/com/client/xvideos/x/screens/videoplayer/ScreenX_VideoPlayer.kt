package com.client.xvideos.x.screens.videoplayer

import android.content.pm.ActivityInfo
import com.client.xvideos.common.util.findActivity
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.util.UnstableApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.ui.theme.XvideosTheme
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.model.TagsMainUploaderPornstar
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.screens.videoplayer.atom.ComposeTags
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu

// Отступы для корректного отображения контента с учётом выреза под камеру (cutout) сверху и слева
private val CutoutTopStartInsets: WindowInsets
    @Composable get() = WindowInsets.displayCutout.only(
        WindowInsetsSides.Top + WindowInsetsSides.Start
    )

/**
 * Экран видеоплеера (Voyager Screen).
 * Управляет жизненным циклом ScreenModel, системной ориентацией экрана
 * и переключением между состояниями: ошибка, загрузка и контент плеера.
 *
 * @param url URL страницы или видеопотока.
 * @param item Опциональные метаданные видео (ItemsX).
 */
class ScreenX_VideoPlayer(
    val url: String,
    val item: ItemsX? = null,
) : Screen {

    override val key: ScreenKey = "ScreenX_VideoPlayer:$url"

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        // Инициализация ScreenModel с фабрикой параметров
        val vm = getScreenModel<ScreenX_VideoPlayerSM, ScreenX_VideoPlayerSM.Factory> { factory ->
            factory.create(url, item)
        }

        // Управление ориентацией устройства и системными панелями (status bar, navigation bar)
        OrientationAndSystemBarsEffect(vm.isFullScreen)

        val onRetryLoad: () -> Unit = remember(vm) { { vm.loadVideo(forceReload = true) } }
        val onPopBack: () -> Unit = remember(navigator) { { navigator.pop() } }

        // Нажатие кнопки «Назад» при ошибке или загрузке закрывает экран
        BackHandler(enabled = vm.isError || vm.isLoading || vm.passedHLS.isBlank(), onBack = onPopBack)

        // Отображение соответствующего UI в зависимости от состояния ScreenModel
        when {
            vm.isError -> {
                VideoPlayerErrorView(
                    onRetry = onRetryLoad,
                    onBack = onPopBack
                )
            }
            vm.isLoading || vm.passedHLS.isBlank() -> {
                VideoPlayerLoadingView(onBack = onPopBack)
            }
            else -> {
                VideoPlayerContentView(vm = vm, navigator = navigator)
            }
        }
    }
}

/**
 * Управляет ориентацией экрана (альбомная/портретная) и отображением системных панелей.
 * При входе в полноэкранный режим скрывает статус-бар и навигационную панель,
 * а при выходе или закрытии экрана возвращает стандартные настройки.
 */
@Composable
private fun OrientationAndSystemBarsEffect(isFullScreen: Boolean) {
    val context = LocalContext.current

    // Альбомная ориентация + скрытие системных баров на время полноэкранного режима
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        val window = activity?.window
        val prevOrientation = activity?.requestedOrientation
        if (isFullScreen) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                // Скрытые бары временно появляются по свайпу от края экрана
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.navigationBars())
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        } else {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
        onDispose {
            if (isFullScreen) {
                activity?.requestedOrientation =
                    prevOrientation ?: ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                window?.let {
                    val controller = WindowCompat.getInsetsController(it, it.decorView)
                    controller.show(WindowInsetsCompat.Type.navigationBars())
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
    }

    // При полном уходе с экрана гарантированно возвращаем портретную ориентацию и восстанавливаем навигацию
    DisposableEffect(Unit) {
        onDispose {
            val activity = context.findActivity()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            activity?.window?.let {
                val controller = WindowCompat.getInsetsController(it, it.decorView)
                controller.show(WindowInsetsCompat.Type.navigationBars())
                controller.hide(WindowInsetsCompat.Type.statusBars())
            }
        }
    }
}

/**
 * Экран ошибки загрузки или воспроизведения видео с кнопками повтора и выхода назад.
 */
@Composable
private fun VideoPlayerErrorView(onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Не удалось загрузить видео", color = Color.White)
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Button(onClick = onRetry) {
                    Text("Повторить")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onBack) {
                    Text("Назад")
                }
            }
        }
    }
}

/**
 * Индикатор ожидания при первичной загрузке страницы/потока видео.
 */
@Composable
private fun VideoPlayerLoadingView(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

/**
 * Stateful-обёртка над контентом плеера.
 * Извлекает данные из [ScreenX_VideoPlayerSM] и связывает события UI с методами ScreenModel и Navigator.
 */
@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerContentView(
    vm: ScreenX_VideoPlayerSM,
    navigator: Navigator,
) {
    VideoPlayerContentView(
        passedHLS = vm.passedHLS,
        videoTitle = vm.currentItem.title,
        isFavorite = vm.isFavorite,
        onFavoriteAdd = { vm.addFavorite() },
        onFavoriteRemove = { vm.removeFavorite() },
        onDownload = { vm.download() },
        onSaveToGallery = { vm.saveToGallery() },
        resumePositionSeconds = vm.resumePositionSeconds,
        isFullScreen = vm.isFullScreen,
        resumeNoticeText = vm.resumeNoticeText,
        tags = vm.tags,
        onPlaybackError = { vm.onPlaybackError() },
        onExitFullScreen = { vm.exitFullScreen() },
        onPopBack = { navigator.pop() },
        onDismissResumeNotice = { vm.dismissResumeNotice() },
        onTagClick = { tag -> vm.openTag(tag, navigator) },
        onChannelClick = { channel -> vm.openChannel(channel, navigator) },
        onPornstarClick = { pornstar -> vm.openPornstar(pornstar, navigator) },
        onRestartFromBeginning = { vm.restartFromBeginning() },
        onToggleFullScreen = { vm.toggleFullScreen() },
        onSaveProgress = { positionSeconds, durationSeconds ->
            vm.saveProgress(positionSeconds, durationSeconds)
        },
    )
}

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
 */
@OptIn(UnstableApi::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
private fun VideoPlayerContentView(
    passedHLS: String,
    videoTitle: String = "",
    isFavorite: Boolean = false,
    onFavoriteAdd: () -> Unit = {},
    onFavoriteRemove: () -> Unit = {},
    onDownload: () -> Unit = {},
    onSaveToGallery: () -> Unit = {},
    resumePositionSeconds: Float?,
    isFullScreen: Boolean,
    resumeNoticeText: String?,
    tags: TagsModel,
    onPlaybackError: () -> Unit,
    onExitFullScreen: () -> Unit,
    onPopBack: () -> Unit,
    onDismissResumeNotice: () -> Unit,
    onTagClick: (String) -> Unit,
    onChannelClick: (TagsMainUploaderPornstar) -> Unit = {},
    onPornstarClick: (TagsMainUploaderPornstar) -> Unit = {},
    onRestartFromBeginning: () -> Unit,
    onToggleFullScreen: () -> Unit,
    onSaveProgress: (positionSeconds: Float, durationSeconds: Int) -> Unit,
) {
    // Режим предпросмотра в IDE (Android Studio @Preview или Layout Inspector).
    // Реальный ExoPlayer (MediaPlayerHost) и системные декодеры не могут инициализироваться в JVM среды IDE.
    // Если активен режим инспекции, отрисовываем легковесный статический макет-заглушку и выходим из функции.
    if (LocalInspectionMode.current) {

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
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
                        tags,
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

    // Автоматическое скрытие контроллеров через 3.5 секунды неактивности при воспроизведении на полном экране (если меню не открыто)
    LaunchedEffect(isFullScreen, areControlsVisible, host.isPaused, isMenuExpanded) {
        if (isFullScreen && areControlsVisible && !host.isPaused && !isMenuExpanded) {
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

    // Синхронизация сохранённой позиции воспроизведения в историю
    RememberHistoryProgressSync(onSaveProgress = onSaveProgress, host = host)

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
        ComposeVideoPlayer(
            playerHost = host,
            modifier = Modifier.fillMaxSize(),
            resetZoomTrigger = resetZoomTrigger,
            onZoomChanged = onZoomChanged,
            onTap = onTap,
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
                                tags,
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

                // Панель управления снизу с автоскрытием в полноэкранном режиме
                AnimatedVisibility(
                    visible = !isFullScreen || areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    X_PlayerBottomBar(
                        host = host,
                        isFullScreen = isFullScreen,
                        onFullScreen = onToggleFullScreen
                    )
                }
            }
        )
    }
}

/**
 * Верхняя панель управления видеоплеером в полноэкранном режиме.
 */
@Composable
private fun PlayerFullScreenTopBar(
    title: String,
    isFavorite: Boolean,
    onFavoriteAdd: () -> Unit,
    onFavoriteRemove: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xCC000000), Color.Transparent)
                )
            )
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Выйти из полного экрана",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (title.isNotBlank()) {
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color(0x66000000))
        ) {
            X_DashboardExpandMenu(
                isFavorite = isFavorite,
                onFavoriteAdd = onFavoriteAdd,
                onFavoriteRemove = onFavoriteRemove,
                onDownload = onDownload,
                onSaveToGallery = onSaveToGallery,
                onExpandedChange = onExpandedChange,
            )
        }
    }
}

/**
 * Фоновая синхронизация прогресса воспроизведения видео.
 * - Периодически (каждые 3 секунды) передает текущую позицию в [onSaveProgress].
 * - При выходе из композиции (закрытии экрана) гарантированно фиксирует финальную позицию.
 */
@Composable
private fun RememberHistoryProgressSync(
    onSaveProgress: (positionSeconds: Float, durationSeconds: Int) -> Unit,
    host: MediaPlayerHost,
) {
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

    // Финальное сохранение текущей позиции при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            onSaveProgress(host.currentTime, host.totalTime)
        }
    }
}

/**
 * Предпросмотр верстки плеера для вкладки Design / Preview в Android Studio.
 * Благодаря проверке [LocalInspectionMode] безопасно рендерится без инициализации нативного ExoPlayer.
 */
@Preview(showBackground = true, backgroundColor = 0xFF040404)
@Composable
private fun VideoPlayerContentViewPreview() {
    XvideosTheme {
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
}
