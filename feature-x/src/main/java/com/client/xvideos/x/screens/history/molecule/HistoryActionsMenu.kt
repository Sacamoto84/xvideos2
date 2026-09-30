package com.client.xvideos.x.screens.history.molecule

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.theme.Theme

/**
 * Выпадающее меню действий для элемента истории (В избранное, Скачать, В галерею, Удалить).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryActionsMenu(
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onDownload: () -> Unit,
    onSaveToGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val onDismissMenu: () -> Unit = remember { { expanded = false } }
    val onToggleExpanded: (Boolean) -> Unit = remember { { isExpanded -> expanded = isExpanded } }

    val handleToggleFavorite = remember(onToggleFavorite) {
        {
            onToggleFavorite()
            expanded = false
        }
    }
    val handleDownload = remember(onDownload) {
        {
            onDownload()
            expanded = false
        }
    }
    val handleSaveToGallery = remember(onSaveToGallery) {
        {
            onSaveToGallery()
            expanded = false
        }
    }
    val handleDelete = remember(onDelete) {
        {
            onDelete()
            expanded = false
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onToggleExpanded,
        modifier = modifier,
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .offset(0.5.dp, 0.5.dp)
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Действия",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.ExpandMenu.backgroundColor,
        ) {
            ExpandMenuActionItem(
                if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                if (isFavorite) "Удалить из избранного" else "В избранное",
                onClick = handleToggleFavorite
            )

            ExpandMenuActionItem(
                Icons.Filled.ArrowCircleDown,
                "Скачать",
                onClick = handleDownload
            )

            ExpandMenuActionItem(
                Icons.Filled.SaveAlt,
                "В галерею",
                onClick = handleSaveToGallery
            )

            ExpandMenuActionItem(
                Icons.Filled.Delete,
                "Удалить из истории",
                onClick = handleDelete
            )
        }
    }
}

@Preview
@Composable
private fun HistoryActionsMenuPreview() {
    HistoryActionsMenu(
        isFavorite = false,
        onToggleFavorite = {},
        onDelete = {},
        onDownload = {},
        onSaveToGallery = {},
    )
}
