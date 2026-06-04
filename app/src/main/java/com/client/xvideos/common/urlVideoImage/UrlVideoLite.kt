package com.client.xvideos.common.urlVideoImage

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
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.redgifs.common.video.player_with_menu.atom.VideoPlayerWithMenuContent
import timber.log.Timber

/**
 * Легкий inline-плеер для X preview.
 *
 * Использует общий MediaPlayerHost, как R/L, и всегда держит картинку-превью поверх
 * загрузки/ошибки, чтобы вместо черного экрана оставался понятный poster.
 */
@Composable
fun UrlVideoLite(
    url: String,
    posterUrl: String = "",
    modifier: Modifier = Modifier,
    fallbackUrls: List<String> = emptyList(),
    onClick: () -> Unit = {}
) {
    val urlCandidates = remember(url, fallbackUrls) { xPreviewVideoCandidates(url, fallbackUrls) }
    var currentUrlIndex by remember(urlCandidates) { mutableStateOf(0) }
    val mediaUrl = urlCandidates.getOrNull(currentUrlIndex)
    var playbackError by remember(urlCandidates, currentUrlIndex) { mutableStateOf(mediaUrl == null) }
    val playerHost = remember(mediaUrl) {
        MediaPlayerHost(
            mediaUrl = mediaUrl.orEmpty(),
            isPaused = mediaUrl == null,
            isMuted = true,
            headers = xPreviewVideoHeaders()
        )
    }

    LaunchedEffect(urlCandidates, posterUrl) {
        Timber.i(
            """
            !!! X preview video candidates
            poster: $posterUrl
            candidates:
            ${urlCandidates.joinToString(separator = "\n")}
            """.trimIndent()
        )
    }

    LaunchedEffect(playerHost, mediaUrl) {
        playerHost.videoFitMode = ScreenResize.FILL
        playerHost.onError = {
            if (currentUrlIndex < urlCandidates.lastIndex) {
                Timber.w(
                    """
                    !!! X preview video fallback
                    failed index: ${currentUrlIndex + 1}/${urlCandidates.size}
                    error: ${it.message}
                    failed url: $mediaUrl
                    next url: ${urlCandidates.getOrNull(currentUrlIndex + 1)}
                    """.trimIndent()
                )
                currentUrlIndex += 1
            } else {
                playbackError = true
                Timber.e(
                    """
                    !!! X preview video error
                    error: ${it.message}
                    failed url: $mediaUrl
                    all candidates:
                    ${urlCandidates.joinToString(separator = "\n")}
                    """.trimIndent()
                )
            }
        }

        if (mediaUrl == null) {
            Timber.w(
                """
                !!! X preview video url missing
                source url: $url
                fallback urls: ${fallbackUrls.joinToString()}
                poster: $posterUrl
                """.trimIndent()
            )
            playerHost.pause()
        } else {
            Timber.i("!!! X preview video load ${currentUrlIndex + 1}/${urlCandidates.size}: $mediaUrl")
            playerHost.play()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (mediaUrl != null && !playbackError) {
            VideoPlayerWithMenuContent(
                modifier = Modifier.fillMaxSize(),
                playerHost = playerHost,
                onClick = onClick,
                autoRotate = false
            )
        }

        AnimatedVisibility(
            visible = playerHost.poster || playbackError || mediaUrl == null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (posterUrl.isNotBlank() && !posterUrl.equals("null", ignoreCase = true)) {
                UrlImage(
                    url = posterUrl,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF202020)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                }
            }
        }

        if (playerHost.poster && !playbackError && mediaUrl != null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.LightGray
            )
        }
    }
}

private fun xPreviewVideoHeaders(): Map<String, String> {
    return mapOf(
        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36",
        "Referer" to "https://www.xvideos.com/",
        "Origin" to "https://www.xvideos.com",
        "Accept" to "*/*",
        "Accept-Language" to "ru,en;q=0.9"
    )
}

private fun xPreviewVideoCandidates(url: String, fallbackUrls: List<String>): List<String> {
    val baseUrls = buildList {
        add(url)
        addAll(fallbackUrls)
    }.mapNotNull { it.normalizedPreviewUrlOrNull() }

    return buildList {
        baseUrls.forEach { candidate ->
            add(candidate)
            candidate.withHost("thumb-cdn77.xvideos-cdn.com")?.let(::add)
            candidate.withHost("thumbs-gcore.xvideos-cdn.com")?.let(::add)
            candidate.withHost("cdn77-pic.xvideos-cdn.com")?.let(::add)
            candidate.withHost("gcore-pic.xvideos-cdn.com")?.let(::add)
        }
    }.distinct()
}

private fun String.normalizedPreviewUrlOrNull(): String? {
    return trim()
        .takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
}

private fun String.withHost(host: String): String? {
    val schemeEnd = indexOf("://").takeIf { it >= 0 } ?: return null
    val pathStart = indexOf('/', startIndex = schemeEnd + 3).takeIf { it >= 0 } ?: return null
    return substring(0, schemeEnd + 3) + host + substring(pathStart)
}
