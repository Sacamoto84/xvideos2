package com.client.xvideos.r.common.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.client.xvideos.common.theme.Theme
import com.client.xvideos.r.model.GifsInfo

/** «В галерею»: сохранить видеофайл в /sdcard/xvideos_download. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_SaveToGallery(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit, onDismiss: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = "", tint = Theme.ExpandMenu.tintColor) },
        text = { Text("В галерею", style = Theme.ExpandMenu.style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            onClick.invoke(item)
            onDismiss.invoke()
        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}
