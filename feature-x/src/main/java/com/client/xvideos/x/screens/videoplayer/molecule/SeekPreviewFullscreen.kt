package com.client.xvideos.x.screens.videoplayer.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import com.client.xvideos.x.screens.videoplayer.atom.StoryboardFrame

/**
 * Вид B превью при перемотке: кадр раскадровки на весь плеер поверх видео
 * (кадр маленький, на весь экран он мыльный — так и задумано), время по центру.
 * Без картинки — только время, видео не перекрывается.
 */
@Composable
fun SeekPreviewFullscreen(
    storyboard: ImageBitmap?,
    frameIndex: Int,
    timeText: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (storyboard != null) {
            StoryboardFrame(
                storyboard = storyboard,
                frameIndex = frameIndex,
                contentScale = contentScale,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
        Text(
            text = timeText,
            style = TextStyle(
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color.Black, blurRadius = 8f)
            )
        )
    }
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewFullscreenPreview() {
    SeekPreviewFullscreen(
        storyboard = ImageBitmap(1920, 1080),
        frameIndex = 42,
        timeText = "5:38",
        contentScale = ContentScale.Crop
    )
}

@Preview(widthDp = 640, heightDp = 360)
@Composable
private fun SeekPreviewFullscreenNoStoryboardPreview() {
    SeekPreviewFullscreen(
        storyboard = null,
        frameIndex = 0,
        timeText = "5:38",
        contentScale = ContentScale.Crop
    )
}
