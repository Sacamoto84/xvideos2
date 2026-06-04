package com.redgifs.common.video.player_with_menu.atom

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.client.xvideos.BuildConfig
import com.client.xvideos.common.videoplayer.host.MediaPlayerHost
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable
import timber.log.Timber
import kotlin.math.absoluteValue

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun VideoPlayerWithMenuContent(
    modifier: Modifier,
    playerHost: MediaPlayerHost,
    onClick: () -> Unit = {},
    autoRotate: Boolean
) {

    if (BuildConfig.DEBUG) { SideEffect { Timber.i("@@@ VideoPlayerWithMenuContent()") } }

    val zoomState = rememberZoomState(maxScale = 3f)
    LaunchedEffect(playerHost.videoFitMode) { zoomState.reset() }

    var volumeDragAmount by remember { mutableFloatStateOf(0f) }
    val volumeDragModifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures(
            onDragStart = { volumeDragAmount = 0f },
            onDragEnd = {
                val dx = if (volumeDragAmount.absoluteValue > 400) 1f else 1 / 30f
                playerHost.isSliding = true
                playerHost.seekTo((playerHost.currentTime + (if (volumeDragAmount > 0) dx else -dx)).coerceIn(0f, playerHost.totalTime.toFloat()))
                playerHost.isSliding = false
            },
            onDragCancel = { },
            onHorizontalDrag = { _, dragAmount -> volumeDragAmount += dragAmount }
        )
    }


    Box( modifier = modifier.clipToBounds() ) {

        Box(modifier = modifier.zoomable(zoomState = zoomState, zoomEnabled = true, enableOneFingerZoom = false, onTap = { onClick.invoke() })) {
            StaticPlayer( playerHost, autoRotate )
        }

        //Нижняя сенсорная часть
        Box(modifier = Modifier.fillMaxHeight(1 / 3f).fillMaxWidth().align(Alignment.BottomCenter).then(volumeDragModifier).alpha(0.5f).background(Color.Transparent))

        if (playerHost.isBuffering) {
            Box( modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center ) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).size(40.dp), color = Color.LightGray)
            }
        }

        if (playerHost.poster) {
//            Box( modifier = Modifier.fillMaxSize().background(Color.Magenta), contentAlignment = Alignment.Center ) {
//
//            }
        }

    }

}
