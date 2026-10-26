package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.util.CMPPlayer2
import com.client.xvideos.xlive.ui.room.atom.XLiveMuteButton
import com.client.xvideos.xlive.ui.room.atom.XLiveRecordingBadge

/**
 * Живой поток комнаты.
 *
 * Общий плеер приложения в режиме прямого эфира: LL-HLS с частями сегментов
 * media3 разбирает сам, задержку от края берёт из `PART-HOLD-BACK` плейлиста.
 *
 * @param hlsUrl адрес мастер-плейлиста; пустой — свежий адрес в пути, по центру индикатор.
 * @param muted звук выключен — так эфир и открывается.
 * @param onToggleMute кнопка звука в углу видео.
 * @param onStarted плеер этого экземпляра начал играть адрес — сайт отдаёт его на один плеер.
 * @param onError плеер не смог играть; текст без адреса потока.
 * @param onBuffering плеер ждёт данных (`true`) или снова играет (`false`).
 * @param recordingStartedAt начало идущей записи по монотонным часам; `null` — записи нет, плашки нет.
 */
@Composable
fun XLiveRoomPlayer(
    hlsUrl: String,
    muted: Boolean,
    onToggleMute: () -> Unit,
    onStarted: (String) -> Unit,
    onError: (String) -> Unit,
    onBuffering: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    recordingStartedAt: Long? = null,
) {
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnError by rememberUpdatedState(onError)
    val currentOnBuffering by rememberUpdatedState(onBuffering)

    // Громкость — в ключе, но плеер из-за неё не пересоздаётся: CMPPlayer2
    // пересобирает источник только по адресу, а громкость ставит отдельно.
    val config = remember(hlsUrl, muted) {
        PlayerPlaybackConfig(url = hlsUrl, isLiveStream = true, volume = if (muted) 0f else 1f)
    }
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
        XLiveMuteButton(
            muted = muted,
            onClick = onToggleMute,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
        )
        if (recordingStartedAt != null) {
            XLiveRecordingBadge(
                startedAtElapsedMs = recordingStartedAt,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
            )
        }
    }
}

@Preview
@Composable
private fun XLiveRoomPlayerPreview() {
    XLiveRoomPlayer(hlsUrl = "", muted = true, onToggleMute = {}, onStarted = {}, onError = {}, onBuffering = {})
}
