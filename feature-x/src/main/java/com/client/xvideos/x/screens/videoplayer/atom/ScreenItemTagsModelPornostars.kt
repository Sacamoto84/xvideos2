package com.client.xvideos.x.screens.videoplayer.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PORNOSTAR_TAG_CORNER = 6.dp
private val PORNOSTAR_TAG_SHAPE = RoundedCornerShape(PORNOSTAR_TAG_CORNER)
private val COUNT_BADGE_CORNER = 4.dp
private val COUNT_BADGE_SHAPE = RoundedCornerShape(COUNT_BADGE_CORNER)
private val COUNT_BADGE_BG = Color(0x33000000)

private val TAG_HEIGHT = 28.dp
private val TAG_PADDING_HORIZONTAL = 3.dp
private val TAG_PADDING_VERTICAL = 2.dp
private val TEXT_PADDING_START = 8.dp
private val TEXT_PADDING_END_WITH_COUNT = 4.dp
private val TEXT_PADDING_END_NO_COUNT = 8.dp
private val BADGE_HEIGHT = 20.dp
private val BADGE_PADDING_END = 4.dp
private val BADGE_PADDING_HORIZONTAL = 5.dp

private val TEXT_FONT_SIZE = 13.sp
private val COUNT_FONT_SIZE = 11.sp

private val PORNOSTAR_TEXT_STYLE = TextStyle(
    color = Color.White,
    fontSize = TEXT_FONT_SIZE,
    fontWeight = FontWeight.Medium
)

private val COUNT_TEXT_STYLE = TextStyle(
    color = Color.White,
    fontSize = COUNT_FONT_SIZE,
    fontFamily = FontFamily.SansSerif,
    textAlign = TextAlign.Center,
    fontWeight = FontWeight.SemiBold
)

private val TAG_BASE_MODIFIER = Modifier
    .padding(horizontal = TAG_PADDING_HORIZONTAL, vertical = TAG_PADDING_VERTICAL)
    .height(TAG_HEIGHT)
    .clip(PORNOSTAR_TAG_SHAPE)

private val COUNT_BADGE_MODIFIER = Modifier
    .padding(end = BADGE_PADDING_END)
    .height(BADGE_HEIGHT)
    .clip(COUNT_BADGE_SHAPE)
    .background(COUNT_BADGE_BG)
    .padding(horizontal = BADGE_PADDING_HORIZONTAL)

private val TEXT_MODIFIER_WITH_COUNT = Modifier.padding(start = TEXT_PADDING_START, end = TEXT_PADDING_END_WITH_COUNT)
private val TEXT_MODIFIER_NO_COUNT = Modifier.padding(start = TEXT_PADDING_START, end = TEXT_PADDING_END_NO_COUNT)
private val ROW_VERTICAL_ALIGNMENT = Alignment.CenterVertically
private val BADGE_BOX_ALIGNMENT = Alignment.Center

/**
 * ## Отображение текста канала и порноактрисы и показ количества подписок на них
 */
@Composable
fun ScreenItemTagsModelPornostars(
    text: String,
    color: Color,
    count: String,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hasCount = remember(count) { count.isNotBlank() }
    val textModifier = if (hasCount) TEXT_MODIFIER_WITH_COUNT else TEXT_MODIFIER_NO_COUNT
    val baseModifier = modifier
        .then(TAG_BASE_MODIFIER)
        .background(color)
    val rowModifier = if (onClick != null) baseModifier.clickable(onClick = onClick) else baseModifier

    Row(
        modifier = rowModifier,
        verticalAlignment = ROW_VERTICAL_ALIGNMENT
    ) {
        Text(
            text = text,
            modifier = textModifier,
            style = PORNOSTAR_TEXT_STYLE
        )

        if (hasCount) {
            Box(
                modifier = COUNT_BADGE_MODIFIER,
                contentAlignment = BADGE_BOX_ALIGNMENT
            ) {
                Text(
                    text = count,
                    style = COUNT_TEXT_STYLE
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ScreenItemTagsModelPornostarsWithCountPreview() {
    ScreenItemTagsModelPornostars(
        text = "Sweetie Fox",
        color = Color(0xFFE91E63),
        count = "120K",
        onClick = {}
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun ScreenItemTagsModelPornostarsWithoutCountPreview() {
    ScreenItemTagsModelPornostars(
        text = "Verified Channel",
        color = Color(0xFF3F51B5),
        count = "",
        onClick = {}
    )
}

