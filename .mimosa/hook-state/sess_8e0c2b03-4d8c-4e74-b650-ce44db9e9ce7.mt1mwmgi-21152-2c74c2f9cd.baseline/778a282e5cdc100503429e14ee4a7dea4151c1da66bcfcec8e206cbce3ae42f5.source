package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.common.saved.SavedRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DropdownMenuItem_AddCollection(item: GifsInfo? = null, savedRed: ()->SavedRed, onDismiss: () -> Unit){
    DropdownMenuItem_AddCollectionContent(
        onClick = {
            if (item == null) return@DropdownMenuItem_AddCollectionContent
            savedRed.invoke().collections.collectionItemGifInfo = item
            savedRed.invoke().collections.visibleDialog = true
            onDismiss.invoke()
        }
    )
}

@Composable
private fun DropdownMenuItem_AddCollectionContent(
    onClick: () -> Unit
) {
    ExpandMenuActionItem(Icons.Default.AddCircleOutline, "Add to Collection", onClick)
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_AddCollectionPreview() {
    XvideosTheme {
        DropdownMenuItem_AddCollectionContent(
            onClick = {}
        )
    }
}
