package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.common.theme.Theme.ExpandMenu.style
import com.client.xvideos.common.theme.Theme.ExpandMenu.tintColor
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails

/**
 * Пункт «Сделать обложкой» для меню элемента коллекции.
 * Зовёт [SavedL.collection.setManualCover] (использует currentCollectionName);
 * тот пишет collection.json, шлёт SnackBar и обновляет список коллекций.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_SetCover(item: PicsDetails? = null, savedL: SavedL? = null, onDismiss: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(Icons.Default.Wallpaper, contentDescription = "", tint = tintColor)
        },
        text = { Text("Сделать обложкой", style = style) },
        onClick = {
            if (item == null || savedL == null) {
                onDismiss()
                return@DropdownMenuItem
            }
            if (savedL.collection.currentCollectionName == null) {
                onDismiss()
                return@DropdownMenuItem
            }
            savedL.collection.setManualCover(item)
            onDismiss()
        },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}
