package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.common.theme.Theme

@Composable
internal fun SavedItemActionsMenu(
    actionIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
) = SavedItemActionsMenu { dismiss ->
    ExpandMenuActionItem(icon = actionIcon, text = actionLabel) {
        dismiss()
        onAction()
    }
}

@Composable
internal fun SavedItemActionsMenu(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    onAction: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier.padding(end = 4.dp)) {
        IconButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.size(48.dp)) {
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Действия",
                tint = Color.LightGray,
                modifier = iconModifier,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = Theme.ExpandMenu.backgroundColor
        ) {
            onAction { expanded = false }
        }
    }
}
