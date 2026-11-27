package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PermIdentity
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

@Composable
fun DropdownMenuItem_FollowContent(
    isFollowed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandMenuActionItem(
        icon = if (isFollowed) Icons.Default.Person else Icons.Default.PermIdentity,
        text = if (isFollowed) "Отписаться" else "Подписаться",
        modifier = modifier,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_FollowContentPreview() {
    DropdownMenuItem_FollowContent(
        isFollowed = false,
        onClick = {}
    )
}
