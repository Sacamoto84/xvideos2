package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo

@Composable
fun DropdownMenuItem_AddCollection(item: GifsInfo? = null, savedRed: () -> SavedRed, onDismiss: () -> Unit) {
    DropdownMenuItem_AddCollectionContent(
        onClick = {
            if (item == null) return@DropdownMenuItem_AddCollectionContent
            savedRed.invoke().collections.collectionItemGifInfo = item
            savedRed.invoke().collections.visibleDialog = true
            onDismiss.invoke()
        }
    )
}
