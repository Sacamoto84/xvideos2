package com.client.xvideos.l.ui.element.expandMenu.element

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.common.theme.Theme.ExpandMenu.style
import com.client.xvideos.common.theme.Theme.ExpandMenu.tintColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_AddCollection(item: PicsDetails? = null, savedL: SavedL? = null, idAlbum: String = "", onDismiss: () -> Unit){
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                Icons.Default.AddCircleOutline,
                contentDescription = "",
                tint = tintColor
            )
        },
        text = { Text("Add to Collection", style = style) },
        onClick = {
            if (item == null || savedL == null) return@DropdownMenuItem
            // Update item with album info if provided
            val itemWithAlbum = if (idAlbum.isNotEmpty()) {
                item.copy(album = idAlbum)
            } else {
                item
            }
            savedL.collection.beginAddToCollection(itemWithAlbum)
            onDismiss.invoke()
        }
    )
}
