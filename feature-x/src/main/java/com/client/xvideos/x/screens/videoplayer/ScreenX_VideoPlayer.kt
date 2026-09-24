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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
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
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.videoplayer.atom.ComposeTags
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val ERROR_SPACER_HEIGHT = 12.dp
private val BUTTON_SPACER_WIDTH = 16.dp
private val BACK_BUTTON_PADDING = 8.dp
private val TAGS_START_PADDING = 56.dp
private val TAGS_END_PADDING = 12.dp
private val TAGS_TOP_PADDING = 8.dp
private val RESUME_PILL_BOTTOM_PADDING_FULLSCREEN = 68.dp
private val RESUME_PILL_BOTTOM_PADDING_PORTRAIT = 84.dp
private const val PROGRESS_SAVE_INTERVAL_MS = 3000L

private const val TEXT_LOAD_ERROR = "Не удалось загрузить видео"
private const val TEXT_RETRY = "Повторить"
private const val TEXT_BACK = "Назад"

class ScreenX_VideoPlayer(
    val url: String,
    val item: ItemsX? = null,
) : Screen {

    override val key: ScreenKey = "ScreenX_VideoPlayer:$url"

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenX_VideoPlayerSM, ScreenX_VideoPlayerSM.Factory> { factory ->
            factory.create(url, item)
        }

        OrientationAndSystemBarsEffect(vm.isFullScreen)

        val onRetryLoad: () -> Unit = remember(vm) { { vm.loadVideo(forceReload = true) } }
        val onPopBack: () -> Unit = remember(navigator) { { navigator.pop() } }

        // Нажатие кнопки «Назад» при ошибке или загрузке закрывает экран
        BackHandler(enabled = vm.isError || vm.isLoading || vm.passedHLS.isBlank(), onBack = onPopBack)

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

    // При полном уходе с экрана гарантированно возвращаем портретную ориентацию и скрытый статус-бар
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

@Composable
private fun VideoPlayerErrorView(onRetry: () -> Unit, onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(TEXT_LOAD_ERROR, color = Color.White)
            Spacer(modifier = Modifier.height(ERROR_SPACER_HEIGHT))
            Row {
                Button(onClick = onRetry) {
                    Text(TEXT_RETRY)
                }
                Spacer(modifier = Modifier.width(BUTTON_SPACER_WIDTH))
                Button(onClick = onBack) {
                    Text(TEXT_BACK)
                }
            }
        }
    }
}

@Composable
private fun VideoPlayerLoadingView(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .windowInsetsPadding(
                    WindowInsets.displayCutout.only(
                        WindowInsetsSides.Top + WindowInsetsSides.Start
                    )
                )
                .padding(BACK_BUTTON_PADDING),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = TEXT_BACK,
                tint = Color.White,
            )
        }
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.White)
        }
    }
}

@OptIn(UnstableApi::class)
@Suppress("LongMethod")
@Composable
private fun VideoPlayerContentView(
    vm: ScreenX_VideoPlayerSM,
    navigator: Navigator,
) {
    // Единый Compose-плеер (общий с R/L). Хост сам освобождает ExoPlayer
    // при выходе из композиции (RememberObserver).
    val host = remember(vm.passedHLS) {
        MediaPlayerHost(
            mediaUrl = vm.passedHLS,
            isMuted = true, // видео X всегда без звука
            isLooping = false,
            startTimeInSeconds = vm.resumePositionSeconds,
        ).apply {
            onError = {
                vm.onPlaybackError()
            }
        }
    }

    var areControlsVisible by remember { mutableStateOf(true) }
    var isZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }

    val onResetZoom: () -> Unit = remember { { resetZoomTrigger++ } }
    val onExitFullScreen: () -> Unit = remember(vm) { { vm.exitFullScreen() } }
    val onPopScreen: () -> Unit = remember(navigator) { { navigator.pop() } }

    // Иерархия «Назад»:
    // 1. При активном зуме сбрасывает масштаб до 1.0x (как в обычном, так и в ландшафтном режиме)
    // 2. В ландшафтном полноэкранном режиме возвращает в портретный режим
    // 3. Выходит из экрана плеера
    BackHandler(enabled = isZoomed, onBack = onResetZoom)
    BackHandler(enabled = !isZoomed && vm.isFullScreen, onBack = onExitFullScreen)
    BackHandler(enabled = !isZoomed && !vm.isFullScreen, onBack = onPopScreen)

    LaunchedEffect(vm.isFullScreen) {
        areControlsVisible = true
    }

    LaunchedEffect(vm.isFullScreen, areControlsVisible, host.isPaused) {
        if (vm.isFullScreen && areControlsVisible && !host.isPaused) {
            delay(3500)
            areControlsVisible = false
        }
    }

    // Авто-скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(vm.resumeNoticeText) {
        if (vm.resumeNoticeText != null) {
            delay(4000)
            vm.dismissResumeNotice()
        }
    }

    RememberHistoryProgressSync(vm = vm, host = host)

    val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
    val onTap: () -> Unit = remember(vm, host) {
        {
            if (vm.isFullScreen) {
                areControlsVisible = !areControlsVisible
            } else {
                host.togglePlayPause()
            }
        }
    }

    val onOverlayBack: () -> Unit = remember(isZoomed, navigator) {
        {
            if (isZoomed) {
                resetZoomTrigger++
            } else {
                navigator.pop()
            }
        }
    }
    val onTagClick: (String) -> Unit = remember(host, vm, navigator) {
        { tag ->
            host.pause()
            vm.openTag(tag, navigator)
        }
    }
    val onRestartPlayback: () -> Unit = remember(host, vm) {
        {
            host.seekTo(0f)
            vm.restartFromBeginning()
        }
    }
    val onToggleFullScreen: () -> Unit = remember(vm) {
        {
            vm.toggleFullScreen()
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
                // Кнопка возврата (только в обычном режиме; в полном экране используются системные жесты/кнопки Android)
                AnimatedVisibility(
                    visible = !vm.isFullScreen,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    IconButton(
                        onClick = onOverlayBack,
                        modifier = Modifier
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Start
                                )
                            )
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                }

                // Теги/каналы поверх видео (только в портретном режиме)
                if (!vm.isFullScreen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(
                                WindowInsets.displayCutout.only(
                                    WindowInsetsSides.Top + WindowInsetsSides.Start
                                )
                            )
                            .padding(start = TAGS_START_PADDING, end = TAGS_END_PADDING, top = TAGS_TOP_PADDING)
                    ) {
                        ComposeTags(
                            vm.tags,
                            onClick = onTagClick
                        )
                    }
                }

                // Всплывающее уведомление о возобновлении с кнопкой «С начала»
                AnimatedVisibility(
                    visible = vm.resumeNoticeText != null && (!vm.isFullScreen || areControlsVisible),
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = if (vm.isFullScreen) RESUME_PILL_BOTTOM_PADDING_FULLSCREEN else RESUME_PILL_BOTTOM_PADDING_PORTRAIT)
                ) {
                    vm.resumeNoticeText?.let { notice ->
                        ResumePlaybackPill(
                            text = notice,
                            onRestart = onRestartPlayback
                        )
                    }
                }

                // Панель управления снизу с автоскрытием в полноэкранном режиме
                AnimatedVisibility(
                    visible = !vm.isFullScreen || areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    X_PlayerBottomBar(
                        host = host,
                        isFullScreen = vm.isFullScreen,
                        onFullScreen = onToggleFullScreen
                    )
                }
            }
        )
    }
}

@Composable
private fun RememberHistoryProgressSync(
    vm: ScreenX_VideoPlayerSM,
    host: MediaPlayerHost,
) {
    // Периодическое сохранение прогресса во время активного воспроизведения
    LaunchedEffect(host.isPaused) {
        if (!host.isPaused) {
            vm.saveProgress(host.currentTime, host.totalTime)
            while (isActive) {
                delay(PROGRESS_SAVE_INTERVAL_MS)
                vm.saveProgress(host.currentTime, host.totalTime)
            }
        }
    }

    // Финальное сохранение текущей позиции при закрытии экрана
    DisposableEffect(Unit) {
        onDispose {
            vm.saveProgress(host.currentTime, host.totalTime)
        }
    }
}
