package com.client.xvideos.x.screens.videoplayer

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.ui.ComposeVideoPlayer
import com.client.xvideos.x.screens.videoplayer.atom.X_PlayerBottomBar

/**
 * Плеер локального (скачанного) файла X.
 *
 * Играет напрямую `file://`-URI скачанного mp4 — без резолва HTML/HLS, без сети.
 * Видео X всегда без звука (как и стриминговый X-плеер).
 *
 * @param fileUrl `file://`-URI локального mp4 (см. `SavedX_Downloads.localUrl`).
 */
class ScreenX_LocalVideoPlayer(val fileUrl: String) : Screen {

    override val key: ScreenKey = uniqueScreenKey

    @OptIn(UnstableApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val host = remember(fileUrl) {
            MediaPlayerHost(
                mediaUrl = fileUrl,
                isMuted = true, // видео X всегда без звука
                isLooping = false,
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF040404))) {
            ComposeVideoPlayer(
                playerHost = host,
                modifier = Modifier.fillMaxSize(),
                onTap = { host.togglePlayPause() },
                overlay = {
                    IconButton(
                        onClick = { navigator.pop() },
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .statusBarsPadding()
                            .padding(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                        )
                    }
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        // Локальный файл — отдельный полноэкранный режим не требуется
                        X_PlayerBottomBar(host = host)
                    }
                }
            )
        }
    }
}
