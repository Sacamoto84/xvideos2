package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.client.xvideos.common.videoplayer.feed.FeedPlayerState
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.model.RedFullScreenPlayerState
import com.client.xvideos.r.ui.fullscreen.redVideoUrl
import com.client.xvideos.r.ui.video.RedPooledVideoPlayer

@Composable
fun RedFullScreenPage(
    item: GifsInfo,
    player: RedFullScreenPlayerState,
    feedState: FeedPlayerState,
    downloadedKeys: Set<String>?,
    index: Int,
    bottomPadding: Dp,
    play: Boolean,
    isCurrentPage: Boolean,
    showOverlay: Boolean,
    onBuffering: (Boolean) -> Unit,
    onZoomChanged: (Boolean) -> Unit = {},
    resetZoomTrigger: Int = 0,
    overlay: @Composable () -> Unit = {},
) {
    // Адрес — на момент входа страницы, `downloadedKeys` в ключах нет. Иначе
    // ролик, скачанный во время просмотра, менял адрес с HLS на файл, а с ним
    // MediaItem — ключ rememberPooledPlayer: плеер возвращался в пул и начинал
    // ролик заново после спиннера (проход 203, C2). Скачанный файл заиграет при
    // следующем входе страницы. Прогрев индекса этим не сбивается: окно
    // предзагрузки перечитывает адрес, только когда индекс входит в окно заново.
    //
    // Пока набор скачанных не прочитан (`null`), адреса нет и плеер не создаётся.
    // Пустой набор до конца обхода каталога выглядел как «не скачан»: после смерти
    // процесса экран сам создаёт DownloadRed, и страница навсегда запоминала HLS у
    // скачанного ролика — без сети он не играл (проход 208, C1).
    val keysLoaded = downloadedKeys != null
    val videoUri = remember(item.id, item.userName, keysLoaded) {
        downloadedKeys?.let { redVideoUrl(item, it) }
    }

    Box(Modifier.fillMaxSize()) {
        if (videoUri == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        } else {
            RedPooledVideoPlayer(
                feedState = feedState,
                index = index,
                url = videoUri,
                modifier = Modifier.padding(bottom = bottomPadding),
                play = play,
                isMute = player.mute,
                isCurrentPage = isCurrentPage,
                autoRotate = player.autoRotate,
                timeA = player.timeA,
                timeB = player.timeB,
                enableAB = player.enableAB,
                speed = player.speed,
                onTimeChanged = { position, duration ->
                    if (isCurrentPage) {
                        player.currentPlayerTime = position
                        player.currentPlayerDuration = duration
                    }
                },
                onPlayerControlsReady = { controls ->
                    if (isCurrentPage) {
                        player.currentPlayerControls = controls
                    }
                },
                onPlayerControlsRelease = { controls ->
                    // Сравнение по ссылке: страница отзывает только свои controls и не
                    // затирает те, что успела выставить пришедшая ей на смену.
                    if (player.currentPlayerControls === controls) player.currentPlayerControls = null
                },
                onClick = { if (isCurrentPage) player.togglePlay() },
                onBufferingChanged = { buffering ->
                    if (isCurrentPage) {
                        onBuffering(buffering)
                    }
                },
                onZoomChanged = onZoomChanged,
                resetZoomTrigger = resetZoomTrigger,
            )
        }

        if (showOverlay) {
            overlay()
        }
    }
}

@Preview
@Composable
private fun RedFullScreenPagePreview() {
    val feedState = rememberFeedPlayerState(poolCapacity = 1)
    Box(Modifier.fillMaxSize()) {
        RedPooledVideoPlayer(
            feedState = feedState,
            index = 0,
            url = "",
            play = false,
            isMute = true,
            isCurrentPage = true,
            autoRotate = false,
            timeA = 0f,
            timeB = 0f,
            enableAB = false,
            speed = com.client.xvideos.common.videoplayer.model.PlayerSpeed.DEFAULT,
            onTimeChanged = { _, _ -> },
            onPlayerControlsReady = {},
            onPlayerControlsRelease = {},
            onClick = {},
            onBufferingChanged = {},
            onZoomChanged = {},
            resetZoomTrigger = 0
        )
    }
}
