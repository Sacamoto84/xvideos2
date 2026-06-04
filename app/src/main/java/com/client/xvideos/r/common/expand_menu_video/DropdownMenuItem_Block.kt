package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.r.common.block.BlockRed
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme
import com.skydoves.compose.stability.runtime.TraceRecomposition

@TraceRecomposition
@OptIn(ExperimentalMaterial3Api::class)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_BlockContent(
    item: GifsInfo? = null,
    onBlockClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenuItem(
        leadingIcon = {
            Icon(
                Icons.Default.Block,
                contentDescription = "",
                tint = ThemeL.ExpandMenu.tintColor,
            )
        },
        text = { Text("Блокировать", style = ThemeL.ExpandMenu.style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            onBlockClick.invoke()
            onDismiss.invoke()
        },
        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
    )
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