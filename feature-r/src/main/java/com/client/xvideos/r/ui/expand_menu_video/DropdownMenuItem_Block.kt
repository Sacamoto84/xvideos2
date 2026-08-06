package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme
import com.skydoves.compose.stability.runtime.TraceRecomposition

@TraceRecomposition
@Composable
fun DropdownMenuItem_Block(item: GifsInfo? = null, block: () -> BlockRed, onDismiss: () -> Unit) {
    DropdownMenuItem_BlockContent(
        item = item,
        onBlockClick = {
            block().blockItem = item
            block().blockVisibleDialog = true
        },
        onDismiss = onDismiss,
    )
}

@Composable
fun DropdownMenuItem_BlockContent(
    item: GifsInfo? = null,
    onBlockClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    ExpandMenuActionItem(Icons.Default.Block, "Блокировать") {
        if (item == null) return@ExpandMenuActionItem
        onBlockClick.invoke()
        onDismiss.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuItem_BlockPreview() {
    XvideosTheme {
        DropdownMenuItem_BlockContent(
            item = GifsInfo(
                id = "test_id",
                userName = "test_user",
                description = "Test Description",
            ),
            onBlockClick = {},
        ) {}
    }
}
