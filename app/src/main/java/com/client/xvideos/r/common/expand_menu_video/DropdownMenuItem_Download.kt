package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@Composable
fun DropdownMenuItem_Download(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit = {}, onDismiss: () -> Unit){
    ExpandMenuActionItem(Icons.Filled.FileDownload, "Скачать") {
        if (item == null) return@ExpandMenuActionItem
        onClick.invoke(item)
        onDismiss.invoke()
    }
}

@Preview(showBackground = true)
@Composable
fun DropdownMenuItem_DownloadPreview() {
    XvideosTheme {
        DropdownMenuItem_Download(
            item = GifsInfo(
                id = "test_id",
                userName = "test_user",
                description = "Test Description"
            ),
            onClick = {},
            onDismiss = {}
        )
    }
}
