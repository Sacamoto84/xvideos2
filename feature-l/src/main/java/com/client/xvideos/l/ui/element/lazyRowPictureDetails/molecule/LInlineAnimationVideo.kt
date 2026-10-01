package com.client.xvideos.l.ui.element.lazyRowPictureDetails.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.VideoPlayerWithMenuContent
import com.client.xvideos.l.model.isLVideoFileUrl
import com.client.xvideos.l.model.lMediaRequestHeaders
import com.client.xvideos.l.ui.element.lazyRowPictureDetails.atom.AnimatedVideoPlaceholder
import timber.log.Timber

@Composable
fun LInlineAnimationVideo(
    url: String,
    previewUrl: String,
    albumName: String,
    modifier: Modifier = Modifier
) {
    val isInspection = LocalInspectionMode.current
    val playerHost = remember(url) {
        MediaPlayerHost(
            mediaUrl = url,
            isPaused = false,
            isMuted = true,
            headers = lMediaRequestHeaders()
        )
    }
    var playbackError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(playerHost) {
        if (!isInspection) {
            playerHost.videoFitMode = ScreenResize.FILL
            playerHost.onError = {
                playbackError = true
                Timber.e("L inline video error: ${it.message}")
            }
            playerHost.play()
        }
    }

    Box(modifier = modifier) {
        if (!isInspection) {
            VideoPlayerWithMenuContent(
                modifier = Modifier.fillMaxSize(),
                playerHost = playerHost,
                onClick = {},
                autoRotate = false
            )
        }

        AnimatedVisibility(
            visible = playerHost.poster || playbackError || isInspection,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (previewUrl.isNotBlank() && !previewUrl.isLVideoFileUrl()) {
                UrlImage(
                    url = previewUrl,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier.fillMaxSize(),
                    albumName = albumName,
                    isAnimated = false
                )
            } else {
                AnimatedVideoPlaceholder(modifier = Modifier.fillMaxSize())
            }
        }

        if (playerHost.poster && !playbackError && !isInspection) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.LightGray
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun LInlineAnimationVideoPreview() {
    LInlineAnimationVideo(
        url = "https://sample.com/video.mp4",
        previewUrl = "",
        albumName = "Sample Album",
        modifier = Modifier
            .width(200.dp)
            .height(150.dp)
    )
}
