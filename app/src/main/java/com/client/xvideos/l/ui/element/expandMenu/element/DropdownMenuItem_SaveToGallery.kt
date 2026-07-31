package com.client.xvideos.l.ui.element.expandMenu.element

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.l.model.PicsDetails

@Preview(apiLevel = 29, showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun Preview() {
    DropdownMenuItem_SaveToGallery(null, onClick = {}, onDismiss = {})
}

/** «В галерею»: сохранить большой файл (оригинал/видео) в /sdcard/xvideos_download. */
@Composable
fun DropdownMenuItem_SaveToGallery(item: PicsDetails? = null, onClick: (PicsDetails) -> Unit = {}, onDismiss: () -> Unit) {
    ExpandMenuActionItem(Icons.Default.SaveAlt, "В галерею") {
        if (item == null) return@ExpandMenuActionItem
        onClick.invoke(item)
        onDismiss.invoke()
    }
}
