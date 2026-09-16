package com.client.xvideos.x.screens.videoplayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.hilt.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.screens.videoplayer.atom.ComposeTags
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar

class ScreenX_VideoPlayer(val url: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        val vm = getScreenModel<ScreenX_VideoPlayerSM, ScreenX_VideoPlayerSM.Factory> { factory ->
            factory.create(url)
        }

        if (vm.isError) {
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
                        Button(onClick = { vm.loadVideo(forceReload = true) }) {
                            Text("Повторить")
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Button(onClick = { navigator.pop() }) {
                            Text("Назад")
                        }
                    }
                }
            }
        } else if (vm.isLoading || vm.passedHLS.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            // Единый Compose-плеер (общий с R/L). Хост сам освобождает ExoPlayer
            // при выходе из композиции (RememberObserver).
            val host = remember(vm.passedHLS) {
                MediaPlayerHost(
                    mediaUrl = vm.passedHLS,
                    isMuted = true, // видео X всегда без звука
                    isLooping = false,
                ).apply {
                    onError = {
                        vm.onPlaybackError()
                    }
                }
            }

            // Позиция, вернувшаяся из полноэкранного экрана через EventBus.
            // Ждём готовности медиа (totalTime > 0), т.к. плеер стартует с 0.
            LaunchedEffect(vm.positionFromFullscreen, host.totalTime) {
                val pos = vm.positionFromFullscreen
                if (pos != -1L && host.totalTime > 0) {
                    host.seekTo(pos / 1000f)
                    host.play()
                    vm.positionFromFullscreen = -1L
                }
            }

            Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
                ComposeVideoPlayer(
                    playerHost = host,
                    modifier = Modifier.fillMaxSize(),
                    onTap = { host.togglePlayPause() },
                    overlay = {
                        // Теги/каналы/порноактрисы поверх видео (вне zoomable-области)
                        Box(modifier = Modifier.align(Alignment.TopCenter)) {
                            ComposeTags(
                                vm.tags,
                                onClick = {
                                    host.pause()
                                    vm.openTag(it, navigator)
                                }
                            )
                        }
                        // Панель управления снизу
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            X_PlayerBottomBar(
                                host = host,
                                onFullScreen = {
                                    host.pause()
                                    vm.openFullScreen(navigator, (host.currentTime * 1000).toLong())
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
