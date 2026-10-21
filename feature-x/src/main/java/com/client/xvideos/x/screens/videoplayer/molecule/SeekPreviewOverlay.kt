package com.client.xvideos.x.screens.videoplayer.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.videoplayer.model.ScreenResize
import com.client.xvideos.x.screens.videoplayer.model.SeekScrub
import com.client.xvideos.x.screens.videoplayer.model.XSeekPreviewMode
import com.client.xvideos.x.screens.videoplayer.model.seekPreviewAnchorX
import com.client.xvideos.x.screens.videoplayer.model.storyboardFrameIndex

/**
 * Превью кадра при перемотке поверх плеера, пока палец ведёт ползунок
 * ([scrub] не `null`). Вид — [mode]: A за бегунком или B на весь плеер; кадр B
 * вписан так же, как видео ([videoFitMode]).
 */
@Composable
fun SeekPreviewOverlay(
    mode: XSeekPreviewMode,
    storyboard: ImageBitmap?,
    scrub: SeekScrub?,
    durationSec: Int,
    videoFitMode: ScreenResize,
    modifier: Modifier = Modifier,
) {
    // Полоса меряется в координатах корня, а кадр A ставится в координатах оверлея.
    var overlayStartPx by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayStartPx = it.positionInRoot().x }
    ) {
        if (scrub == null) return@Box
        val frameIndex = storyboardFrameIndex(scrub.positionSec, durationSec)
        val timeText = formatTime(scrub.positionSec.toInt())
        when (mode) {
            XSeekPreviewMode.FOLLOW -> SeekPreviewFollow(
                storyboard = storyboard,
                frameIndex = frameIndex,
                timeText = timeText,
                anchorX = seekPreviewAnchorX(
                    positionSec = scrub.positionSec,
                    durationSec = durationSec,
                    trackStartPx = scrub.trackStartPx,
                    trackWidthPx = scrub.trackWidthPx,
                    overlayStartPx = overlayStartPx,
                )
            )
            XSeekPreviewMode.FULLSCREEN -> SeekPreviewFullscreen(
                storyboard = storyboard,
                frameIndex = frameIndex,
                timeText = timeText,
                contentScale = if (videoFitMode == ScreenResize.FILL) ContentScale.Crop else ContentScale.Fit
            )
        }
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewOverlayPreview() {
    SeekPreviewOverlay(
        mode = XSeekPreviewMode.FOLLOW,
        storyboard = null,
        scrub = SeekScrub(positionSec = 97f, trackStartPx = 120f, trackWidthPx = 900f),
        durationSec = 805,
        videoFitMode = ScreenResize.FILL
    )
}
