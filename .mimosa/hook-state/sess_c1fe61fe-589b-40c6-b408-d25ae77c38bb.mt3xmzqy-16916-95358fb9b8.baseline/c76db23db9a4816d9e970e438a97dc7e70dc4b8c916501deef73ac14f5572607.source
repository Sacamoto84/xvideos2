package com.client.xvideos.x.screens.videoplayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

        if (vm.passedHLS == "") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            // Единый Compose-плеер (общий с R/L). Хост сам освобождает ExoPlayer
            // при выходе из композиции (RememberObserver).
            val host = remember(vm.passedHLS) {
                MediaPlayerHost(
                    mediaUrl = vm.passedHLS,
                    isMuted = true, // видео X всегда без звука
                    isLooping = false,
                )
            }

            // Позиция, вернувшаяся из полноэкранного экрана через EventBus.
            // Ждём готовности медиа (totalTime > 0), т.к. плеер стартует с 0.
            LaunchedEffect(vm.positionFromFullscreen, host.totalTime) {
                val pos = vm.positionFromFullscreen
                if (pos != -1L && host.totalTime > 0) {
                    host.seekTo(pos / 1000f)
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
                            ComposeTags(vm.tags, onClick = { vm.openTag(it, navigator) })
                        }
                        // Панель управления снизу
                        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                            X_PlayerBottomBar(
                                host = host,
                                onFullScreen = {
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
