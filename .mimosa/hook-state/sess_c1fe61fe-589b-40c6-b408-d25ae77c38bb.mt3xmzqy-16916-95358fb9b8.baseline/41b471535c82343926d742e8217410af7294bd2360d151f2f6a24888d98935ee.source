package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DropdownMenuItem_Share(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit, onDismiss: () -> Unit){
    ExpandMenuActionItem(Icons.Default.Share, "Поделиться") {
        if (item == null) return@ExpandMenuActionItem
        onClick.invoke(item)
        onDismiss.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuItem_SharePreview() {
    XvideosTheme {
        DropdownMenuItem_Share(
            item = GifsInfo(
                id = "123",
                createDate = 1672531200,
                likes = 100,
                width = 640,
                height = 480,
                tags = listOf("cat", "gif", "funny"),
                description = "A funny cat gif",
                views = 1000,
                type = 1,
                userName = "sampleUser"
            ),
            onClick = {},
            onDismiss = {}
        )
    }
}
