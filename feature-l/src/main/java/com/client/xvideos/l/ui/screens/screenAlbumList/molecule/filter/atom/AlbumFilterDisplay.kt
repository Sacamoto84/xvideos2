package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.style.StyleGenresTags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.DataAlbumFilterDisplay
import com.client.xvideos.l.model.albumFilterDisplay

private val UNIQUE_PRIMARY_LIST = albumFilterDisplay.map { it.primary }.distinct()
private val FIELD_SHAPE = RoundedCornerShape(6.dp)

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
fun PreviewAlbumFilterDisplay() {
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

@Composable
private fun FilterDropdownField(
    text: String,
    style: TextStyle,
    palette: StyleGenresTags.Palette,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(FIELD_SHAPE)
            .border(1.dp, palette.border, FIELD_SHAPE)
            .background(palette.field)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = text,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = style
            )
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = palette.textSecondary
            )
        }
    }
}
