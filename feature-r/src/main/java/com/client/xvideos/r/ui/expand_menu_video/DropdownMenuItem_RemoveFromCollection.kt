package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo

@Composable
fun DropdownMenuItem_RemoveFromCollection(
    item: GifsInfo? = null,
    onRefresh: () -> Unit,
    savedRed: () -> SavedRed,
    onDismiss: () -> Unit
) {
    val selectedCollection by savedRed.invoke().collections.selectedCollection.collectAsStateWithLifecycle()

    DropdownMenuItem_RemoveFromCollectionContent(
        item = item,
        selectedCollection = selectedCollection,
        onRemove = { itemId, collectionName ->
            savedRed.invoke().collections.deleteItemFromCollection(itemId, collectionName)
        },
        onRefresh = onRefresh,
        onDismiss = onDismiss
    )
}
