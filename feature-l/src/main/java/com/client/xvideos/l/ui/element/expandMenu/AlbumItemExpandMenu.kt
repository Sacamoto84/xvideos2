package com.client.xvideos.l.ui.element.expandMenu

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.client.xvideos.common.expandmenu.LazyExpandMenuAnchor
import com.client.xvideos.l.featured.saved.SavedL
import com.client.xvideos.l.model.PicsDetails
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_AddCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Download
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_RemoveFromCollection
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SaveToGallery
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Share
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Меню элемента альбома. Само меню поднимается только после первого нажатия —
 * см. [LazyExpandMenuAnchor], там же объяснение зачем.
 */
@Composable
fun AlbumItemExpandMenu(
    item: PicsDetails? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDownload: (PicsDetails) -> Unit = {},
    onShare: (PicsDetails) -> Unit = {},
    onSaveToGallery: (PicsDetails) -> Unit = {},
    isCollection: Boolean = false,
    savedL: SavedL? = null,
    onRemoveFromCollection: (PicsDetails) -> Unit = {},
    idAlbum: String = ""
) {
    LazyExpandMenuAnchor(
        modifier = modifier,
        menuWidth = IntrinsicSize.Max,
        onOpen = onClick
    ) { dismiss ->

        DropdownMenuItem_Download(item, onClick = { onDownload(it) }) { dismiss() }

        DropdownMenuItem_Share(item, onClick = { onShare(it) }) { dismiss() }

        DropdownMenuItem_SaveToGallery(item, onClick = { onSaveToGallery(it) }) { dismiss() }

        DropdownMenuItem_AddCollection(item, savedL, idAlbum) { dismiss() }

        // Show RemoveFromCollection only when in collection view
        if (isCollection) {
            DropdownMenuItem_RemoveFromCollection(item, onRemoveFromCollection, savedL) { dismiss() }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
private fun AlbumItemExpandMenuPreview() {
    XvideosTheme(darkTheme = true) {
        AlbumItemExpandMenu(
            item = PicsDetails(
                height = 1080,
                width = 1920,
                is_animated = false,
                url_to_original = null,
                url_to_video = null,
                album = "preview-album",
                thumbnails = emptyList(),
            ),
            idAlbum = "preview-album"
        )
    }
}
