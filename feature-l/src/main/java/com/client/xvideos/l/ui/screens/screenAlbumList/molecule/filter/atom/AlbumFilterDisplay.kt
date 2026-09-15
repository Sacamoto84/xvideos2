package com.client.xvideos.l.ui.screens.screenAlbumList.molecule.filter.atom

import com.client.xvideos.common.theme.Theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.l.model.albumFilterDisplay

@Preview(showSystemUi = false, showBackground = false)
@Composable
fun PreviewAlbumFilterDisplay() {
    val select by remember { mutableStateOf("alpha_a") }
    AlbumFilterDisplay(select, onRequestApply = {})
}

@Composable
fun AlbumFilterDisplay(startString: String, onRequestApply: (String) -> Unit) {

    val list = albumFilterDisplay
    val palette = StyleGenresTags.Palette

    val uniquePrimaryList = remember { albumFilterDisplay.map { it.primary }.distinct() }

    var showPrimaryDialog by remember { mutableStateOf(false) }
    var showSecondaryDialog by remember { mutableStateOf(false) }

    var selected by remember(startString) {
        mutableStateOf(list.firstOrNull { it.request == startString } ?: list.first())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // --- Первое поле (Primary) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                .background(palette.field)
                .clickable { showPrimaryDialog = true }
                .padding(horizontal = 6.dp),
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
                    style = Theme.L.Type.rowValue.copy(color = palette.textPrimary)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // --- Второе поле (Secondary) ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.border, RoundedCornerShape(6.dp))
                .background(palette.field)
                .clickable { showSecondaryDialog = true }
                .padding(horizontal = 6.dp),
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
                    style = Theme.L.Type.rowValue.copy(color = palette.textPrimary)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = palette.textSecondary
                )
            }
        }

        Spacer(Modifier.width(4.dp))

        // --- Кнопка Apply ---
        Box(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.accent, RoundedCornerShape(6.dp))
                .background(palette.accentDark)
                .clickable(onClick = { onRequestApply(selected.request) }),
            contentAlignment = Alignment.Center
        ) {
            Text("Apply", color = Color.White, style = Theme.L.Type.button.copy(color = Color.White))
        }
    }

    // --- Диалог выбора Primary ---
    if (showPrimaryDialog) {
        AlbumFilterSelectDialog(
            title = "Sort by",
            items = uniquePrimaryList,
            selectedItem = selected.primary,
            itemTitle = { it },
            onDismiss = { showPrimaryDialog = false },
            onSelect = { primary ->
                selected = list.first { it.primary == primary }
                showPrimaryDialog = false
            }
        )
    }

    // --- Диалог выбора Secondary ---
    if (showSecondaryDialog) {
        val secondaryItems = list.filter { it.primary == selected.primary }
        AlbumFilterSelectDialog(
            title = selected.primary,
            items = secondaryItems,
            selectedItem = selected,
            itemTitle = { it.secondary },
            onDismiss = { showSecondaryDialog = false },
            onSelect = { item ->
                selected = item
                showSecondaryDialog = false
            }
        )
    }
}
