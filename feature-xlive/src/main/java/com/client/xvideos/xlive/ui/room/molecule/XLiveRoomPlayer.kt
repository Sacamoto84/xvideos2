package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.util.CMPPlayer2

/**
 * Живой поток комнаты.
 *
 * Общий плеер приложения в режиме прямого эфира: LL-HLS с частями сегментов
 * media3 разбирает сам, задержку от края берёт из `PART-HOLD-BACK` плейлиста.
 *
 * @param hlsUrl адрес мастер-плейлиста; пустой — свежий адрес в пути, по центру индикатор.
 * @param onStarted плеер этого экземпляра начал играть адрес — сайт отдаёт его на один плеер.
 * @param onError плеер не смог играть; текст без адреса потока.
 * @param onBuffering плеер ждёт данных (`true`) или снова играет (`false`).
 */
@Composable
fun XLiveRoomPlayer(
    hlsUrl: String,
    onStarted: (String) -> Unit,
    onError: (String) -> Unit,
    onBuffering: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnBuffering by rememberUpdatedState(onBuffering)

    val config = remember(hlsUrl) { PlayerPlaybackConfig(url = hlsUrl, isLiveStream = true) }
    val callbacks = remember {
        PlayerPlaybackCallbacks(
            bufferCallback = { currentOnBuffering(it) },
            error = { currentOnError(it.message) },
        )
    }

    // Ключ — адрес: эффект срабатывает на каждый новый адрес и на каждый новый
    // экземпляр плеера после пересоздания Activity, но не на перекомпоновку.
    LaunchedEffect(hlsUrl) {
        if (hlsUrl.isNotBlank()) currentOnStarted(hlsUrl)
    }

    Box(modifier = modifier.background(Color.Black)) {
        CMPPlayer2(modifier = Modifier.fillMaxSize(), config = config, callbacks = callbacks)
        if (hlsUrl.isBlank()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Preview
@Composable
private fun XLiveRoomPlayerPreview() {
    XLiveRoomPlayer(hlsUrl = "", onStarted = {}, onError = {}, onBuffering = {})
}
