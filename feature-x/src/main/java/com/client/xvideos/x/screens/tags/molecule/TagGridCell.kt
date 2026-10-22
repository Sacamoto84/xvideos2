package com.client.xvideos.x.screens.tags.molecule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.client.xvideos.x.model.ItemsX
import com.client.xvideos.x.screens.common.UrlVideoImageAndLongClickX
import com.client.xvideos.x.screens.tags.model.TagCellActions
import com.client.xvideos.x.screens.ui.expandMenu.X_DashboardExpandMenu

/**
 * Ячейка сетки видео в выдаче по тегу: превью и меню «три точки» в правом
 * верхнем углу — как у ленты раздела.
 */
@Composable
fun TagGridCell(
    cell: ItemsX,
    actions: TagCellActions,
    modifier: Modifier = Modifier,
) {
    val handleOpen = remember(cell, actions) { { actions.onOpenVideo(cell) } }
    val handleFavoriteAdd = remember(cell, actions) { { actions.onFavoriteAdd(cell) } }
    val handleFavoriteRemove = remember(cell, actions) { { actions.onFavoriteRemove(cell) } }
    val handleDownload = remember(cell, actions) { { actions.onDownload(cell) } }
    val handleSaveToGallery = remember(cell, actions) { { actions.onSaveToGallery(cell) } }
    Box(
        modifier = modifier
            .aspectRatio(352f / 198f)
            .padding(1.dp)
            .background(Color.DarkGray)
    ) {
        UrlVideoImageAndLongClickX(
            cell,
            onLongClick = handleOpen,
            onDoubleClick = handleOpen,
            localVideo = actions.localVideoOf(cell),
        ) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                X_DashboardExpandMenu(
                    isFavorite = actions.isFavorite(cell.id),
                    onFavoriteAdd = handleFavoriteAdd,
                    onFavoriteRemove = handleFavoriteRemove,
                    onDownload = handleDownload,
                    onSaveToGallery = handleSaveToGallery,
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF121212)
@Composable
private fun TagGridCellPreview() {
    TagGridCell(
        cell = ItemsX(
            id = 101L,
            title = "Preview Tag Video",
            duration = "10:00",
            views = "100K",
            channel = "Studio",
            href = "/video",
            nameProfile = "Studio",
            linkProfile = "/studio",
        ),
        actions = TagCellActions.None,
    )
}
