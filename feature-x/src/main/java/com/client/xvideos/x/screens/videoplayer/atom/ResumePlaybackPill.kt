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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.ui.theme.XvideosTheme

private val PILL_CORNER = 20.dp
private val PILL_SHAPE = RoundedCornerShape(PILL_CORNER)
private val PILL_BUTTON_CORNER = 6.dp
private val PILL_BUTTON_SHAPE = RoundedCornerShape(PILL_BUTTON_CORNER)
private val PILL_BG_COLOR = Color(0xDD212121)
private val PILL_RESTART_COLOR = Color(0xFFFF5252)
private val PILL_FONT_SIZE = 13.sp
private val PILL_SHADOW_ELEVATION = 4.dp
private val PILL_TONAL_ELEVATION = 6.dp
private val PILL_ROW_HORIZONTAL_PADDING = 14.dp
private val PILL_ROW_VERTICAL_PADDING = 7.dp
private val PILL_SPACER_WIDTH = 10.dp
private val PILL_BUTTON_HORIZONTAL_PADDING = 4.dp
private val PILL_BUTTON_VERTICAL_PADDING = 2.dp
private const val BUTTON_RESTART_TEXT = "С начала"
private val COLOR_WHITE = Color.White
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically

private val PILL_TEXT_STYLE = TextStyle(
    color = COLOR_WHITE,
    fontSize = PILL_FONT_SIZE
)

private val PILL_RESTART_TEXT_STYLE = TextStyle(
    color = PILL_RESTART_COLOR,
    fontWeight = FontWeight.Bold,
    fontSize = PILL_FONT_SIZE
)

private val PILL_SURFACE_BASE_MODIFIER = Modifier.clip(PILL_SHAPE)
private val PILL_ROW_MODIFIER = Modifier.padding(
    horizontal = PILL_ROW_HORIZONTAL_PADDING,
    vertical = PILL_ROW_VERTICAL_PADDING
)
private val PILL_SPACER_MODIFIER = Modifier.width(PILL_SPACER_WIDTH)
private val PILL_BUTTON_FULL_MODIFIER = Modifier
    .clip(PILL_BUTTON_SHAPE)
    .padding(
        horizontal = PILL_BUTTON_HORIZONTAL_PADDING,
        vertical = PILL_BUTTON_VERTICAL_PADDING
    )

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
        modifier = if (modifier == Modifier) PILL_SURFACE_BASE_MODIFIER else modifier.then(PILL_SURFACE_BASE_MODIFIER),
        color = PILL_BG_COLOR,
        shadowElevation = PILL_SHADOW_ELEVATION,
        tonalElevation = PILL_TONAL_ELEVATION,
    ) {
        Row(
            modifier = PILL_ROW_MODIFIER,
            verticalAlignment = ROW_VERTICAL_ALIGNMENT
        ) {
            Text(
                text = text,
                style = PILL_TEXT_STYLE
            )
            Spacer(modifier = PILL_SPACER_MODIFIER)
            Text(
                text = BUTTON_RESTART_TEXT,
                style = PILL_RESTART_TEXT_STYLE,
                modifier = PILL_BUTTON_FULL_MODIFIER.clickable(onClick = onRestart)
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
