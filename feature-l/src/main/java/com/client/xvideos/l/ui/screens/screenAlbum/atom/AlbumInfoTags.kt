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

private val TAG_CHIP_SHAPE = RoundedCornerShape(4.dp)

@Composable
fun AlbumInfoTags(tags: () -> (List<Tag>), onClick: (String) -> Unit) {
    val tagTextStyle = remember(Theme.L.Type.caption, Theme.L.textColor) {
        Theme.L.Type.caption.copy(color = Theme.L.textColor, fontSize = 14.sp)
    }

    val tagList = tags()

    FlowRow(verticalArrangement = Arrangement.Center) {
        tagList.forEach { tag ->
            key(tag.id) {
                val label = remember(tag.text, tag.count) {
                    "${tag.text.capitalizeEachWord()} (${tag.count})"
                }
                val handleClick = remember(tag.text, onClick) { { onClick(tag.text) } }

                Text(
                    text = label,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .border(1.dp, Theme.L.secondaryColor, TAG_CHIP_SHAPE)
                        .clip(TAG_CHIP_SHAPE)
                        .clickable(onClick = handleClick)
                        .padding(4.dp),
                    color = Theme.L.textColor,
                    style = tagTextStyle
                )
            }
        }
    }
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
