package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

@Composable
internal fun DropdownMenuItem_AddCollectionContent(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandMenuActionItem(
        icon = Icons.Default.AddCircleOutline,
        text = "В локальную коллекцию",
        modifier = modifier,
        onClick = onClick,
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_AddCollectionContentPreview() {
    DropdownMenuItem_AddCollectionContent(onClick = {})
}
