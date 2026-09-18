package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HeartBroken
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.model.PicsDetails

@Preview(apiLevel = 29, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview() {
    DropdownMenuItem_ServerUnlike(null, onClick = {}, onDismiss = {})
}

@Composable
fun DropdownMenuItem_ServerUnlike(
    url: PicsDetails? = null,
    onClick: (PicsDetails) -> Unit = {},
    onDismiss: () -> Unit
) {
    ExpandMenuActionItem(Icons.Outlined.HeartBroken, "Удалить лайк на сервере") {
        if (url == null) return@ExpandMenuActionItem
        onClick.invoke(url)
        onDismiss.invoke()
    }
}
