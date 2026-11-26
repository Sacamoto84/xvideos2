package com.client.xvideos.r.ui.explorer.tab.saved.tab.molecule

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.ui.explorer.tab.saved.tab.atom.SavedItemActionsMenu

@Composable
internal fun DownloadItemActions(
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
) {
    SavedItemActionsMenu(modifier = modifier, iconModifier = iconModifier) { dismiss ->
        ExpandMenuActionItem(Icons.Outlined.Fullscreen, "Открыть во весь экран") {
            dismiss()
            onOpen()
        }
        ExpandMenuActionItem(Icons.Outlined.Share, "Поделиться") {
            dismiss()
            onShare()
        }
        ExpandMenuActionItem(Icons.Outlined.Delete, "Удалить загрузку") {
            dismiss()
            onDelete()
        }
    }
}

@Preview
@Composable
private fun DownloadItemActionsPreview() {
    DownloadItemActions(onOpen = {}, onShare = {}, onDelete = {})
}
