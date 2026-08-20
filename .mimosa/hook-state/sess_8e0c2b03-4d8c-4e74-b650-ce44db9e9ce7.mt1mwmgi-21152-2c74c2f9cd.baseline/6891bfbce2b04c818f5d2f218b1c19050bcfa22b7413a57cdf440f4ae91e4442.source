package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.LazyExpandMenuAnchor
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_AddCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Delete
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_RemoveFromCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SaveToGallery
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SetCover
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Share
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Меню элемента в сохранённых лайках. Само меню поднимается только после
 * первого нажатия — см. [LazyExpandMenuAnchor].
 */
@Composable
fun SavedLikesItemExpandMenu(
    item: PicsDetails? = null,
    onClick: () -> Unit = {},
    onDelete: (PicsDetails) -> Unit = {},
    onAddCollection: (PicsDetails) -> Unit = {},
    onRemoveFromCollection: (PicsDetails) -> Unit = {},
    onShare: (PicsDetails) -> Unit = {},
    onSaveToGallery: (PicsDetails) -> Unit = {},
    isCollection: Boolean = false,
    savedL: SavedL? = null
) {
    LazyExpandMenuAnchor(
        menuWidth = IntrinsicSize.Min,
        onOpen = onClick
    ) { dismiss ->

        DropdownMenuItem_Share(item, onClick = { onShare(it) }) { dismiss() }

        DropdownMenuItem_SaveToGallery(item, onClick = { onSaveToGallery(it) }) { dismiss() }

        // Show Delete only when NOT in collection view
        if (!isCollection) {
            DropdownMenuItem_Delete(item, onClick = { onDelete(it) }) { dismiss() }
        }

        DropdownMenuItem_AddCollection(item, savedL) { dismiss() }

        // Show RemoveFromCollection always (when in collection view or when item is in any collection)
        if (isCollection) {
            DropdownMenuItem_RemoveFromCollection(item, onRemoveFromCollection, savedL) { dismiss() }
            DropdownMenuItem_SetCover(item, savedL) { dismiss() }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
private fun SavedLikesItemExpandMenuPreview() {
    XvideosTheme(darkTheme = true) {
        SavedLikesItemExpandMenu(
            item = PicsDetails(
                height = 1080,
                width = 1920,
                is_animated = false,
                url_to_original = null,
                url_to_video = null,
                album = "preview-album",
                thumbnails = emptyList(),
            )
        )
    }
}
