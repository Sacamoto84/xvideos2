package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.videoplayer.model.ScreenResize

@Composable
fun FitModeToggle(
    videoFitMode: ScreenResize,
    onToggleFitMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = if (videoFitMode == ScreenResize.FILL) "Fill" else "Fit",
        style = TextStyle(
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        ),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable(onClick = onToggleFitMode)
    )
}

@Preview
@Composable
private fun FitModeTogglePreview() {
    FitModeToggle(
        videoFitMode = ScreenResize.FIT,
        onToggleFitMode = {}
    )
}
