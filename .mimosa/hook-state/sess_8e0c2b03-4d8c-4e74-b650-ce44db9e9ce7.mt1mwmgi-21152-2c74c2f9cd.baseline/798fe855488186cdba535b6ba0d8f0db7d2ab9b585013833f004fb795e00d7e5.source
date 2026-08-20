package com.client.xvideos.r.ui.expand_menu_video

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.runtime.Composable
import com.client.xvideos.common.expandmenu.ExpandMenuActionItem
import com.client.xvideos.r.model.GifsInfo

/** «В галерею»: сохранить видеофайл в общую галерею. */
@Composable
fun DropdownMenuItem_SaveToGallery(item: GifsInfo? = null, onClick: (GifsInfo) -> Unit, onDismiss: () -> Unit) {
    ExpandMenuActionItem(Icons.Default.SaveAlt, "В галерею") {
        if (item == null) return@ExpandMenuActionItem
        onClick.invoke(item)
        onDismiss.invoke()
    }
}
