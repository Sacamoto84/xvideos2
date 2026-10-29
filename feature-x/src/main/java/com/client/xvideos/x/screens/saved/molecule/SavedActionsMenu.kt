package com.client.xvideos.x.screens.saved.molecule

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
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
 * «⋮» на карточке «Сохранённого»: в галерею, P2P и удалить — как меню
 * карточки «Избранного». Каждый пункт закрывает меню.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedActionsMenu(
    onSaveToGallery: () -> Unit,
    onShareP2p: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    val onExpandedChange = remember { { isExpanded: Boolean -> expanded = isExpanded } }
    val onDismissMenu = remember { { expanded = false } }
    val handleSaveToGallery = remember(onSaveToGallery) {
        {
            onSaveToGallery()
            expanded = false
        }
    }
    val handleShareP2p = remember(onShareP2p) {
        {
            onShareP2p()
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
        onExpandedChange = onExpandedChange,
        modifier = modifier
    ) {
        IconButton(
            modifier = Modifier
                .size(48.dp)
                .menuAnchor(ExposedDropdownMenuAnchorType.SecondaryEditable),
            onClick = {}
        ) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = "Действия",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .offset(0.5.dp, 0.5.dp)
            )
            Icon(
                Icons.Default.MoreVert,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismissMenu,
            modifier = Modifier.width(IntrinsicSize.Min),
            containerColor = Theme.ExpandMenu.backgroundColor
        ) {
            ExpandMenuActionItem(Icons.Filled.SaveAlt, "В галерею", onClick = handleSaveToGallery)
            ExpandMenuActionItem(Icons.Filled.Share, "P2P", onClick = handleShareP2p)
            ExpandMenuActionItem(Icons.Filled.Delete, "Удалить", onClick = handleDelete)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF262626)
@Composable
private fun SavedActionsMenuPreview() {
    SavedActionsMenu(
        onSaveToGallery = {},
        onShareP2p = {},
        onDelete = {}
    )
}
