package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.l.theme.ThemeL
import com.client.xvideos.r.model.GifsInfo
import com.client.xvideos.ui.theme.XvideosTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_Share(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit, onDismiss: () -> Unit){
    DropdownMenuItem(
        leadingIcon = {Icon(Icons.Default.Share, contentDescription = "", tint = ThemeL.ExpandMenu.tintColor)},
        text = { Text("Поделиться", style = ThemeL.ExpandMenu.style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            //DownloadRed.downloadItem(item)
            onClick.invoke(item)
            onDismiss.invoke()
        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
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