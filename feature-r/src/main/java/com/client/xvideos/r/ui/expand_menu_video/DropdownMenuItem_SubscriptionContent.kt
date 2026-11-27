package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.Unsubscribe
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem

@Composable
fun DropdownMenuItem_SubscriptionContent(
    isSubscribed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExpandMenuActionItem(
        icon = if (isSubscribed) Icons.Default.Unsubscribe else Icons.Default.Subscriptions,
        text = if (isSubscribed) "Unsubscribe" else "Subscribe",
        modifier = modifier,
        onClick = onClick
    )
}

@Preview(showBackground = true)
@Composable
private fun DropdownMenuItem_SubscriptionContentPreview() {
    DropdownMenuItem_SubscriptionContent(
        isSubscribed = true,
        onClick = {}
    )
}
