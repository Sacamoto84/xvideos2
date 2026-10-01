package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.model.AlbumListFilter
import com.client.xvideos.l.net.AlbumListFilterGenreCountResponse
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

@Composable
fun AlbumListFilterTags(
    filter: AlbumListFilter,
    filterTagStateCount: List<AlbumListFilterGenreCountResponse>?,
    modifier: Modifier = Modifier,
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
        modifier = modifier
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

@Preview(showBackground = true, backgroundColor = 0xFF141418)
@Composable
private fun AlbumListFilterTagsPreview() {
    AlbumListFilterTags(
        filter = AlbumListFilter(),
        filterTagStateCount = emptyList(),
        onChange = {}
    )
}
