package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.ui.theme.XvideosTheme

private val PILL_SHAPE = RoundedCornerShape(20.dp)
private val PILL_BUTTON_SHAPE = RoundedCornerShape(6.dp)
private val PILL_BG_COLOR = Color(0xDD212121)
private val PILL_RESTART_COLOR = Color(0xFFFF5252)
private val PILL_FONT_SIZE = 13.sp
private val PILL_SHADOW_ELEVATION = 4.dp
private val PILL_TONAL_ELEVATION = 6.dp
private const val BUTTON_RESTART_TEXT = "С начала"

/**
 * Плашка с уведомлением о возобновлении воспроизведения и кнопкой «С начала».
 */
@Composable
fun ResumePlaybackPill(
    text: String,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clip(PILL_SHAPE),
        color = PILL_BG_COLOR,
        shadowElevation = PILL_SHADOW_ELEVATION,
        tonalElevation = PILL_TONAL_ELEVATION,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = PILL_FONT_SIZE,
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = BUTTON_RESTART_TEXT,
                color = PILL_RESTART_COLOR,
                fontWeight = FontWeight.Bold,
                fontSize = PILL_FONT_SIZE,
                modifier = Modifier
                    .clip(PILL_BUTTON_SHAPE)
                    .clickable(onClick = onRestart)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    }
}

@Preview
@Composable
private fun ResumePlaybackPillPreview() {
    XvideosTheme(darkTheme = true) {
        ResumePlaybackPill(
            text = "Продолжить с 05:20",
            onRestart = {}
        )
    }
}
