package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse

@Composable
fun AlbumListFilterTags(
    filter: AlbumListFilter,
    filterTagStateCount: List<AlbumListFilterGenreCountResponse>?,
    onChange: (AlbumListFilter) -> Unit
) {
    val tagCountItems = filterTagStateCount.orEmpty()
    val tagsPlus = remember(filter.tagPlus) { filter.tagPlus.distinct() }
    val tagsMinus = remember(filter.tagMinus) { filter.tagMinus.distinct() }
    val tagsCorrect = rememberSelectableTags(tagCountItems, tagsPlus, tagsMinus)
    val tagCountByTerm = rememberTagCountIndex(tagCountItems)
    val palette = StyleGenresTags.Palette

    var showDialog by remember { mutableStateOf(false) }
    val totalSelected = tagsPlus.size + tagsMinus.size
    val selectorText = if (totalSelected == 0) "Any" else "$totalSelected selected"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface)
    ) {
        TagsTriggerRow(
            selectorText = selectorText,
            totalSelected = totalSelected,
            onClick = { showDialog = true }
        )

        if (tagsPlus.isNotEmpty() || tagsMinus.isNotEmpty()) {
            ActiveTagsChips(
                tagsPlus = tagsPlus,
                tagsMinus = tagsMinus,
                onRemovePlus = { item -> onChange(filter.copy(tagPlus = tagsPlus - item)) },
                onRemoveMinus = { item -> onChange(filter.copy(tagMinus = tagsMinus - item)) }
            )
        }
    }

    if (showDialog) {
        AlbumFilterTagsDialog(
            tagsPlus = tagsPlus,
            tagsMinus = tagsMinus,
            selectableTags = tagsCorrect,
            tagCountByTerm = tagCountByTerm,
            onAddPlus = { item -> onChange(filter.copy(tagPlus = tagsPlus + item)) },
            onAddMinus = { item -> onChange(filter.copy(tagMinus = tagsMinus + item)) },
            onRemovePlus = { item -> onChange(filter.copy(tagPlus = tagsPlus - item)) },
            onRemoveMinus = { item -> onChange(filter.copy(tagMinus = tagsMinus - item)) },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun TagsTriggerRow(
    selectorText: String,
    totalSelected: Int,
    onClick: () -> Unit
) {
    val palette = StyleGenresTags.Palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Tags",
            style = Theme.L.Type.rowTitle.copy(
                fontWeight = FontWeight.Bold,
                color = palette.textPrimary
            )
        )

        Row(
            modifier = Modifier
                .widthIn(min = 160.dp, max = 220.dp)
                .height(43.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                .background(palette.field)
                .clickable { onClick() }
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectorText,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = Theme.L.Type.rowTitle.copy(
                    color = if (totalSelected == 0) palette.textPrimary else palette.selectedText,
                    fontWeight = FontWeight.Bold
                )
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}

@Composable
private fun ActiveTagsChips(
    tagsPlus: List<String>,
    tagsMinus: List<String>,
    onRemovePlus: (String) -> Unit,
    onRemoveMinus: (String) -> Unit
) {
    val palette = StyleGenresTags.Palette
    Spacer(modifier = Modifier.height(6.dp))
    Column(modifier = Modifier.fillMaxWidth()) {
        tagsPlus.forEach { item ->
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
                    style = Theme.L.Type.bodyLarge.copy(
                        color = StyleGenresTags.colorSelectTextItem,
                        fontWeight = FontWeight.Bold
                    ),
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

        tagsMinus.forEach { item ->
            val annotatedText = buildAnnotatedString {
                withStyle(SpanStyle(color = palette.excludedBorder, textDecoration = TextDecoration.Underline)) {
                    append("NOT")
                }
                append(" $item")
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
                    style = Theme.L.Type.bodyLarge.copy(
                        color = StyleGenresTags.colorExcludedTextItem,
                        fontWeight = FontWeight.Bold
                    ),
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

@Composable
private fun rememberSelectableTags(
    tagCountItems: List<AlbumListFilterGenreCountResponse>,
    tagsPlus: List<String>,
    tagsMinus: List<String>
): List<String> = remember(tagCountItems, tagsPlus, tagsMinus) {
    tagCountItems.map { it.term }.toSet()
        .minus(tagsPlus.toSet())
        .minus(tagsMinus.toSet())
        .toList()
}

@Composable
private fun rememberTagCountIndex(
    tagCountItems: List<AlbumListFilterGenreCountResponse>
): Map<String, Int> = remember(tagCountItems) {
    tagCountItems.associate { it.term to it.count }
}
