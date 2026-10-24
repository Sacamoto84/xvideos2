package com.client.xvideos.xlive.ui.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.util.CMPPlayer2

/**
 * Живой HLS-поток комнаты XLive.
 *
 * Общий плеер приложения в режиме прямого эфира: LL-HLS с частями сегментов
 * media3 разбирает сам, задержку от края берёт из `PART-HOLD-BACK` плейлиста.
 *
 * @param hlsUrl адрес мастер-плейлиста.
 * @param onError плеер не смог играть; текст без адреса потока.
 * @param onBuffering плеер ждёт данных (`true`) или снова играет (`false`).
 */
@Composable
fun XLiveStreamPlayer(
    hlsUrl: String,
    onError: (String) -> Unit,
    onBuffering: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnError by rememberUpdatedState(onError)
    val currentOnBuffering by rememberUpdatedState(onBuffering)

    val config = remember(hlsUrl) { PlayerPlaybackConfig(url = hlsUrl, isLiveStream = true) }
    val callbacks = remember {
        PlayerPlaybackCallbacks(
            bufferCallback = { currentOnBuffering(it) },
            error = { currentOnError(it.message) },
        )
    }

    CMPPlayer2(modifier = modifier, config = config, callbacks = callbacks)
}

@Preview
@Composable
private fun XLiveStreamPlayerPreview() {
    XLiveStreamPlayer(hlsUrl = "", onError = {}, onBuffering = {})
}
