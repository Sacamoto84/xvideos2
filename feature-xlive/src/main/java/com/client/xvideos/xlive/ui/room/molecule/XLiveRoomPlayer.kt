package com.client.xvideos.xlive.ui.room.molecule

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.videoplayer.local.molecule.LocalPlayerFullScreenTopBar
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackCallbacks
import com.client.xvideos.common.videoplayer.model.PlayerPlaybackConfig
import com.client.xvideos.common.videoplayer.ui.VideoZoomHud
import com.client.xvideos.common.videoplayer.util.CMPPlayer2
import com.client.xvideos.xlive.ui.room.atom.XLiveFullScreenButton
import com.client.xvideos.xlive.ui.room.atom.XLiveMuteButton
import com.client.xvideos.xlive.ui.room.atom.XLiveRecordingBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

/** Задержка до скрытия панелей управления на полном экране, мс. */
private const val CONTROLS_AUTO_HIDE_DELAY_MS = 3500L

/** Предельное увеличение кадра щипком. */
private const val MAX_ZOOM_SCALE = 4f

/**
 * Живой поток комнаты.
 *
 * Общий плеер приложения в режиме прямого эфира: LL-HLS с частями сегментов
 * media3 разбирает сам, задержку от края берёт из `PART-HOLD-BACK` плейлиста.
 * Кадр увеличивается щипком (до [MAX_ZOOM_SCALE]) и двойным тапом; при смене
 * полноэкранного режима масштаб сбрасывается.
 *
 * @param hlsUrl адрес мастер-плейлиста; пустой — свежий адрес в пути, по центру индикатор.
 * @param muted звук выключен — так эфир и открывается.
 * @param onToggleMute кнопка звука в углу видео.
 * @param onStarted плеер этого экземпляра начал играть адрес — сайт отдаёт его на один плеер.
 * @param onError плеер не смог играть; текст без адреса потока.
 * @param onBuffering плеер ждёт данных (`true`) или снова играет (`false`).
 * @param isFullScreen видео открыто на весь экран в альбомном режиме.
 * @param title заголовок (ник комнаты) для верхней панели на весь экран.
 * @param onToggleFullScreen кнопка переключения полного экрана.
 * @param onExitFullScreen кнопка «назад» в верхней панели на весь экран.
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
    isFullScreen: Boolean = false,
    title: String = "",
    onToggleFullScreen: () -> Unit = {},
    onExitFullScreen: () -> Unit = {},
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

    var areControlsVisible by remember { mutableStateOf(true) }
    val zoomState = rememberZoomState(maxScale = MAX_ZOOM_SCALE)
    val scope = rememberCoroutineScope()

    // Рамка видео при смене режима меняет размер: прежний сдвиг кадра к ней не подходит.
    LaunchedEffect(isFullScreen) {
        areControlsVisible = true
        zoomState.reset()
    }

    LaunchedEffect(isFullScreen, areControlsVisible) {
        if (isFullScreen && areControlsVisible) {
            delay(CONTROLS_AUTO_HIDE_DELAY_MS)
            areControlsVisible = false
        }
    }

    // Обрезка по рамке: увеличенный кадр не залезает на шапку под видео.
    Box(
        modifier = modifier
            .background(Color.Black)
            .clipToBounds()
    ) {
        // Увеличивается только кадр: кнопки и плашки лежат поверх, вне зум-области.
        // Двойной тап — 2.5× в точку касания и обратно (поведение zoomable по умолчанию).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zoomable(
                    zoomState = zoomState,
                    enableOneFingerZoom = false,
                    onTap = { if (isFullScreen) areControlsVisible = !areControlsVisible },
                )
        ) {
            CMPPlayer2(modifier = Modifier.fillMaxSize(), config = config, callbacks = callbacks)
        }
        if (hlsUrl.isBlank()) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.align(Alignment.Center))
        }

        VideoZoomHud(
            scale = zoomState.scale,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = if (isFullScreen) 56.dp else 8.dp),
            onReset = { scope.launch { zoomState.reset() } },
        )

        if (isFullScreen) {
            AnimatedVisibility(
                visible = areControlsVisible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                LocalPlayerFullScreenTopBar(
                    title = title,
                    onBack = onExitFullScreen
                )
            }
        }

        AnimatedVisibility(
            visible = !isFullScreen || areControlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Row(
                modifier = Modifier
                    .windowInsetsPadding(
                        if (isFullScreen) WindowInsets.displayCutout.only(WindowInsetsSides.Bottom + WindowInsetsSides.End)
                        else WindowInsets(0, 0, 0, 0)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                XLiveMuteButton(
                    muted = muted,
                    onClick = onToggleMute,
                )
                XLiveFullScreenButton(
                    isFullScreen = isFullScreen,
                    onClick = onToggleFullScreen,
                )
            }
        }

        if (recordingStartedAt != null) {
            XLiveRecordingBadge(
                startedAtElapsedMs = recordingStartedAt,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(
                        if (isFullScreen) WindowInsets.displayCutout.only(WindowInsetsSides.Start)
                        else WindowInsets(0, 0, 0, 0)
                    )
                    .padding(
                        start = 8.dp,
                        top = if (isFullScreen) 56.dp else 8.dp,
                        end = 8.dp,
                        bottom = 8.dp,
                    ),
            )
        }
    }
}

@Preview
@Composable
private fun XLiveRoomPlayerPreview() {
    XLiveRoomPlayer(hlsUrl = "", muted = true, onToggleMute = {}, onStarted = {}, onError = {}, onBuffering = {})
}
