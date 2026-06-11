package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.theme.Theme.L.ExpandMenu.style
import com.client.xvideos.common.theme.Theme.L.ExpandMenu.tintColor
import com.client.xvideos.l.model.PicsDetails

@Preview(apiLevel = 29, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview() {
    DropdownMenuItem_SaveToGallery(null, onClick = {}, onDismiss = {})
}

/** «В галерею»: сохранить большой файл (оригинал/видео) в /sdcard/xvideos_download. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuItem_SaveToGallery(item: PicsDetails? = null, onClick: (PicsDetails) -> Unit = {}, onDismiss: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = { Icon(Icons.Default.SaveAlt, contentDescription = "", tint = tintColor) },
        text = { Text("В галерею", style = style) },
        onClick = {
            if (item == null) return@DropdownMenuItem
            onClick.invoke(item)
            onDismiss.invoke()
        }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
    )
}
