package com.client.xvideos.l.ui.screens.screenFullScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.noRippleClickable
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.VideoPlayerWithMenuContent
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lMediaRequestHeaders

private val ERROR_CONTAINER_SHAPE = RoundedCornerShape(8.dp)
private val ERROR_BG_COLOR = Color(0xBF000000)
private val POSTER_PLACEHOLDER_BG = Color(0xFF202020)
private val COLOR_WHITE = Color.White
private val COLOR_LIGHT_GRAY = Color.LightGray
private val ERROR_HORIZONTAL_PADDING = 16.dp
private val ERROR_VERTICAL_PADDING = 10.dp
private val ERROR_FONT_SIZE = 14.sp
private const val TEXT_PLAYBACK_ERROR = "Ошибка воспроизведения"
private val ENTER_FADE = fadeIn()
private val EXIT_FADE = fadeOut()
private val ICON_PLAY_ARROW = Icons.Default.PlayArrow
private val ALIGN_CENTER = Alignment.Center
private val FULL_SIZE_MODIFIER = Modifier.fillMaxSize()
private val CONTENT_SCALE_FIT = ContentScale.Fit
private val POSTER_PLACEHOLDER_MODIFIER = Modifier.background(POSTER_PLACEHOLDER_BG)
private val ERROR_BOX_BASE_MODIFIER = Modifier
    .clip(ERROR_CONTAINER_SHAPE)
    .background(ERROR_BG_COLOR)
    .padding(horizontal = ERROR_HORIZONTAL_PADDING, vertical = ERROR_VERTICAL_PADDING)

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
    resetZoomTrigger: Int = 0,
    onZoomChanged: (Boolean) -> Unit = {},
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
                modifier = FULL_SIZE_MODIFIER
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
            timber.log.Timber.e("L fullscreen video error: ${it.message}")
        }
    }

    LaunchedEffect(playerHost, autoPlay, isCurrentPage) {
        if (autoPlay && isCurrentPage) {
            playerHost.play()
        } else {
            playerHost.pause()
        }
    }

    Box(modifier = modifier) {
        VideoPlayerWithMenuContent(
            modifier = FULL_SIZE_MODIFIER,
            playerHost = playerHost,
            onClick = onTap,
            autoRotate = rotate,
            seekDragEnabled = seekDragEnabled,
            resetZoomTrigger = resetZoomTrigger,
            onZoomChanged = onZoomChanged
        )

        AnimatedVisibility(
            visible = playerHost.poster || playbackError,
            enter = ENTER_FADE,
            exit = EXIT_FADE
        ) {
            LFullScreenVideoPoster(
                previewUrl = previewUrl,
                albumName = albumName,
                modifier = FULL_SIZE_MODIFIER
            )
        }

        if (playerHost.poster && !playbackError) {
            CircularProgressIndicator(
                modifier = Modifier.align(ALIGN_CENTER),
                color = COLOR_LIGHT_GRAY
            )
        }

        if (playbackError) {
            Box(
                modifier = Modifier
                    .align(ALIGN_CENTER)
                    .then(ERROR_BOX_BASE_MODIFIER),
                contentAlignment = ALIGN_CENTER
            ) {
                Text(
                    text = TEXT_PLAYBACK_ERROR,
                    color = COLOR_WHITE,
                    fontSize = ERROR_FONT_SIZE
                )
            }
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
            contentScale = CONTENT_SCALE_FIT,
            modifier = modifier,
            albumName = albumName,
            autoPlay = false,
            isAnimated = false
        )
    } else {
        Box(
            modifier = modifier.then(POSTER_PLACEHOLDER_MODIFIER),
            contentAlignment = ALIGN_CENTER
        ) {
            Icon(ICON_PLAY_ARROW, contentDescription = null, tint = COLOR_WHITE)
        }
    }
}
