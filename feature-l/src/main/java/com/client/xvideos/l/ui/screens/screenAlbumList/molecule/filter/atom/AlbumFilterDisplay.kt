package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.l.model.albumFilterDisplay

private val UNIQUE_PRIMARY_LIST = albumFilterDisplay.map { it.primary }.distinct()
private val filterFieldShape = RoundedCornerShape(6.dp)

@Preview(showBackground = true, backgroundColor = 0xFF1C1C1C)
@Composable
fun PreviewAlbumFilterDisplay() {
    var select by remember { mutableStateOf("date_trending") }
    AlbumFilterDisplay(select, onRequestApply = { select = it })
}

@Composable
fun AlbumFilterDisplay(startString: String, onRequestApply: (String) -> Unit) {

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

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Первое поле (Primary) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(filterFieldShape)
                .border(1.dp, palette.border, filterFieldShape)
                .background(palette.field)
                .clickable(onClick = onOpenPrimary)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.primary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = valueTextStyle
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }

        // --- Второе поле (Secondary) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(filterFieldShape)
                .border(1.dp, palette.border, filterFieldShape)
                .background(palette.field)
                .clickable(onClick = onOpenSecondary)
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected.secondary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = valueTextStyle
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }
    }

    // --- Диалог выбора Primary ---
    if (showPrimaryDialog) {
        AlbumFilterSelectDialog(
            title = "Sort by",
            items = UNIQUE_PRIMARY_LIST,
            selectedItem = selected.primary,
            itemTitle = { it },
            onDismiss = onDismissPrimary,
            onSelect = { primary ->
                val newSelected = list.firstOrNull { it.primary == primary } ?: list.first()
                selected = newSelected
                showPrimaryDialog = false
                onRequestApply(newSelected.request)
            }
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
            onSelect = { item ->
                selected = item
                showSecondaryDialog = false
                onRequestApply(item.request)
            }
        )
    }
}
