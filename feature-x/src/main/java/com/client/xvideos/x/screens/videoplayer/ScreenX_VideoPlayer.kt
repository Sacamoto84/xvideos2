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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.displayCutoutPadding
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
import cafe.adriel.voyager.core.screen.uniqueScreenKey
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

class ScreenX_VideoPlayer(
    val url: String,
    val item: ItemsX? = null,
) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenX_VideoPlayerSM, ScreenX_VideoPlayerSM.Factory> { factory ->
            factory.create(url, item)
        }

        OrientationAndSystemBarsEffect(vm.isFullScreen)

        // Нажатие кнопки «Назад» в ландшафтном режиме возвращает в портретный режим
        BackHandler(enabled = vm.isFullScreen) {
            vm.exitFullScreen()
        }

        when {
            vm.isError -> {
                VideoPlayerErrorView(
                    onRetry = { vm.loadVideo(forceReload = true) },
                    onBack = { navigator.pop() }
                )
            }
            vm.isLoading || vm.passedHLS.isBlank() -> {
                VideoPlayerLoadingView()
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
private fun VideoPlayerLoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
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

    LaunchedEffect(vm.isFullScreen) {
        areControlsVisible = true
    }

    LaunchedEffect(vm.isFullScreen, areControlsVisible, host.isPaused) {
        if (vm.isFullScreen && areControlsVisible && !host.isPaused) {
            delay(3500)
            areControlsVisible = false
        }
    }

    RememberHistoryProgressSync(vm = vm, host = host)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
        ComposeVideoPlayer(
            playerHost = host,
            modifier = Modifier.fillMaxSize(),
            onTap = {
                if (vm.isFullScreen) {
                    areControlsVisible = !areControlsVisible
                } else {
                    host.togglePlayPause()
                }
            },
            overlay = {
                // Кнопка возврата (в ландшафтном режиме скрывается вместе с контролами)
                AnimatedVisibility(
                    visible = !vm.isFullScreen || areControlsVisible,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    IconButton(
                        onClick = {
                            if (vm.isFullScreen) {
                                vm.exitFullScreen()
                            } else {
                                navigator.pop()
                            }
                        },
                        modifier = Modifier
                            .displayCutoutPadding()
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
                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        ComposeTags(
                            vm.tags,
                            onClick = {
                                host.pause()
                                vm.openTag(it, navigator)
                            }
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
                        .padding(bottom = if (vm.isFullScreen) 68.dp else 84.dp)
                ) {
                    vm.resumeNoticeText?.let { notice ->
                        ResumePlaybackPill(
                            text = notice,
                            onRestart = {
                                host.seekTo(0f)
                                vm.restartFromBeginning()
                            }
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
                        onFullScreen = {
                            vm.toggleFullScreen()
                        }
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
                delay(3000)
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

    // Автоматическое скрытие плашки о возобновлении через 4 секунды
    LaunchedEffect(vm.resumeNoticeText) {
        if (vm.resumeNoticeText != null) {
            delay(4000)
            vm.dismissResumeNotice()
        }
    }
}
