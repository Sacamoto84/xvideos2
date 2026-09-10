package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.noRippleClickable
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.VideoPlayerWithMenuContent
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lMediaRequestHeaders

/**
 * Видео на странице полноэкранного просмотра L.
 *
 * Выделено из `L_FullScreenImage.kt` (было 800 строк). Тела функций не менялись
 * — перенос дословный.
 */
@Composable
internal fun LFullScreenVideo(
    url: String,
    previewUrl: String,
    albumName: String,
    autoPlay: Boolean,
    isCurrentPage: Boolean,
    isPlayerActive: Boolean,
    isMuted: Boolean,
    seekDragEnabled: Boolean,
    rotate: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit
) {
    // Соседние страницы пейджера тоже скомпонованы, и каждая поднимала свой
    // ExoPlayer (кодек + буферы). Плеер создаём только когда прокрутка
    // остановилась на этой странице, до этого показываем постер.
    if (!isPlayerActive) {
        Box(modifier = modifier.noRippleClickable(onClick = onTap)) {
            LFullScreenVideoPoster(
                previewUrl = previewUrl,
                albumName = albumName,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    val playerHost = remember(url) {
        MediaPlayerHost(
            mediaUrl = url,
            isPaused = !autoPlay || !isCurrentPage,
            isMuted = isMuted,
            headers = lMediaRequestHeaders()
        )
    }

    // playerHost помнится по url, поэтому переключение звука доводим отдельно.
    LaunchedEffect(playerHost, isMuted) {
        if (isMuted) playerHost.mute() else playerHost.unmute()
    }
    var playbackError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(playerHost) {
        playerHost.videoFitMode = ScreenResize.FIT
        playerHost.onError = {
            playbackError = true
            timber.log.Timber.e("!!! L fullscreen video error: ${it.message}")
        }
    }

    LaunchedEffect(autoPlay, isCurrentPage) {
        if (autoPlay && isCurrentPage) {
            playerHost.play()
        } else {
            playerHost.pause()
        }
    }

    Box(modifier = modifier) {
        VideoPlayerWithMenuContent(
            modifier = Modifier.fillMaxSize(),
            playerHost = playerHost,
            onClick = onTap,
            autoRotate = rotate,
            seekDragEnabled = seekDragEnabled
        )

        AnimatedVisibility(
            visible = playerHost.poster || playbackError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            LFullScreenVideoPoster(
                previewUrl = previewUrl,
                albumName = albumName,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (playerHost.poster && !playbackError) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.LightGray
            )
        }
    }
}

@Composable
private fun LFullScreenVideoPoster(
    previewUrl: String,
    albumName: String,
    modifier: Modifier = Modifier
) {
    if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
        UrlImage(
            url = previewUrl,
            contentScale = ContentScale.Fit,
            modifier = modifier,
            albumName = albumName,
            autoPlay = false,
            isAnimated = false
        )
    } else {
        Box(
            modifier = modifier.background(Color(0xFF202020)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
        }
    }
}
