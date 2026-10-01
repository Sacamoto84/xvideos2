package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

@Composable
fun ActiveTagsChips(
    tagsPlus: List<String>,
    tagsMinus: List<String>,
    onRemovePlus: (String) -> Unit,
    onRemoveMinus: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = StyleGenresTags.Palette
    val chipTextStyle = remember(Theme.L.Type.bodyLarge) {
        Theme.L.Type.bodyLarge.copy(
            color = StyleGenresTags.colorSelectTextItem,
            fontWeight = FontWeight.Bold
        )
    }
    val excludedChipTextStyle = remember(Theme.L.Type.bodyLarge) {
        Theme.L.Type.bodyLarge.copy(
            color = StyleGenresTags.colorExcludedTextItem,
            fontWeight = FontWeight.Bold
        )
    }

    Spacer(modifier = Modifier.height(6.dp))
    Column(modifier = modifier.fillMaxWidth()) {
        tagsPlus.forEach { item ->
            key(item) {
                Row(
                    modifier = Modifier
                        .then(StyleGenresTags.modifierSelectTextItem)
                        .clickable { onRemovePlus(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item,
                        color = StyleGenresTags.colorSelectTextItem,
                        style = chipTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = palette.selectedBorder,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        tagsMinus.forEach { item ->
            key(item) {
                val annotatedText = remember(item, palette.excludedBorder) {
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) {
                            append("NOT")
                        }
                        append(" $item")
                    }
                }
                Row(
                    modifier = Modifier
                        .then(StyleGenresTags.modifierExcludedTextItem)
                        .clickable { onRemoveMinus(item) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = annotatedText,
                        color = StyleGenresTags.colorExcludedTextItem,
                        style = excludedChipTextStyle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = palette.excludedBorder,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ActiveTagsChipsPreview() {
    ActiveTagsChips(
        tagsPlus = listOf("Cosplay", "Anime"),
        tagsMinus = listOf("3D"),
        onRemovePlus = {},
        onRemoveMinus = {}
    )
}
