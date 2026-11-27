package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.model.GifsInfo

@Composable
fun DropdownMenuItem_BlockContent(
    onBlockClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    item: GifsInfo? = null,
) {
    ExpandMenuActionItem(Icons.Default.Block, "Блокировать", modifier = modifier) {
        if (item == null) return@ExpandMenuActionItem
        onBlockClick.invoke()
        onDismiss.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuItem_BlockContentPreview() {
    DropdownMenuItem_BlockContent(
        item = GifsInfo(
            id = "test_id",
            userName = "test_user",
            description = "Test Description",
        ),
        onBlockClick = {},
        onDismiss = {},
    )
}
