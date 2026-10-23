package com.client.xvideos.l.ui.screens.screenAlbum.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.common.util.capitalizeEachWord
import com.client.xvideos.l.model.Tag
import com.client.xvideos.l.ui.screens.screenAlbum.model.groupAlbumTags
import com.client.xvideos.l.ui.screens.screenAlbum.model.textWithoutCategory
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Теги альбома рядами по категориям: «Character: [Frieren (260)]», ниже —
 * теги без категории под заголовком «Tags:». Заголовок ряда оформлен как «Audiences:».
 */
@Composable
fun AlbumInfoTags(
    tags: () -> (List<Tag>),
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val headerStyle = remember(Theme.L.Type.rowTitle) {
        Theme.L.Type.rowTitle.copy(fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
    }
    val tagTextStyle = remember(Theme.L.Type.caption, Theme.L.textColor) {
        Theme.L.Type.caption.copy(color = Theme.L.textColor, fontSize = 14.sp)
    }

    val tagList = tags()
    val groups = remember(tagList) { groupAlbumTags(tagList) }

    Column(modifier = modifier) {
        groups.forEach { group ->
            key(group.category) {
                FlowRow(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "${group.title}: ",
                        color = Theme.L.textColor,
                        style = headerStyle,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    group.tags.forEach { tag ->
                        key(tag.id) {
                            val label = remember(tag.text, tag.category, tag.count) {
                                "${tag.textWithoutCategory().capitalizeEachWord()} (${tag.count})"
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
        }
    }
}

@Preview
@Composable
private fun AlbumInfoTagsPreview() {
    XvideosTheme {
        AlbumInfoTags(
            tags = {
                listOf(
                    Tag(id = "tag1", category = "Character", text = "Character: frieren", url = "url1", count = 260),
                    Tag(id = "tag2", category = "Ethnicity", text = "Ethnicity: elf", url = "url2", count = 163),
                    Tag(id = "tag3", text = "nature photography", url = "url3", count = 150)
                )
            },
            onClick = {}
        )
    }
}
