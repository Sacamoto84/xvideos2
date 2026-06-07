package com.client.xvideos.r.common.expand_menu_video

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DropdownMenuItem_RemoveFromCollection(
    item: GifsInfo? = null,
    onRefresh: () -> Unit,
    savedRed: () -> SavedRed,
    onDismiss: () -> Unit
) {
    val selectedCollection by savedRed.invoke().collections.selectedCollection.collectAsStateWithLifecycle()

    DropdownMenuItem_RemoveFromCollection(
        item = item,
        selectedCollection = selectedCollection,
        onRemove = { itemId, collectionName ->
            savedRed.invoke().collections.deleteItemFromCollection(itemId, collectionName)
        },
        onRefresh = onRefresh,
        onDismiss = onDismiss
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_RemoveFromCollection(
    item: GifsInfo?,
    selectedCollection: String?,
    onRemove: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                Icons.Default.RemoveCircleOutline,
                contentDescription = "",
                tint = Theme.L.ExpandMenu.tintColor
            )
        },
        text = { Text("Remove from Collection", style = Theme.L.ExpandMenu.style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            if (selectedCollection == null) {
                onDismiss.invoke()
                return@DropdownMenuItem
            }
            onRemove(item.id, selectedCollection)
            onRefresh.invoke()

            onDismiss.invoke()
        },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_RemoveFromCollectionPreview() {
    XvideosTheme {
        DropdownMenuItem_RemoveFromCollection(
            item = GifsInfo(id = "sample_id"),
            selectedCollection = "Sample Collection",
            onRemove = { _, _ -> },
            onRefresh = {},
            onDismiss = {}
        )
    }
}