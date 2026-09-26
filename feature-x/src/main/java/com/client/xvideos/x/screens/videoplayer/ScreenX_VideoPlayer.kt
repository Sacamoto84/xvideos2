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
import com.client.xvideos.x.model.TagsModel
import com.client.xvideos.x.screens.videoplayer.atom.ComposeTags
import com.client.xvideos.x.screens.videoplayer.atom.ResumePlaybackPill
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val CutoutTopStartInsets: WindowInsets
    @Composable get() = WindowInsets.displayCutout.only(
        WindowInsetsSides.Top + WindowInsetsSides.Start
    )

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

@Composable
private fun VideoPlayerLoadingView(onBack: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = Color.White)
    }
}

@OptIn(UnstableApi::class)
@Composable
private fun VideoPlayerContentView(
    vm: ScreenX_VideoPlayerSM,
    navigator: Navigator,
) {
    VideoPlayerContentView(
        passedHLS = vm.passedHLS,
        resumePositionSeconds = vm.resumePositionSeconds,
        isFullScreen = vm.isFullScreen,
        resumeNoticeText = vm.resumeNoticeText,
        tags = vm.tags,
        onPlaybackError = { vm.onPlaybackError() },
        onExitFullScreen = { vm.exitFullScreen() },
        onPopBack = { navigator.pop() },
        onDismissResumeNotice = { vm.dismissResumeNotice() },
        onTagClick = { tag -> vm.openTag(tag, navigator) },
        onRestartFromBeginning = { vm.restartFromBeginning() },
        onToggleFullScreen = { vm.toggleFullScreen() },
        onSaveProgress = { positionSeconds, durationSeconds ->
            vm.saveProgress(positionSeconds, durationSeconds)
        },
    )
}

@OptIn(UnstableApi::class)
@Suppress("LongMethod", "LongParameterList")
@Composable
private fun VideoPlayerContentView(
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
) {
    if (LocalInspectionMode.current) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Video Preview", color = Color.White)
            }

            if (!isFullScreen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .windowInsetsPadding(CutoutTopStartInsets)
                        .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                ) {
                    ComposeTags(
                        tags,
                        onClick = onTagClick
                    )
                }
            }

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

    var areControlsVisible by remember { mutableStateOf(true) }
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

    LaunchedEffect(isFullScreen) {
        areControlsVisible = true
    }

    LaunchedEffect(isFullScreen, areControlsVisible, host.isPaused) {
        if (isFullScreen && areControlsVisible && !host.isPaused) {
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

    RememberHistoryProgressSync(onSaveProgress = onSaveProgress, host = host)

    val onZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
    val onTap: () -> Unit = remember(isFullScreen, host) {
        {
            if (isFullScreen) {
                areControlsVisible = !areControlsVisible
            } else {
                host.togglePlayPause()
            }
        }
    }

    val handleTagClick: (String) -> Unit = remember(host) {
        { tag ->
            host.pause()
            onTagClick(tag)
        }
    }
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

                // Теги/каналы поверх видео (только в портретном режиме)
                if (!isFullScreen) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .windowInsetsPadding(CutoutTopStartInsets)
                            .padding(start = 4.dp, end = 4.dp, top = 4.dp)
                    ) {
                        ComposeTags(
                            tags,
                            onClick = handleTagClick
                        )
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
