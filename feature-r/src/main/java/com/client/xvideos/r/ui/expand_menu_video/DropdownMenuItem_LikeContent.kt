package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

@Composable
internal fun DropdownMenuItem_LikeContent(
    isLiked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandMenuActionItem(
        icon = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        text = if (isLiked) "Убрать локальный лайк" else "Лайк на устройстве",
        modifier = modifier,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_LikeContentPreview() {
    Column {
        DropdownMenuItem_LikeContent(isLiked = true, onClick = {})
        DropdownMenuItem_LikeContent(isLiked = false, onClick = {})
    }
}
