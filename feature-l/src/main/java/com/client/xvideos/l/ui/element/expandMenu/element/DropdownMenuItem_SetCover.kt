package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.runtime.Composable
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

/**
 * Пункт «Сделать обложкой» для меню элемента коллекции.
 * Зовёт [SavedL.collection.setManualCover] (использует currentCollectionName);
 * тот пишет collection.json, шлёт SnackBar и обновляет список коллекций.
 */
@Composable
fun DropdownMenuItem_SetCover(item: PicsDetails? = null, savedL: SavedL? = null, onDismiss: () -> Unit) {
    ExpandMenuActionItem(Icons.Default.Wallpaper, "Сделать обложкой") {
        if (item == null || savedL == null) {
            onDismiss()
            return@ExpandMenuActionItem
        }
        if (savedL.collection.currentCollectionName == null) {
            onDismiss()
            return@ExpandMenuActionItem
        }
        savedL.collection.setManualCover(item)
        onDismiss()
    }
}
