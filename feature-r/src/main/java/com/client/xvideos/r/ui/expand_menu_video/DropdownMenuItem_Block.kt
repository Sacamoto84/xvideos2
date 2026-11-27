package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.runtime.Composable
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.GifsInfo
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
