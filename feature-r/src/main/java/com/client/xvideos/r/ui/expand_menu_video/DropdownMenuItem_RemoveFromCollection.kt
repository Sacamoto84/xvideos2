package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
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

@Composable
fun DropdownMenuItem_RemoveFromCollection(
    item: GifsInfo?,
    selectedCollection: String?,
    onRemove: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    ExpandMenuActionItem(Icons.Default.RemoveCircleOutline, "Remove from Collection") {
        if (item == null) return@ExpandMenuActionItem
        if (selectedCollection == null) {
            onDismiss.invoke()
            return@ExpandMenuActionItem
        }
        onRemove(item.id, selectedCollection)
        onRefresh.invoke()

        onDismiss.invoke()
    }
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
