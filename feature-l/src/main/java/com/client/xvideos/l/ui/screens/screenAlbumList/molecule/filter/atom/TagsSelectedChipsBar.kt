package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.filter.style.StyleGenresTags

@Composable
fun TagsSelectedChipsBar(
    tagsPlus: List<String>,
    tagsMinus: List<String>,
    onRemovePlus: (String) -> Unit,
    onRemoveMinus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val chipShape = RoundedCornerShape(6.dp)
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        itemsIndexed(tagsPlus, key = { index, tag -> "plus_${tag}#$index" }) { _, tag ->
            Row(
                modifier = Modifier
                    .clip(chipShape)
                    .border(1.dp, palette.selectedBorder, chipShape)
                    .background(palette.selected)
                    .clickable { onRemovePlus(tag) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "+ $tag",
                    color = palette.selectedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.selectedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        itemsIndexed(tagsMinus, key = { index, tag -> "minus_${tag}#$index" }) { _, tag ->
            Row(
                modifier = Modifier
                    .clip(chipShape)
                    .border(1.dp, palette.excludedBorder, chipShape)
                    .background(palette.excluded)
                    .clickable { onRemoveMinus(tag) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val annotatedTag = buildAnnotatedString {
                    withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) { append("NOT") }
                    append(" $tag")
                }
                Text(
                    text = annotatedTag,
                    color = palette.excludedText,
                    style = Theme.L.Type.rowValue.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint = palette.excludedBorder,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Preview
@Composable
private fun TagsSelectedChipsBarPreview() {
    TagsSelectedChipsBar(
        tagsPlus = listOf("Cosplay", "Anime"),
        tagsMinus = listOf("3D"),
        onRemovePlus = {},
        onRemoveMinus = {}
    )
}
