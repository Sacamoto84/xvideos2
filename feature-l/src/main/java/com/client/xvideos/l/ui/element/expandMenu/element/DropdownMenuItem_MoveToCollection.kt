package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

@Composable
fun DropdownMenuItem_MoveToCollection(
    item: PicsDetails? = null,
    savedL: SavedL? = null,
    idAlbum: String = "",
    onDismiss: () -> Unit
) {
    ExpandMenuActionItem(Icons.AutoMirrored.Outlined.DriveFileMove, "Move to Collection") {
        if (item == null || savedL == null) return@ExpandMenuActionItem
        val itemWithAlbum = if (idAlbum.isNotEmpty()) {
            item.copy(album = idAlbum)
        } else {
            item
        }
        savedL.collection.beginMoveToCollection(itemWithAlbum)
        onDismiss.invoke()
    }
}

@Preview
@Composable
private fun DropdownMenuItem_MoveToCollectionPreview() {
    DropdownMenuItem_MoveToCollection(onDismiss = {})
}
