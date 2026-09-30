package com.client.xvideos.r.ui.fullscreen.molecule

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.navigator.Navigator
import com.client.xvideos.common.videoplayer.feed.rememberFeedPlayerState
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.r.ui.fullscreen.ScreenRedFullScreenSM

/**
 * Экран одного ролика — вход без ленты (`feedKey == null`).
 */
@Composable
fun RedFullScreenSingle(
    item: GifsInfo,
    vm: ScreenRedFullScreenSM,
    navigator: Navigator
) {
    var isVideoBuffering by remember { mutableStateOf(false) }
    var isZoomed by remember { mutableStateOf(false) }
    var resetZoomTrigger by remember { mutableIntStateOf(0) }
    val downloadedKeys by vm.downloadRed.downloadedVideoKeys.collectAsStateWithLifecycle()

    val handleBuffering: (Boolean) -> Unit = remember { { isVideoBuffering = it } }
    val handleZoomChanged: (Boolean) -> Unit = remember { { isZoomed = it } }
    val handleBack: () -> Unit = remember(navigator) {
        {
            if (isZoomed) {
                resetZoomTrigger++
            } else {
                navigator.pop()
            }
        }
    }

    // Нажатие кнопки «Назад» при активном увеличении кадра сбрасывает зум, иначе закрывает плеер
    BackHandler(enabled = isZoomed) {
        resetZoomTrigger++
    }
    BackHandler(enabled = !isZoomed, onBack = handleBack)

    val feedState = rememberFeedPlayerState(poolCapacity = 1)

    // Без этого statusControl.currentPlayingIndex остаётся C.INDEX_UNSET, политика
    // всегда отдаёт CACHED_ONLY, и preload-менеджер на этом экране не делает ничего.
    LaunchedEffect(feedState) { feedState.updateCurrentPage(0) }

    DisposableEffect(item.id) {
        onDispose { vm.resetSpeed() }
    }

    RedFullScreenScaffold(vm = vm, isVideoBuffering = isVideoBuffering) { bottomPadding ->
        RedFullScreenPage(
            item = item,
            vm = vm,
            navigator = navigator,
            feedState = feedState,
            downloadedKeys = downloadedKeys,
            index = 0,
            bottomPadding = bottomPadding,
            play = vm.play,
            isCurrentPage = true,
            showOverlay = true,
            onBuffering = handleBuffering,
            onZoomChanged = handleZoomChanged,
            resetZoomTrigger = resetZoomTrigger,
            onBack = handleBack,
        )
    }
}

@Preview
@Composable
private fun RedFullScreenSinglePreview() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("RedFullScreenSingle", color = Color.White)
    }
}
