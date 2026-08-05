package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

@Composable
fun DropdownMenuItem_RemoveFromCollection(item: PicsDetails? = null, onRefresh: (PicsDetails) -> Unit = {}, savedL: SavedL? = null, onDismiss: () -> Unit){

    val selectedCollection = savedL?.collection?.currentCollectionName

    ExpandMenuActionItem(Icons.Default.RemoveCircleOutline, "Remove from Collection") {
        if (item == null || savedL == null) return@ExpandMenuActionItem
        if (selectedCollection == null) {
            onDismiss.invoke()
            return@ExpandMenuActionItem
        }
        savedL.collection.remove(item, selectedCollection)
        onRefresh(item)

        onDismiss.invoke()
    }
}
