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
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_SaveToGallery
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_ServerUnlike
import com.client.xvideos.l.ui.element.expandMenu.element.DropdownMenuItem_Share
import com.client.xvideos.ui.theme.XvideosTheme

/**
 * Меню элемента в лайках на сервере Luscious.
 * Картинка уже лайкнута на сервере, поэтому доступно только удаление лайка ("Удалить лайк на сервере").
 * Опция "Лайк на сервере" отсутствует.
 */
@Composable
fun ServerLikesItemExpandMenu(
    item: PicsDetails? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onDownload: (PicsDetails) -> Unit = {},
    onServerUnlike: (PicsDetails) -> Unit = {},
    onShare: (PicsDetails) -> Unit = {},
    onSaveToGallery: (PicsDetails) -> Unit = {},
    savedL: SavedL? = null,
    idAlbum: String = ""
) {
    LazyExpandMenuAnchor(
        modifier = modifier,
        menuWidth = IntrinsicSize.Max,
        onOpen = onClick
    ) { dismiss ->

        DropdownMenuItem_Download(item, onClick = { onDownload(it) }) { dismiss() }

        DropdownMenuItem_ServerUnlike(item, onClick = { onServerUnlike(it) }) { dismiss() }

        DropdownMenuItem_Share(item, onClick = { onShare(it) }) { dismiss() }

        DropdownMenuItem_SaveToGallery(item, onClick = { onSaveToGallery(it) }) { dismiss() }

        DropdownMenuItem_AddCollection(item, savedL, idAlbum) { dismiss() }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF303030)
@Composable
private fun ServerLikesItemExpandMenuPreview() {
    XvideosTheme(darkTheme = true) {
        ServerLikesItemExpandMenu(
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
