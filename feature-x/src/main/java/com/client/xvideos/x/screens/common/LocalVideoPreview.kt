package com.client.xvideos.x.screens.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.coil.UrlImage
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import com.client.xvideos.common.videoplayer.model.MediaClip
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.common.videoplayer.ui.VideoPlayerWithMenuContent
import com.client.xvideos.x.feature.saved.SavedLocalVideo
import com.client.xvideos.x.feature.saved.localPreviewClips
import com.client.xvideos.x.feature.saved.videoDurationMs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Превью сохранённого ролика X прямо в карточке — 5 отрезков по секунде из
 * самого скачанного файла, по кругу, без звука и без сети. Плеер — общий
 * путь (`MediaPlayerHost` → `CMPPlayer2`), поэтому он сам встаёт на паузу в
 * фоне и под экраном замка и освобождается, когда карточка уходит из
 * композиции. Постер [posterUrl] — поверх, пока не пошёл первый кадр, и при
 * ошибке. Тап — [onClick] (вернуть постер), удержание — [onLongClick]. Пока
 * плеера нет (длительность ещё читается или ошибка), касания доходят до
 * самой карточки.
 */
@Composable
fun LocalVideoPreview(
    video: SavedLocalVideo,
    posterUrl: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Долгое нажатие по играющему превью — карточка открывает им плеер. */
    onLongClick: (() -> Unit)? = null,
) {
    if (LocalInspectionMode.current) {
        Box(modifier = modifier.fillMaxSize().background(Color(0xFF202020)))
        return
    }

    // Отрезки — после чтения длительности с диска; `null` — ещё читается.
    val clips by produceState<List<MediaClip>?>(initialValue = null, video.fileUrl) {
        value = localPreviewClips(withContext(Dispatchers.IO) { videoDurationMs(video.fileUrl) })
    }
    var playbackError by remember(video.fileUrl) { mutableStateOf(false) }
    val playerHost = remember(video.fileUrl, clips) {
        clips?.let { MediaPlayerHost(mediaUrl = video.fileUrl, isMuted = true, clips = it) }
    }

    LaunchedEffect(playerHost) {
        val host = playerHost ?: return@LaunchedEffect
        host.videoFitMode = ScreenResize.FILL
        host.onError = {
            // В сообщении бывает путь к файлу — в журнал без него.
            Timber.w("X превью из файла: ошибка воспроизведения")
            playbackError = true
        }
        host.play()
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        val host = playerHost
        if (host != null && !playbackError) {
            VideoPlayerWithMenuContent(
                modifier = Modifier.fillMaxSize(),
                playerHost = host,
                onClick = onClick,
                onLongClick = onLongClick,
                autoRotate = false,
                // Отрезки по секунде: перематывать внутри превью нечего.
                seekDragEnabled = false,
            )
        }

        AnimatedVisibility(
            visible = host == null || host.poster || playbackError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            UrlImage(
                url = posterUrl,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Preview
@Composable
private fun LocalVideoPreviewPreview() {
    LocalVideoPreview(
        video = SavedLocalVideo(fileUrl = "file:///v.mp4", posterPath = null),
        posterUrl = "",
        onClick = {}
    )
}
