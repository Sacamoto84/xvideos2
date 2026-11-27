package com.client.xvideos.r.ui.explorer.tab.saved.tab.atom

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

@Composable
internal fun SavedSingleActionMenu(
    actionIcon: ImageVector,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) = SavedItemActionsMenu(modifier = modifier) { dismiss ->
    ExpandMenuActionItem(icon = actionIcon, text = actionLabel) {
        dismiss()
        onAction()
    }
}

@Preview
@Composable
private fun SavedSingleActionMenuPreview() {
    SavedSingleActionMenu(
        actionIcon = Icons.Filled.Delete,
        actionLabel = "Удалить",
        onAction = {},
    )
}
