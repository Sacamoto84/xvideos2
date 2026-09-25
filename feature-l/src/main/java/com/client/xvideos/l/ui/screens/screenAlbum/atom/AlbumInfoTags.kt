package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.capitalizeEachWord
import com.client.xvideos.l.model.Tag
import com.client.xvideos.ui.theme.XvideosTheme

private val TAG_CHIP_CORNER = 4.dp
private val TAG_CHIP_SHAPE = RoundedCornerShape(TAG_CHIP_CORNER)
private val TAG_BORDER_WIDTH = 1.dp
private val TAG_VERTICAL_PADDING = 2.dp
private val TAG_CONTENT_PADDING = 4.dp
private val TAG_FONT_SIZE = 14.sp

private val TAG_CHIP_BASE_MODIFIER = Modifier
    .padding(vertical = TAG_VERTICAL_PADDING)
    .clip(TAG_CHIP_SHAPE)

private val TAG_CHIP_CONTENT_PADDING_MODIFIER = Modifier.padding(TAG_CONTENT_PADDING)
private val FLOW_ROW_VERTICAL_ARRANGEMENT = Arrangement.Center

@Composable
fun AlbumInfoTags(
    tags: () -> (List<Tag>),
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tagTextStyle = remember(Theme.L.Type.caption, Theme.L.textColor) {
        Theme.L.Type.caption.copy(color = Theme.L.textColor, fontSize = TAG_FONT_SIZE)
    }

    val tagList = tags()

    FlowRow(
        modifier = modifier,
        verticalArrangement = FLOW_ROW_VERTICAL_ARRANGEMENT
    ) {
        tagList.forEach { tag ->
            key(tag.id) {
                val label = remember(tag.text, tag.count) {
                    "${tag.text.capitalizeEachWord()} (${tag.count})"
                }
                val handleClick = remember(tag.text, onClick) { { onClick(tag.text) } }

                AlbumTagChip(
                    label = label,
                    onClick = handleClick,
                    textStyle = tagTextStyle
                )
            }
        }
    }
}

@Composable
private fun AlbumTagChip(
    label: String,
    onClick: () -> Unit,
    textStyle: androidx.compose.ui.text.TextStyle,
    modifier: Modifier = Modifier,
) {
    val borderColor = Theme.L.secondaryColor
    val chipBorderModifier = remember(borderColor) {
        TAG_CHIP_BASE_MODIFIER.border(TAG_BORDER_WIDTH, borderColor, TAG_CHIP_SHAPE)
    }
    val chipModifier = if (modifier == Modifier) chipBorderModifier else modifier.then(chipBorderModifier)

    Text(
        text = label,
        modifier = chipModifier
            .clickable(onClick = onClick)
            .then(TAG_CHIP_CONTENT_PADDING_MODIFIER),
        color = Theme.L.textColor,
        style = textStyle
    )
}

@Preview
@Composable
fun AlbumInfoTagsPreview() {
    XvideosTheme {
        AlbumInfoTags(
            tags = {
                listOf(
                    Tag(id = "tag1", category = "general", text = "nature photography", url = "url1", count = 150),
                    Tag(id = "tag2", category = "location", text = "mountain view", url = "url2", count = 75)
                )
            },
            onClick = {}
        )
    }
}
