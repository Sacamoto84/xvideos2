package com.client.xvideos.common.videoplayer.local.molecule

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.local.atom.StoryboardFrame
import com.client.xvideos.common.videoplayer.local.model.seekPreviewFollowLeft
import kotlin.math.roundToInt

private val FRAME_SHAPE = RoundedCornerShape(8.dp)

/**
 * Вид A превью при перемотке: маленький кадр над полосой, центр — над бегунком
 * [anchorX] (px в координатах этой области), у краёв кадр упирается. Время под
 * кадром. Без картинки раскадровки — только время в той же точке.
 */
@Composable
fun SeekPreviewFollow(
    storyboard: ImageBitmap?,
    frameIndex: Int,
    timeText: String,
    anchorX: Float,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        // Размер — от ширины области плеера: на раскрытом складном кадр крупнее.
        // Нижняя граница 180dp: в портрете телефона 22% ширины — около 87dp, и
        // кадр был слишком мелким (владелец, 21.10.2026). На весь экран 22% и так
        // больше 180dp, там размер не меняется.
        val frameWidth = (maxWidth * 0.22f).coerceIn(180.dp, 240.dp)
        val density = LocalDensity.current
        val left = with(density) {
            seekPreviewFollowLeft(anchorX, frameWidth.toPx(), maxWidth.toPx(), 8.dp.toPx())
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset { IntOffset(left.roundToInt(), 0) }
                .padding(bottom = 44.dp)
                .width(frameWidth)
        ) {
            if (storyboard != null) {
                StoryboardFrame(
                    storyboard = storyboard,
                    frameIndex = frameIndex,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(frameWidth, frameWidth * 9f / 16f)
                        .clip(FRAME_SHAPE)
                        .border(2.dp, Color.White, FRAME_SHAPE)
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = timeText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = Shadow(color = Color.Black, blurRadius = 6f)
                )
            )
        }
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewFollowPreview() {
    SeekPreviewFollow(
        storyboard = ImageBitmap(1920, 1080),
        frameIndex = 12,
        timeText = "1:37",
        anchorX = 300f
    )
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewFollowNoStoryboardPreview() {
    SeekPreviewFollow(
        storyboard = null,
        frameIndex = 0,
        timeText = "1:37",
        anchorX = 300f
    )
}
