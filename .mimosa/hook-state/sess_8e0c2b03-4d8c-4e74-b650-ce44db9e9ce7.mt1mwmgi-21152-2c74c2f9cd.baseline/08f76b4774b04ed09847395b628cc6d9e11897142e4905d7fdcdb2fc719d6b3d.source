package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.runtime.Composable
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

@Composable
fun DropdownMenuItem_AddCollection(item: PicsDetails? = null, savedL: SavedL? = null, idAlbum: String = "", onDismiss: () -> Unit){
    ExpandMenuActionItem(Icons.Default.AddCircleOutline, "Add to Collection") {
        if (item == null || savedL == null) return@ExpandMenuActionItem
        // Update item with album info if provided
        val itemWithAlbum = if (idAlbum.isNotEmpty()) {
            item.copy(album = idAlbum)
        } else {
            item
        }
        savedL.collection.beginAddToCollection(itemWithAlbum)
        onDismiss.invoke()
    }
}
