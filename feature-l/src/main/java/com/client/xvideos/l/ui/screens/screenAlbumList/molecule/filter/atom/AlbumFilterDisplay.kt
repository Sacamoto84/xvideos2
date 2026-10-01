package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.DataAlbumFilterDisplay
import com.client.xvideos.l.model.albumFilterDisplay
import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

private val UNIQUE_PRIMARY_LIST = albumFilterDisplay.map { it.primary }.distinct()

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
private fun AlbumFilterDisplayPreview() {
    var select by remember { mutableStateOf("date_trending") }
    AlbumFilterDisplay(select, onRequestApply = { select = it })
}

@Composable
fun AlbumFilterDisplay(
    startString: String,
    onRequestApply: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val list = albumFilterDisplay
    val palette = StyleGenresTags.Palette

    var showPrimaryDialog by remember { mutableStateOf(false) }
    var showSecondaryDialog by remember { mutableStateOf(false) }

    val onOpenPrimary = remember { { showPrimaryDialog = true } }
    val onOpenSecondary = remember { { showSecondaryDialog = true } }
    val onDismissPrimary = remember { { showPrimaryDialog = false } }
    val onDismissSecondary = remember { { showSecondaryDialog = false } }

    var selected by remember(startString) {
        mutableStateOf(list.firstOrNull { it.request == startString } ?: list.first())
    }

    val valueTextStyle = remember(palette.textPrimary) {
        Theme.L.Type.rowValue.copy(color = palette.textPrimary)
    }

    val onSelectPrimary: (String) -> Unit = remember(list, onRequestApply) {
        { primary ->
            val newSelected = list.firstOrNull { it.primary == primary } ?: list.first()
            selected = newSelected
            showPrimaryDialog = false
            onRequestApply(newSelected.request)
        }
    }
    val onSelectSecondary: (DataAlbumFilterDisplay) -> Unit = remember(onRequestApply) {
        { item ->
            selected = item
            showSecondaryDialog = false
            onRequestApply(item.request)
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterDropdownField(
            text = selected.primary,
            style = valueTextStyle,
            palette = palette,
            onClick = onOpenPrimary,
            modifier = Modifier.weight(1f)
        )

        FilterDropdownField(
            text = selected.secondary,
            style = valueTextStyle,
            palette = palette,
            onClick = onOpenSecondary,
            modifier = Modifier.weight(1f)
        )
    }

    // --- Диалог выбора Primary ---
    if (showPrimaryDialog) {
        AlbumFilterSelectDialog(
            title = "Sort by",
            items = UNIQUE_PRIMARY_LIST,
            selectedItem = selected.primary,
            itemTitle = { it },
            onDismiss = onDismissPrimary,
            onSelect = onSelectPrimary
        )
    }

    // --- Диалог выбора Secondary ---
    if (showSecondaryDialog) {
        val secondaryItems = remember(list, selected.primary) {
            list.filter { it.primary == selected.primary }
        }
        AlbumFilterSelectDialog(
            title = selected.primary,
            items = secondaryItems,
            selectedItem = selected,
            itemTitle = { it.secondary },
            onDismiss = onDismissSecondary,
            onSelect = onSelectSecondary
        )
    }
}
