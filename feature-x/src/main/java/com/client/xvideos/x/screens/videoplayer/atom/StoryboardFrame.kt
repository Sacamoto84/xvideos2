package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.screens.videoplayer.model.storyboardFrameRect
import kotlin.math.roundToInt

/**
 * Один кадр [frameIndex] из картинки раскадровки [storyboard], вписанный в свою
 * область по [contentScale] и по центру. Картинка меньше сетки — ничего не рисует.
 */
@Composable
fun StoryboardFrame(
    storyboard: ImageBitmap,
    frameIndex: Int,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val frame = remember(storyboard, frameIndex) {
        storyboardFrameRect(frameIndex, storyboard.width, storyboard.height)
    }
    Canvas(modifier = modifier.clipToBounds()) {
        val source = frame ?: return@Canvas
        val sourceSize = Size(source.width.toFloat(), source.height.toFloat())
        val scale = contentScale.computeScaleFactor(sourceSize, size)
        val width = sourceSize.width * scale.scaleX
        val height = sourceSize.height * scale.scaleY
        drawImage(
            image = storyboard,
            srcOffset = IntOffset(source.left, source.top),
            srcSize = IntSize(source.width, source.height),
            dstOffset = IntOffset(((size.width - width) / 2f).roundToInt(), ((size.height - height) / 2f).roundToInt()),
            dstSize = IntSize(width.roundToInt(), height.roundToInt()),
        )
    }
}

@Preview
@Composable
private fun StoryboardFramePreview() {
    StoryboardFrame(
        storyboard = ImageBitmap(1920, 1080),
        frameIndex = 0,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(160.dp, 90.dp)
    )
}
