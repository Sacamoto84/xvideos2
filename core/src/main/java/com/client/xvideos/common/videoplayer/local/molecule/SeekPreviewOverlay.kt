package com.client.xvideos.common.videoplayer.local.molecule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.videoplayer.local.model.SeekScrub
import com.client.xvideos.common.videoplayer.local.model.seekPreviewAnchorX
import com.client.xvideos.common.videoplayer.local.model.storyboardFrameIndex

/**
 * Превью кадра при перемотке поверх плеера, пока палец ведёт ползунок
 * ([scrub] не `null`): маленький кадр над бегунком, время под ним. Вид «на
 * весь плеер» убран решением владельца от 21.10.2026 — кадр раскадровки
 * маленький и на весь экран выглядит плохо.
 */
@Composable
fun SeekPreviewOverlay(
    storyboard: ImageBitmap?,
    scrub: SeekScrub?,
    durationSec: Int,
    modifier: Modifier = Modifier,
) {
    // Полоса меряется в координатах корня, а кадр ставится в координатах оверлея.
    var overlayStartPx by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayStartPx = it.positionInRoot().x }
    ) {
        if (scrub == null) return@Box
        SeekPreviewFollow(
            storyboard = storyboard,
            frameIndex = storyboardFrameIndex(scrub.positionSec, durationSec),
            timeText = formatTime(scrub.positionSec.toInt()),
            anchorX = seekPreviewAnchorX(
                positionSec = scrub.positionSec,
                durationSec = durationSec,
                trackStartPx = scrub.trackStartPx,
                trackWidthPx = scrub.trackWidthPx,
                overlayStartPx = overlayStartPx,
            )
        )
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewOverlayPreview() {
    SeekPreviewOverlay(
        storyboard = null,
        scrub = SeekScrub(positionSec = 97f, trackStartPx = 120f, trackWidthPx = 900f),
        durationSec = 805
    )
}
