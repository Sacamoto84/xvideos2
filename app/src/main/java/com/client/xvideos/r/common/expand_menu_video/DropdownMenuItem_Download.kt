package com.client.xvideos.r.common.expand_menu_video

import com.client.xvideos.common.theme.Theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_Download(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit = {}, onDismiss: () -> Unit){
    DropdownMenuItem(
        leadingIcon = {Icon(Icons.Filled.FileDownload, contentDescription = "", tint = Theme.L.ExpandMenu.tintColor)},
        text = {Text("Скачать", style = Theme.L.ExpandMenu.style)},
        onClick = {
            if (item == null) return@DropdownMenuItem
            onClick.invoke(item)
            //DownloadRed.downloadItem(item)
            onDismiss.invoke()
        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
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