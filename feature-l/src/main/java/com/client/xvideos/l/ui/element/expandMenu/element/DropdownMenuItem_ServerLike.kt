package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.model.PicsDetails

@Preview(apiLevel = 29, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview() {
    DropdownMenuItem_ServerLike(null, onClick = {}, onDismiss = {})
}

@Composable
fun DropdownMenuItem_ServerLike(
    url: PicsDetails? = null,
    onClick: (PicsDetails) -> Unit = {},
    onDismiss: () -> Unit
) {
    ExpandMenuActionItem(Icons.Outlined.FavoriteBorder, "Лайк на сервере") {
        if (url == null) return@ExpandMenuActionItem
        onClick.invoke(url)
        onDismiss.invoke()
    }
}
