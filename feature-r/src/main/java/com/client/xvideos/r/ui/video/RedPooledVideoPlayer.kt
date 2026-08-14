package com.client.xvideos.r.ui.video

import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.ScaleAndRotateTransformation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.lifecycle.rememberPooledPlayer
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.r.common.video.PlayerControls
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/**
 * Страница ленты, работающая на общем пуле плееров [FeedPlayerState].
 *
 * Отличие от [RedVideoPlayerWithMenu]: `ExoPlayer` не создаётся на каждую страницу,
 * а берётся из пула (`rememberPooledPlayer`) и возвращается туда же при уходе
 * страницы из композиции. Медиа-источник приходит от preload-менеджера, то есть
 * соседние ролики уже частично загружены к моменту свайпа.
 */
@OptIn(UnstableApi::class)
@Composable
fun RedPooledVideoPlayer(
    feedState: FeedPlayerState,
    index: Int,
    url: String,
    play: Boolean,
    isMute: Boolean,
    isCurrentPage: Boolean,
    autoRotate: Boolean,
    timeA: Float,
    timeB: Float,
    enableAB: Boolean,
    onChangeTime: (Pair<Float, Int>) -> Unit,
    onPlayerControlsReady: (PlayerControls) -> Unit,
    onClick: () -> Unit,
    isBuferring: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mediaItem = remember(index, url) { feedState.mediaItemFor(index, url) }

    val player: ExoPlayer? = rememberPooledPlayer(
        mediaItem = mediaItem,
        playerPool = feedState.playerPool,
        playerSetup = { exo ->
            exo.setMediaSource(feedState.mediaSourceFor(mediaItem, index))
            exo.prepare()
        },
        playerTeardown = { exo -> exo.playWhenReady = false },
    )

    var isBuffering by remember(player) { mutableStateOf(true) }
    LaunchedEffect(isBuffering) { isBuferring(isBuffering) }

    LaunchedEffect(player, play, isCurrentPage) {
        player?.playWhenReady = play && isCurrentPage
    }

    LaunchedEffect(player, isMute) {
        player?.volume = if (isMute) 0f else 1f
    }

    LaunchedEffect(player, autoRotate) {
        val rotate = ScaleAndRotateTransformation.Builder()
            .setRotationDegrees(if (autoRotate) -90f else 0f)
            .build()
        player?.setVideoEffects(listOf(rotate))
    }

    DisposableEffect(player) {
        val exo = player
        if (exo == null) {
            onDispose { }
        } else {
            val listener = object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    isBuffering = playbackState == Player.STATE_BUFFERING
                }
            }
            exo.addListener(listener)
            isBuffering = exo.playbackState == Player.STATE_BUFFERING
            onDispose { exo.removeListener(listener) }
        }
    }

    // Время/длительность и петля A-B. Шаг 50 мс — как в прежнем CMPPlayer2,
    // чтобы поведение полосы времени и A-B не изменилось.
    LaunchedEffect(player, isCurrentPage, enableAB, timeA, timeB) {
        val exo = player ?: return@LaunchedEffect
        while (isActive) {
            val position = (exo.currentPosition / 1000f).coerceAtLeast(0f)
            val durationMs = exo.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            if (isCurrentPage) onChangeTime(position to (durationMs / 1000).toInt())
            if (enableAB && position >= timeB) exo.seekTo((timeA * 1000).toLong())
            delay(50)
        }
    }

    LaunchedEffect(player, isCurrentPage) {
        val exo = player ?: return@LaunchedEffect
        if (!isCurrentPage) return@LaunchedEffect
        onPlayerControlsReady(object : PlayerControls {
            override fun forward(seconds: Float) {
                exo.seekTo(exo.currentPosition + (seconds * 1000).toLong())
            }

            override fun rewind(seconds: Float) {
                exo.seekTo((exo.currentPosition - (seconds * 1000).toLong()).coerceAtLeast(0L))
            }

            override fun seekTo(positionSeconds: Float) {
                exo.seekTo((positionSeconds * 1000).toLong().coerceAtLeast(0L))
            }

            override fun stop() {
                exo.playWhenReady = false
                exo.seekTo(0L)
            }

            override fun pause() {
                exo.playWhenReady = false
            }

            override fun play() {
                exo.playWhenReady = true
            }
        })
    }

    val zoomState = rememberZoomState(maxScale = 3f)

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        ContentFrame(
            player = player,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onTap = { onClick() },
                ),
            contentScale = ContentScale.Fit,
            keepContentOnReset = true,
        )

        if (isBuffering) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color.LightGray,
                )
            }
        }
    }
}
