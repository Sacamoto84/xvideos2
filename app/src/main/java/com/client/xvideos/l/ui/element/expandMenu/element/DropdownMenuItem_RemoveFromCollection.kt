package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.style
import com.client.xvideos.l.theme.ThemeL.ExpandMenu.tintColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_RemoveFromCollection(item: PicsDetails? = null, onRefresh: (PicsDetails) -> Unit = {}, savedL: SavedL? = null, onDismiss: () -> Unit){

    val selectedCollection = savedL?.collection?.currentCollectionName

    DropdownMenuItem(
        leadingIcon = {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "",
                tint = tintColor
            )
        },
        text = { Text("Remove from Collection", style = style) },
        onClick = {
            if (item == null || savedL == null) return@DropdownMenuItem
            if (selectedCollection == null) {
                onDismiss.invoke()
                return@DropdownMenuItem
            }
            savedL.collection.remove(item, selectedCollection)
            onRefresh(item)

            onDismiss.invoke()
        }
    )
}
