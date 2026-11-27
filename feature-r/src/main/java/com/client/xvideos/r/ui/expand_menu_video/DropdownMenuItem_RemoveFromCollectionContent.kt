package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.model.GifsInfo

@Composable
fun DropdownMenuItem_RemoveFromCollectionContent(
    item: GifsInfo?,
    selectedCollection: String?,
    onRemove: (String, String) -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandMenuActionItem(Icons.Default.RemoveCircleOutline, "Remove from Collection", modifier = modifier) {
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
private fun DropdownMenuItem_RemoveFromCollectionContentPreview() {
    DropdownMenuItem_RemoveFromCollectionContent(
        item = GifsInfo(id = "sample_id"),
        selectedCollection = "Sample Collection",
        onRemove = { _, _ -> },
        onRefresh = {},
        onDismiss = {}
    )
}
